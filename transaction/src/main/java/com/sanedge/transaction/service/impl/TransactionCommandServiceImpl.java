package com.sanedge.transaction.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transaction.domain.requests.CreateTransactionRequest;
import com.sanedge.transaction.domain.requests.UpdateTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.enums.PaymentStatus;
import com.sanedge.transaction.repository.TransactionCommandRepository;
import com.sanedge.transaction.repository.TransactionQueryRepository;
import com.sanedge.transaction.entity.TransactionOutbox;
import com.sanedge.transaction.repository.TransactionOutboxRepository;
import com.sanedge.transaction.service.KafkaService;
import com.sanedge.transaction.service.TransactionCommandService;

import com.sanedge.common.adapter.merchant.MerchantPort;
import com.sanedge.common.adapter.model.Order;
import com.sanedge.common.adapter.model.OrderItem;
import com.sanedge.common.adapter.model.ShippingAddress;
import com.sanedge.common.adapter.order.OrderPort;
import com.sanedge.common.adapter.order_item.OrderItemPort;
import com.sanedge.common.adapter.shipping_address.ShippingAddressPort;
import com.sanedge.common.adapter.user.UserPort;

import io.vertx.core.json.JsonObject;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransactionCommandServiceImpl implements TransactionCommandService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionCommandServiceImpl.class);

    private final TransactionQueryRepository transactionQueryRepository;
    private final TransactionCommandRepository transactionCommandRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    MerchantPort merchantPort;

    @Inject
    OrderPort orderPort;

    @Inject
    OrderItemPort orderItemPort;

    @Inject
    ShippingAddressPort shippingAddressPort;

    @Inject
    UserPort userPort;

    @Inject
    KafkaService kafkaService;

    @Inject
    TransactionOutboxRepository transactionOutboxRepository;

    @Inject
    public TransactionCommandServiceImpl(TransactionQueryRepository transactionQueryRepository,
            TransactionCommandRepository transactionCommandRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.transactionQueryRepository = transactionQueryRepository;
        this.transactionCommandRepository = transactionCommandRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    private Uni<Void> invalidateCache(Long txId, Integer orderId) {
        Uni<Void> deleteTx = txId != null ? redisService.deleteReactive("transaction:id:" + txId).replaceWithVoid()
                : Uni.createFrom().voidItem();
        Uni<Void> deleteOrderTx = orderId != null
                ? redisService.deleteReactive("transaction:order:" + orderId).replaceWithVoid()
                : Uni.createFrom().voidItem();
        return Uni.combine().all().unis(deleteTx, deleteOrderTx).discardItems()
                .chain(v -> redisService.deleteReactive("transaction:all:*").replaceWithVoid())
                .chain(v -> redisService.deleteReactive("transaction:active:*").replaceWithVoid())
                .chain(v -> redisService.deleteReactive("transaction:trashed:*").replaceWithVoid())
                .chain(v -> redisService.deleteReactive("transaction:merchant:*").replaceWithVoid())
                .onFailure().recoverWithItem((Void) null);
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<TransactionResponse>> create(CreateTransactionRequest req) {
        logger.info("Creating new transaction | orderId={}, merchantId={}", req.getOrderID(), req.getMerchantID());

        return findExistingTransaction(req.getOrderID())
                .chain(existing -> {
                    if (existing.isPresent()) {
                        throw new ResourceAlreadyExistsException(
                                "An active transaction already exists for order id=" + req.getOrderID());
                    }
                    return tracingMetrics.traceAndMeasure("createTransaction", "create_transaction",
                Attributes.builder()
                        .put("order.id", req.getOrderID() != null ? req.getOrderID().toString() : "null")
                        .put("merchant.id", req.getMerchantID() != null ? req.getMerchantID().toString() : "null")
                        .build(),
                () -> merchantPort.findById(req.getMerchantID())
                        .chain(merchant -> orderPort.findById(req.getOrderID()))
                        .chain(order -> {
                            if (order == null || order.id() == 0) {
                                logger.error("Order not found | orderId={}", req.getOrderID());
                                throw new ResourceNotFoundException("Order not found");
                            }

                            return Uni.combine().all().unis(
                                    orderItemPort.findOrderItemByOrder(order.id()),
                                    shippingAddressPort.findByOrder(order.id()))
                                    .asTuple().chain(tuple -> {
                                        List<OrderItem> orderItems = tuple.getItem1();
                                        ShippingAddress shipping = tuple.getItem2();

                                        if (orderItems.isEmpty()) {
                                            logger.error("No order items found | orderId={}", req.getOrderID());
                                            throw new IllegalArgumentException("No order items found");
                                        }

                                        if (shipping == null || shipping.id() == 0) {
                                            logger.error("Shipping address not found | orderId={}", req.getOrderID());
                                            throw new ResourceNotFoundException("Shipping address not found");
                                        }

                                        int totalAmount = 0;
                                        for (OrderItem item : orderItems) {
                                            if (item.quantity() <= 0) {
                                                throw new IllegalArgumentException("Invalid order item quantity");
                                            }
                                            totalAmount += item.price() * item.quantity();
                                        }
                                        totalAmount += shipping.shippingCost();
                                        int ppn = totalAmount * 11 / 100;
                                        int totalAmountWithTax = totalAmount + ppn;

                                        String paymentStatus = req.getAmount() >= totalAmountWithTax ? "success"
                                                : "failed";
                                        if (paymentStatus.equals("failed")) {
                                            logger.error("Insufficient payment amount | amount={}, required={}",
                                                    req.getAmount(), totalAmountWithTax);
                                            throw new IllegalArgumentException("Insufficient payment amount");
                                        }

                                        req.setAmount(totalAmountWithTax);

                                        Transaction transaction = new Transaction();
                                        transaction.setOrderId(req.getOrderID());
                                        transaction.setMerchantId(req.getMerchantID());
                                        transaction.setPaymentMethod(req.getPaymentMethod());
                                        transaction.setAmount(req.getAmount());
                                        transaction.setStatus(PaymentStatus.PENDING);

                                        return transactionCommandRepository.persist(transaction)
                                                .chain(saved -> sendTransactionEmail(saved, order).replaceWith(saved));
                                    });
                        })
                        .chain(saved -> {
                            TransactionResponse response = TransactionResponse.from(saved);
                            return invalidateCache(saved.id, saved.getOrderId())
                                    .map(v -> {
                                        logger.info("Transaction created successfully | transactionId={}", saved.id);
                                        return ApiResponse.success("Transaction created successfully", response);
                                    });
                        }));
                });
    }

    private Uni<java.util.Optional<Transaction>> findExistingTransaction(Integer orderId) {
        Uni<java.util.Optional<Transaction>> existing = transactionQueryRepository.findActivePaymentByOrderId(orderId);
        return existing != null ? existing : Uni.createFrom().item(java.util.Optional.empty());
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<TransactionResponse>> update(UpdateTransactionRequest req) {
        logger.info("Updating transaction | transactionId={}", req.getTransactionID());

        return tracingMetrics.traceAndMeasure("updateTransaction", "update_transaction",
                Attributes.builder()
                        .put("transaction.id",
                                req.getTransactionID() != null ? req.getTransactionID().toString() : "null")
                        .build(),
                () -> transactionQueryRepository.findTransactionById(req.getTransactionID().longValue())
                        .chain(existingTx -> {
                            if (existingTx.isEmpty()) {
                                logger.error("Transaction not found | transactionId={}", req.getTransactionID());
                                throw new ResourceNotFoundException("Transaction not found");
                            }
                            Transaction tx = existingTx.get();

                            if (PaymentStatus.SUCCESS.equals(tx.getStatus()) ||
                                    PaymentStatus.REFUNDED.equals(tx.getStatus())) {
                                logger.error("Transaction cannot be modified | transactionId={}",
                                        req.getTransactionID());
                                throw new IllegalArgumentException("Transaction cannot be modified");
                            }

                            return merchantPort.findById(req.getMerchantID())
                                    .chain(merchant -> orderPort.findById(req.getOrderID()))
                                    .chain(order -> {
                                        if (order == null || order.id() == 0) {
                                            logger.error("Order not found | orderId={}", req.getOrderID());
                                            throw new ResourceNotFoundException("Order not found");
                                        }

                                        return Uni.combine().all().unis(
                                                orderItemPort.findOrderItemByOrder(order.id()),
                                                shippingAddressPort.findByOrder(order.id()))
                                                .asTuple().chain(tuple -> {
                                                    List<OrderItem> orderItems = tuple.getItem1();
                                                    ShippingAddress shipping = tuple.getItem2();

                                                    if (orderItems.isEmpty()) {
                                                        logger.error("No order items found | orderId={}",
                                                                req.getOrderID());
                                                        throw new IllegalArgumentException("No order items found");
                                                    }

                                                    if (shipping == null || shipping.id() == 0) {
                                                        logger.error("Shipping address not found | orderId={}",
                                                                req.getOrderID());
                                                        throw new ResourceNotFoundException(
                                                                "Shipping address not found");
                                                    }

                                                    int totalAmount = 0;
                                                    for (OrderItem item : orderItems) {
                                                        if (item.quantity() <= 0) {
                                                            throw new IllegalArgumentException(
                                                                    "Invalid order item quantity");
                                                        }
                                                        totalAmount += item.price() * item.quantity();
                                                    }
                                                    totalAmount += shipping.shippingCost();
                                                    int ppn = totalAmount * 11 / 100;
                                                    int totalAmountWithTax = totalAmount + ppn;

                                                    String paymentStatus = req.getAmount() >= totalAmountWithTax
                                                            ? "success"
                                                            : "failed";
                                                    if (paymentStatus.equals("failed")) {
                                                        logger.error(
                                                                "Insufficient payment amount | amount={}, required={}",
                                                                req.getAmount(), totalAmountWithTax);
                                                        throw new IllegalArgumentException(
                                                                "Insufficient payment amount");
                                                    }

                                                    req.setAmount(totalAmountWithTax);

                                                    tx.setOrderId(req.getOrderID());
                                                    tx.setMerchantId(req.getMerchantID());
                                                    tx.setPaymentMethod(req.getPaymentMethod());
                                                    tx.setAmount(req.getAmount());
                                                    tx.setStatus(PaymentStatus.PENDING);

                                                    return transactionCommandRepository.persist(tx);
                                                });
                                    });
                        })
                        .chain(updated -> {
                            TransactionResponse response = TransactionResponse.from(updated);
                            return invalidateCache(updated.id, updated.getOrderId())
                                    .map(v -> {
                                        logger.info("Transaction updated successfully | transactionId={}", updated.id);
                                        return ApiResponse.success("Transaction updated successfully", response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<TransactionResponseDeleteAt>> trash(Integer id) {
        logger.info("Trashing transaction id={}", id);

        return tracingMetrics.traceAndMeasure("trashTransaction", "trash_transaction",
                Attributes.builder().put("transaction.id", id.toString()).build(),
                () -> transactionCommandRepository.trashed(id.longValue())
                        .chain(transaction -> {
                            if (transaction == null) {
                                throw new ResourceNotFoundException("Transaction not found or already trashed");
                            }
                            TransactionResponseDeleteAt response = TransactionResponseDeleteAt.from(transaction);

                            return invalidateCache(id.longValue(), transaction.getOrderId())
                                    .map(v -> {
                                        logger.info("Successfully trashed transaction with ID: {}", id);
                                        return ApiResponse.success("Transaction trashed successfully", response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<TransactionResponseDeleteAt>> restore(Integer id) {
        logger.info("Restoring transaction id={}", id);

        return tracingMetrics.traceAndMeasure("restoreTransaction", "restore_transaction",
                Attributes.builder().put("transaction.id", id.toString()).build(),
                () -> transactionCommandRepository.restore(id.longValue())
                        .chain(transaction -> {
                            if (transaction == null) {
                                throw new ResourceNotFoundException("Transaction not found or not trashed");
                            }
                            TransactionResponseDeleteAt response = TransactionResponseDeleteAt.from(transaction);

                            return invalidateCache(id.longValue(), transaction.getOrderId())
                                    .map(v -> {
                                        logger.info("Successfully restored transaction with ID: {}", id);
                                        return ApiResponse.success("Transaction restored successfully", response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> delete(Integer id) {
        Attributes attrs = Attributes.builder().put("transaction.id", id.toString()).build();
        logger.warn("Permanently deleting transaction id={}", id);

        return tracingMetrics.traceAndMeasure("deleteTransactionPermanent", "delete_transaction_permanent", attrs,
                () -> {
                    return transactionCommandRepository.deletePermanent(id.longValue())
                            .chain(deletedTx -> {
                                if (deletedTx == null) {
                                    logger.warn(
                                            "Permanent delete failed - transaction not found or must be trashed before permanent deletion with id: {}",
                                            id);
                                    throw new InvalidRequestException(
                                            "Transaction not found or must be trashed before permanent deletion");
                                }

                                return invalidateCache(id.longValue(), deletedTx.getOrderId())
                                        .map(v2 -> {
                                            logger.info("Successfully permanently deleted transaction with ID: {}", id);
                                            return ApiResponse.success("Transaction permanently deleted");
                                        });
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAll() {
        logger.info("Restoring ALL trashed transactions");

        return tracingMetrics.traceAndMeasure("restoreAllTransactions", "restore_all_transactions", () -> {
            return transactionCommandRepository.restoreAllDeleted()
                    .chain(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed transactions found");
                        }
                        return invalidateCache(null, null)
                                .map(v -> {
                                    logger.info("Successfully restored all trashed transactions");
                                    return ApiResponse.success("All transactions restored successfully");
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAll() {
        logger.warn("Permanently deleting ALL trashed transactions");

        return tracingMetrics.traceAndMeasure("deleteAllTransactionsPermanent", "delete_all_transactions_permanent",
                () -> {
                    return transactionCommandRepository.deleteAllDeleted()
                            .chain(success -> {
                                if (!success) {
                                    throw new ResourceNotFoundException("No trashed transactions found");
                                }
                                return invalidateCache(null, null)
                                        .map(v -> {
                                            logger.info("Successfully permanently deleted all trashed transactions");
                                            return ApiResponse.success("All transactions permanently deleted");
                                        });
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteByOrder(Integer orderId) {
        logger.warn("Permanently deleting transaction by order id={}", orderId);

        return tracingMetrics.traceAndMeasure("deleteTransactionByOrderPermanent",
                "delete_transaction_by_order_permanent",
                Attributes.builder().put("order.id", orderId.toString()).build(),
                () -> transactionCommandRepository.deleteByOrderPermanent(orderId.longValue())
                        .chain(deleted -> invalidateCache(null, orderId)
                                .map(v -> {
                                    logger.info("Successfully permanently deleted transaction by order ID: {}",
                                            orderId);
                                    return ApiResponse.success("Transaction by order permanently deleted", deleted);
                                })));
    }

    // Package-private to allow @Spy-based happy-path tests to bypass the email
    // Kafka chain.
    Uni<Void> sendTransactionEmail(Transaction tx, Order order) {
        return userPort
                .findById(order.userId())
                .chain(user -> {
                    String subject = "Transaction Created Successfully";
                    String body = String.format(
                            "Hello %s %s,\n\nYour transaction has been created successfully!\n\nTransaction Details:\n- ID: %s\n- Amount: %s\n- Payment Method: %s\n- Status: %s\n\nThank you for shopping with us!\n\nRegards,\nSupport Team",
                            user.firstname(), user.lastname(), tx.id, tx.getAmount(), tx.getPaymentMethod(),
                            tx.getStatus());

                    String eventId = UUID.randomUUID().toString();
                    JsonObject payload = new JsonObject()
                            .put("email", user.email())
                            .put("subject", subject)
                            .put("body", body)
                            .put("event_id", eventId)
                            .put("schema_version", 1)
                            .put("event_type", "email-service-topic-transaction-create")
                            .put("occurred_at", Instant.now().toString());

                    if (transactionOutboxRepository == null) {
                        return kafkaService.sendExistingEvent("email-service-topic-transaction-create", user.email(), payload);
                    }

                    TransactionOutbox event = new TransactionOutbox();
                    event.setEventId(eventId);
                    event.setTopic("email-service-topic-transaction-create");
                    event.setEventKey(user.email());
                    event.setPayload(payload.encode());
                    return transactionOutboxRepository.persist(event).replaceWithVoid();
                })
                .onFailure().invoke(err -> logger.error("Failed to send transaction email", err))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }
}