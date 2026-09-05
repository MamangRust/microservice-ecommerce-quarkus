package com.sanedge.order.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ForbiddenException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.requests.CreateOrderItemRequest;
import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.UpdateOrderItemRequest;
import com.sanedge.order.domain.requests.UpdateOrderRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.entity.Order;
import com.sanedge.order.repository.OrderCommandRepository;
import com.sanedge.order.repository.OrderQueryRepository;
import com.sanedge.order.service.OrderCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ApplicationScoped
public class OrderCommandServiceImpl implements OrderCommandService {
    private static final Logger logger = LoggerFactory.getLogger(OrderCommandServiceImpl.class);

    @Inject
    @GrpcClient("merchant")
    pb.merchant.MerchantQueryService merchantQueryService;

    @Inject
    @GrpcClient("user")
    pb.user.UserQueryService userQueryService;

    @Inject
    @GrpcClient("product")
    pb.product.ProductQueryService productQueryService;

    @Inject
    @GrpcClient("product")
    pb.product.ProductCommandService productCommandService;

    @Inject
    @GrpcClient("order_item")
    pb.order_item.OrderItemCommandService orderItemCommandServiceGrpc;

    @Inject
    @GrpcClient("order_item")
    pb.order_item.OrderItemQueryService orderItemQueryServiceGrpc;

    @Inject
    @GrpcClient("transaction")
    pb.transaction.TransactionQueryService transactionQueryService;

    @Inject
    @GrpcClient("shipping_address")
    pb.shipping_address.MutinyShippingCommandServiceGrpc.MutinyShippingCommandServiceStub shippingCommandService;

    @Inject
    @GrpcClient("shipping_address")
    pb.shipping_address.MutinyShippingQueryServiceGrpc.MutinyShippingQueryServiceStub shippingQueryService;

    private final OrderQueryRepository orderQueryRepository;
    private final OrderCommandRepository orderCommandRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    public OrderCommandServiceImpl(OrderQueryRepository orderQueryRepository,
            OrderCommandRepository orderCommandRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.orderQueryRepository = orderQueryRepository;
        this.orderCommandRepository = orderCommandRepository;
        this.validator = validator;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    private <T> void validateRequest(T req) {
        Set<ConstraintViolation<T>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            logger.error("Validation failed: {}", sb);
            throw new ConstraintViolationException("Validation failed: " + sb, violations);
        }
    }

    private Uni<Void> invalidateCache(Long orderId) {
        if (orderId != null) {
            return Uni.combine().all().unis(
                    redisService.deleteReactive("order:id:" + orderId),
                    redisService.deleteReactive("order:relation:" + orderId)).discardItems();
        }
        return Uni.createFrom().voidItem();
    }

    Uni<Integer> processCreateOrderItems(List<CreateOrderItemRequest> items, Order order, int index,
            int currentTotalPrice) {
        List<CreateOrderItemRequest> reserved = new ArrayList<>();
        return processCreateOrderItemsInternal(items, order, index, currentTotalPrice, reserved)
                .onFailure().recoverWithUni(error -> preserveFailure(error, rollbackCreatedOrder(order.id, reserved)));
    }

    private Uni<Integer> processCreateOrderItemsInternal(List<CreateOrderItemRequest> items, Order order, int index,
            int currentTotalPrice, List<CreateOrderItemRequest> reserved) {
        if (index >= items.size()) {
            return Uni.createFrom().item(currentTotalPrice);
        }

        CreateOrderItemRequest itemReq = items.get(index);
        return productQueryService
                .findById(pb.product.ProductCommon.FindByIdProductRequest.newBuilder().setId(itemReq.getProductId())
                        .build())
                .chain(prodResponse -> {
                    if (prodResponse == null || !prodResponse.hasData() || prodResponse.getData().getId() == 0) {
                        throw new ResourceNotFoundException("Product not found with id=" + itemReq.getProductId());
                    }
                    pb.product.ProductCommon.ProductResponse product = prodResponse.getData();
                    if (itemReq.getQuantity() <= 0 || product.getCountInStock() < itemReq.getQuantity()) {
                        throw new InvalidRequestException(
                                "Insufficient stock or invalid quantity for product id=" + itemReq.getProductId());
                    }

                    int authoritativePrice = product.getPrice();
                    var createReq = pb.order_item.OrderItemCommand.CreateOrderItemRecordRequest.newBuilder()
                            .setOrderId(order.id.intValue())
                            .setProductId(itemReq.getProductId())
                            .setQuantity(itemReq.getQuantity())
                            .setPrice(authoritativePrice)
                            .build();

                    return adjustStock(
                            pb.product.ProductCommand.AdjustProductStockRequest.newBuilder()
                                    .setProductId(itemReq.getProductId())
                                    .setDelta(-itemReq.getQuantity())
                                    .build())
                            .chain(stockResponse -> {
                                reserved.add(itemReq);
                                return orderItemCommandServiceGrpc.createOrderItem(createReq)
                                        .replaceWithVoid();
                            })
                            .chain(() -> processCreateOrderItemsInternal(items, order, index + 1,
                                    currentTotalPrice + (itemReq.getQuantity() * authoritativePrice), reserved));
                });
    }

    private Uni<Void> rollbackCreatedOrder(Long orderId, List<CreateOrderItemRequest> reserved) {
        return compensateStock(reserved)
                .chain(() -> orderItemCommandServiceGrpc.deleteOrderItemByOrderRollback(
                        pb.order_item.OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                                .setId(orderId.intValue()).build()))
                .replaceWithVoid();
    }

    private Uni<Void> rollbackOrderAfterPersistenceFailure(Long orderId, List<CreateOrderItemRequest> reserved) {
        return rollbackCreatedOrder(orderId, reserved)
                .chain(() -> orderCommandRepository.deleteCreated(orderId).replaceWithVoid());
    }

    private <T> Uni<T> preserveFailure(Throwable original, Uni<Void> compensation) {
        return compensation
                .onFailure().invoke(error -> logger.error("Commerce compensation failed; preserving original failure",
                        error))
                .onFailure().recoverWithItem((Void) null)
                .chain(() -> Uni.createFrom().failure(original));
    }

    private Uni<pb.product.ProductCommon.ApiResponseProduct> adjustStock(
            pb.product.ProductCommand.AdjustProductStockRequest request) {
        Uni<pb.product.ProductCommon.ApiResponseProduct> adjusted = productCommandService.adjustStock(request);
        return adjusted != null
                ? adjusted
                : Uni.createFrom().item(pb.product.ProductCommon.ApiResponseProduct.getDefaultInstance());
    }

    private Uni<Void> compensateStock(List<CreateOrderItemRequest> reserved) {
        if (reserved == null || reserved.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        Uni<Void> compensation = Uni.createFrom().voidItem();
        for (int i = reserved.size() - 1; i >= 0; i--) {
            CreateOrderItemRequest item = reserved.get(i);
            compensation = compensation.chain(() -> adjustStock(
                    pb.product.ProductCommand.AdjustProductStockRequest.newBuilder()
                            .setProductId(item.getProductId())
                            .setDelta(item.getQuantity())
                            .build()).replaceWithVoid());
        }
        return compensation
                .invoke(() -> recordStockCompensation("success", reserved.size()))
                .onFailure().invoke(error -> recordStockCompensation("failure", reserved.size()));
    }

    private void recordStockCompensation(String result, int items) {
        if (tracingMetrics != null) {
            tracingMetrics.recordStockCompensation(result, items);
        }
    }


    private Uni<Void> ensureOrderDeletable(Long orderId) {
        if (transactionQueryService == null) {
            return Uni.createFrom().voidItem();
        }
        return transactionQueryService.findByOrderId(
                pb.transaction.TransactionQuery.FindByOrderIdTransactionRequest.newBuilder()
                        .setOrderId(orderId.intValue()).build())
                .chain(response -> {
                    if (response != null && response.hasData()
                            && "success".equalsIgnoreCase(response.getData().getPaymentStatus())) {
                        throw new InvalidRequestException("Paid order cannot be permanently deleted");
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<Void> adjustOrderStock(Long orderId, int direction) {
        if (orderItemQueryServiceGrpc == null) {
            return Uni.createFrom().voidItem();
        }
        return orderItemQueryServiceGrpc.findOrderItemByOrder(
                pb.order_item.OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                        .setId(orderId.intValue()).build())
                .chain(response -> {
                    List<pb.order_item.OrderItemCommon.OrderItemResponse> items = response == null
                            ? List.of()
                            : response.getDataList();
                    List<CreateOrderItemRequest> adjusted = new ArrayList<>();
                    return adjustOrderStock(items, 0, direction, adjusted)
                            .onFailure().call(error -> {
                                Uni<Void> rollback = Uni.createFrom().voidItem();
                                for (int i = adjusted.size() - 1; i >= 0; i--) {
                                    var item = adjusted.get(i);
                                    rollback = rollback.chain(() -> adjustStock(
                                            pb.product.ProductCommand.AdjustProductStockRequest.newBuilder()
                                                    .setProductId(item.getProductId())
                                                    .setDelta(-direction * item.getQuantity())
                                                    .build()).replaceWithVoid());
                                }
                                return rollback;
                            });
                });
    }

    private Uni<Void> adjustOrderStock(List<pb.order_item.OrderItemCommon.OrderItemResponse> items,
            int index, int direction, List<CreateOrderItemRequest> adjusted) {
        if (index >= items.size()) {
            return Uni.createFrom().voidItem();
        }
        var item = items.get(index);
        return adjustStock(
                pb.product.ProductCommand.AdjustProductStockRequest.newBuilder()
                        .setProductId(item.getProductId())
                        .setDelta(direction * item.getQuantity())
                        .build())
                .invoke(() -> {
                    CreateOrderItemRequest marker = new CreateOrderItemRequest();
                    marker.setProductId(item.getProductId());
                    marker.setQuantity(item.getQuantity());
                    adjusted.add(marker);
                })
                .replaceWithVoid()
                .chain(() -> adjustOrderStock(items, index + 1, direction, adjusted));
    }

    private record UpdatedOrderItems(int totalPrice, List<CreateOrderItemRequest> adjustments) {
    }

    Uni<UpdatedOrderItems> processUpdateOrderItems(List<UpdateOrderItemRequest> items, Order order, int index,
            int currentTotalPrice) {
        if (items == null || items.isEmpty()) {
            return Uni.createFrom().item(new UpdatedOrderItems(currentTotalPrice, new ArrayList<>()));
        }
        List<CreateOrderItemRequest> adjustments = new ArrayList<>();
        Uni<pb.order_item.OrderItemCommon.ApiResponsesOrderItem> existingItems =
                orderItemQueryServiceGrpc == null ? null : orderItemQueryServiceGrpc.findOrderItemByOrder(
                        pb.order_item.OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                                .setId(order.id.intValue()).build());
        Uni<UpdatedOrderItems> operation = existingItems == null
                ? processUpdateOrderItemsInternal(items, order, index, currentTotalPrice, List.of(), adjustments)
                : existingItems.chain(response -> processUpdateOrderItemsInternal(items, order, index,
                        currentTotalPrice, response == null ? List.of() : response.getDataList(), adjustments));
        return operation.onFailure().recoverWithUni(error -> preserveFailure(error,
                compensateAdjustments(adjustments)));
    }


    private Uni<UpdatedOrderItems> processUpdateOrderItemsInternal(List<UpdateOrderItemRequest> items, Order order, int index,
            int currentTotalPrice, List<pb.order_item.OrderItemCommon.OrderItemResponse> existingItems,
            List<CreateOrderItemRequest> adjustments) {
        if (index >= items.size()) {
            return Uni.createFrom().item(new UpdatedOrderItems(currentTotalPrice, adjustments));
        }

        UpdateOrderItemRequest itemReq = items.get(index);
        return productQueryService
                .findById(pb.product.ProductCommon.FindByIdProductRequest.newBuilder().setId(itemReq.getProductId())
                        .build())
                .chain(prodResponse -> {
                    if (prodResponse == null || !prodResponse.hasData() || prodResponse.getData().getId() == 0) {
                        throw new ResourceNotFoundException("Product not found with id=" + itemReq.getProductId());
                    }
                    pb.product.ProductCommon.ProductResponse product = prodResponse.getData();
                    int stockDelta;
                    int oldQuantity = 0;
                    if (itemReq.getOrderItemId() != null && itemReq.getOrderItemId() > 0) {
                        var existingItem = existingItems.stream()
                                .filter(existing -> existing.getId() == itemReq.getOrderItemId())
                                .findFirst().orElseThrow(() -> new ResourceNotFoundException(
                                        "Order item not found with id=" + itemReq.getOrderItemId()));
                        if (existingItem.getProductId() != itemReq.getProductId()) {
                            throw new InvalidRequestException("Order item product cannot be changed");
                        }
                        oldQuantity = existingItem.getQuantity();
                        stockDelta = oldQuantity - itemReq.getQuantity();
                    } else {
                        stockDelta = -itemReq.getQuantity();
                    }
                    if (itemReq.getQuantity() <= 0) {
                        throw new InvalidRequestException("Order item quantity must be positive");
                    }

                    var stockRequest = pb.product.ProductCommand.AdjustProductStockRequest.newBuilder()
                            .setProductId(itemReq.getProductId()).setDelta(stockDelta).build();
                    Uni<Void> persistUnit = (stockDelta == 0
                            ? Uni.createFrom().voidItem()
                            : adjustStock(stockRequest).invoke(() -> {
                                CreateOrderItemRequest marker = new CreateOrderItemRequest();
                                marker.setProductId(itemReq.getProductId());
                                marker.setQuantity(stockDelta);
                                adjustments.add(marker);
                            }).replaceWithVoid())
                            .chain(() -> {
                                if (itemReq.getOrderItemId() != null && itemReq.getOrderItemId() > 0) {
                                    return orderItemCommandServiceGrpc.updateOrderItem(
                                            pb.order_item.OrderItemCommand.UpdateOrderItemRecordRequest.newBuilder()
                                                    .setOrderItemId(itemReq.getOrderItemId())
                                                    .setQuantity(itemReq.getQuantity())
                                                    .setPrice(product.getPrice()).build()).replaceWithVoid();
                                }
                                return orderItemCommandServiceGrpc.createOrderItem(
                                        pb.order_item.OrderItemCommand.CreateOrderItemRecordRequest.newBuilder()
                                                .setOrderId(order.id.intValue()).setProductId(itemReq.getProductId())
                                                .setQuantity(itemReq.getQuantity()).setPrice(product.getPrice()).build())
                                        .replaceWithVoid();
                            });
                    return persistUnit
                            .chain(() -> processUpdateOrderItemsInternal(items, order, index + 1,
                                    currentTotalPrice + (itemReq.getQuantity() * product.getPrice()), existingItems,
                                    adjustments));
                });
    }

    private Uni<Void> compensateAdjustments(List<CreateOrderItemRequest> adjustments) {
        if (adjustments == null || adjustments.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        Uni<Void> compensation = Uni.createFrom().voidItem();
        for (int i = adjustments.size() - 1; i >= 0; i--) {
            CreateOrderItemRequest adjustment = adjustments.get(i);
            compensation = compensation.chain(() -> adjustStock(
                    pb.product.ProductCommand.AdjustProductStockRequest.newBuilder()
                            .setProductId(adjustment.getProductId())
                            .setDelta(-adjustment.getQuantity()).build()).replaceWithVoid());
        }
        return compensation
                .invoke(() -> recordStockCompensation("success", adjustments.size()))
                .onFailure().invoke(error -> recordStockCompensation("failure", adjustments.size()));
    }

    @Override
    @WithSession
    public Uni<ApiResponse<OrderResponse>> create(CreateOrderRequest request) {
        logger.info("Creating new order for merchantId={} and userId={}", request.getMerchantId(),
                request.getUserId());

        try {
            validateRequest(request);
        } catch (Exception e) {
            logger.error("Validation failed for create order", e);
            return Uni.createFrom().failure(e);
        }

        Attributes attributes = Attributes.builder()
                .put("merchant.id", request.getMerchantId() != null ? request.getMerchantId().toString() : "null")
                .put("user.id", request.getUserId() != null ? request.getUserId().toString() : "null")
                .build();

        return tracingMetrics.traceAndMeasure("createOrder", "create_order", attributes, () -> merchantQueryService
                .findById(pb.merchant.MerchantCommon.FindByIdMerchantRequest.newBuilder().setId(request.getMerchantId())
                        .build())
                .chain(merchantResponse -> {
                    if (merchantResponse == null || !merchantResponse.hasData()
                            || merchantResponse.getData().getId() == 0) {
                        throw new ResourceNotFoundException("Merchant not found");
                    }
                    return userQueryService
                            .findById(pb.user.UserCommon.FindByIdUserRequest.newBuilder().setId(request.getUserId())
                                    .build());
                })
                .chain(userResponse -> {
                    if (userResponse == null || !userResponse.hasData() || userResponse.getData().getId() == 0) {
                        throw new ResourceNotFoundException("User not found");
                    }

                    Order order = new Order();
                    order.setMerchantId(request.getMerchantId());
                    order.setUserId(request.getUserId());
                    order.setTotalPrice(0);

                    return orderCommandRepository.persistNew(order);
                })
                .chain(savedOrder -> {
                    List<CreateOrderItemRequest> reserved = new ArrayList<>();
                    return processCreateOrderItemsInternal(request.getItems(), savedOrder, 0, 0, reserved)
                            .chain(totalPrice -> {
                                var createShippingReq = pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest.newBuilder()
                                        .setOrderId(savedOrder.id.intValue())
                                        .setAlamat(request.getShippingAddress().getAlamat() == null ? "" : request.getShippingAddress().getAlamat())
                                        .setProvinsi(request.getShippingAddress().getProvinsi() == null ? "" : request.getShippingAddress().getProvinsi())
                                        .setKota(request.getShippingAddress().getKota() == null ? "" : request.getShippingAddress().getKota())
                                        .setCourier(request.getShippingAddress().getCourier() == null ? "" : request.getShippingAddress().getCourier())
                                        .setShippingMethod(request.getShippingAddress().getShippingMethod() == null ? "" : request.getShippingAddress().getShippingMethod())
                                        .setShippingCost(request.getShippingAddress().getShippingCost() != null ? request.getShippingAddress().getShippingCost() : 0)
                                        .setNegara(request.getShippingAddress().getNegara() == null ? "" : request.getShippingAddress().getNegara())
                                        .build();

                                int shippingCost = request.getShippingAddress().getShippingCost() != null
                                        ? request.getShippingAddress().getShippingCost() : 0;
                                int subtotalWithShipping = totalPrice + shippingCost;
                                savedOrder.setTotalPrice(subtotalWithShipping + (subtotalWithShipping * 11 / 100));
                                return Uni.combine().all().unis(
                                        shippingCommandService.createShipping(createShippingReq),
                                        orderCommandRepository.updateTotalPrice(savedOrder.id, savedOrder.getTotalPrice())
                                                .chain(updated -> updated == 1
                                                        ? Uni.createFrom().item(updated)
                                                        : Uni.createFrom().failure(new ResourceNotFoundException(
                                                                "Order disappeared before total-price update"))))
                                                .asTuple().chain(tuple -> {
                                            OrderResponse response = OrderResponse.from(savedOrder);

                                            return invalidateCache(savedOrder.id)
                                                    .map(v -> {
                                                        logger.info("Successfully created order with ID: {}",
                                                                savedOrder.id);
                                                        return ApiResponse.success("Order created successfully",
                                                                response);
                                                    });
                                        });
                            })
                            .onFailure().recoverWithUni(error -> preserveFailure(error,
                                    rollbackOrderAfterPersistenceFailure(savedOrder.id, reserved)));
                })
                .onFailure().invoke(e -> logger.error("Failed to create order for user: {}", request.getUserId(), e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderResponse>> update(UpdateOrderRequest request) {
        if (request == null) {
            return Uni.createFrom().failure(new InvalidRequestException("Update request cannot be null"));
        }
        if (request.getOrderId() == null) {
            logger.error("OrderId is required for update");
            return Uni.createFrom().failure(new ResourceNotFoundException("OrderId is required"));
        }
        try {
            validateRequest(request);
        } catch (Exception e) {
            logger.error("Validation failed for update order", e);
            return Uni.createFrom().failure(e);
        }
        logger.info("Updating order id={}", request.getOrderId());

        return tracingMetrics.traceAndMeasure("updateOrder", "update_order",
                Attributes.builder().put("order.id", request.getOrderId().toString()).build(),
                () -> orderQueryRepository.findOrderById(request.getOrderId().longValue())
                        .<Order>chain(optOrder -> {
                            if (optOrder.isEmpty()) {
                                throw new ResourceNotFoundException("Order not found");
                            }
                            return userQueryService
                                    .findById(pb.user.UserCommon.FindByIdUserRequest.newBuilder()
                                            .setId(request.getUserId())
                                            .build())
                                    .map(userResponse -> {
                                        if (userResponse == null || !userResponse.hasData()
                                                || userResponse.getData().getId() == 0) {
                                            throw new ResourceNotFoundException("User not found");
                                        }
                                        Order existingOrder = optOrder.get();
                                        if (!request.getUserId().equals(existingOrder.getUserId())) {
                                            throw new ForbiddenException("You are not allowed to update this order");
                                        }
                                        return existingOrder;
                                    });
                        })
                        .<ApiResponse<OrderResponse>>chain(existingOrder -> {
                            return processUpdateOrderItems(request.getItems(), existingOrder, 0, 0)
                                    .<ApiResponse<OrderResponse>>chain(updatedItems -> {
                                        int totalPrice = updatedItems.totalPrice();
                                        Uni<io.smallrye.mutiny.tuples.Tuple2<pb.shipping_address.ShippingAddressCommon.ApiResponseShipping, Order>> businessUpdate =
                                                shippingQueryService.findById(pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.newBuilder()
                                                        .setId(request.getShippingAddress().getShippingId())
                                                        .build())
                                                        .chain(shippingResp -> {
                                                            if (shippingResp == null || !shippingResp.hasData()
                                                                    || shippingResp.getData().getId() == 0) {
                                                                throw new ResourceNotFoundException("Shipping address not found");
                                                            }

                                                            var updateShippingReq = pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest.newBuilder()
                                                                    .setShippingId(request.getShippingAddress().getShippingId())
                                                                    .setOrderId(existingOrder.id.intValue())
                                                                    .setAlamat(request.getShippingAddress().getAlamat() == null ? "" : request.getShippingAddress().getAlamat())
                                                                    .setProvinsi(request.getShippingAddress().getProvinsi() == null ? "" : request.getShippingAddress().getProvinsi())
                                                                    .setKota(request.getShippingAddress().getKota() == null ? "" : request.getShippingAddress().getKota())
                                                                    .setCourier(request.getShippingAddress().getCourier() == null ? "" : request.getShippingAddress().getCourier())
                                                                    .setShippingMethod(request.getShippingAddress().getShippingMethod() == null ? "" : request.getShippingAddress().getShippingMethod())
                                                                    .setShippingCost(request.getShippingAddress().getShippingCost() != null ? request.getShippingAddress().getShippingCost() : 0)
                                                                    .setNegara(request.getShippingAddress().getNegara() == null ? "" : request.getShippingAddress().getNegara())
                                                                    .build();

                                                            int shippingCost = request.getShippingAddress().getShippingCost() != null
                                                                    ? request.getShippingAddress().getShippingCost() : 0;
                                                            int subtotalWithShipping = totalPrice + shippingCost;
                                                            existingOrder.setTotalPrice(subtotalWithShipping
                                                                    + (subtotalWithShipping * 11 / 100));

                                                            return Uni.combine().all().unis(
                                                                    shippingCommandService.updateShipping(updateShippingReq),
                                                                    orderCommandRepository.persist(existingOrder)).asTuple();
                                                        });

                                        return businessUpdate
                                                .onFailure().recoverWithUni(error -> preserveFailure(error,
                                                        compensateAdjustments(updatedItems.adjustments())))
                                                .chain(tuple -> {
                                                    OrderResponse response = OrderResponse.from(existingOrder);
                                                    return invalidateCache(existingOrder.id)
                                                            .map(v -> {
                                                                logger.info("Successfully updated order with ID: {}",
                                                                        existingOrder.id);
                                                                return ApiResponse.success("Order updated successfully",
                                                                        response);
                                                            });
                                                });
                                    });
                        })
                        .onFailure()
                        .invoke(e -> logger.error("Failed to update order ID: {}", request.getOrderId(), e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderResponseDeleteAt>> trash(Long id) {
        logger.info("Trashing order id={}", id);

        return tracingMetrics.traceAndMeasure("trashOrder", "trash_order",
                Attributes.builder().put("order.id", id.toString()).build(), () -> orderCommandRepository.trashed(id)
                        .chain(order -> {
                            if (order == null) {
                                logger.warn("Failed to trash order - not found or already trashed with ID: {}", id);
                                throw new ResourceNotFoundException("Order not found or already trashed");
                            }
                                    return adjustOrderStock(id, 1)
                                    .onFailure().recoverWithUni(error -> preserveFailure(error,
                                            orderCommandRepository.restore(id).replaceWithVoid()))
                                    .replaceWith(order);
                        })
                        .chain(order -> {
                            OrderResponseDeleteAt response = OrderResponseDeleteAt.from(order);
                            return invalidateCache(id).map(v -> {
                                logger.info("Successfully trashed order with ID: {}", id);
                                return ApiResponse.success("Order trashed successfully!", response);
                            });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to trash order ID: {}", id, e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<OrderResponseDeleteAt>> restore(Long id) {
        logger.info("Restoring order id={}", id);

        return tracingMetrics.traceAndMeasure("restoreOrder", "restore_order",
                Attributes.builder().put("order.id", id.toString()).build(), () -> orderCommandRepository.restore(id)
                        .chain(order -> {
                            if (order == null) {
                                logger.warn("Failed to restore order - not found or not trashed with ID: {}", id);
                                throw new ResourceNotFoundException("Order not found or not trashed");
                            }
                            return adjustOrderStock(id, -1)
                                    .onFailure().recoverWithUni(error -> preserveFailure(error,
                                            orderCommandRepository.trashed(id).replaceWithVoid()))
                                    .replaceWith(order);
                        })
                        .chain(order -> {
                            OrderResponseDeleteAt response = OrderResponseDeleteAt.from(order);
                            return invalidateCache(id).map(v -> {
                                logger.info("Successfully restored order with ID: {}", id);
                                return ApiResponse.success("Order restored successfully!", response);
                            });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to restore order ID: {}", id, e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> delete(Long id) {
        Attributes attrs = Attributes.builder().put("order.id", id).build();
        logger.warn("Permanently deleting order id={}", id);

        return tracingMetrics.traceAndMeasure("deleteOrderPermanent", "delete_order_permanent", attrs, () -> {
            return ensureOrderDeletable(id)
                    .chain(() -> orderCommandRepository.deletePermanent(id))
                    .chain(deletedOrder -> {
                        if (deletedOrder == null) {
                            logger.warn(
                                    "Permanent delete failed - order not found or must be trashed before permanent deletion with id: {}",
                                    id);
                            throw new InvalidRequestException(
                                    "Order not found or must be trashed before permanent deletion");
                        }

                        return invalidateCache(id)
                                .map(v2 -> {
                                    logger.info("Successfully permanently deleted order with ID: {}", id);
                                    return ApiResponse.success("Order permanently deleted!");
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAll() {
        logger.info("Restoring ALL trashed orders");

        return tracingMetrics.traceAndMeasure("restoreAllOrders", "restore_all_orders", () ->
                orderCommandRepository.findAllDeleted()
                        .chain(orders -> {
                            if (orders.isEmpty()) {
                                throw new ResourceNotFoundException("No trashed orders found");
                            }
                            return restoreOrdersSequentially(orders, 0)
                                    .map(v -> ApiResponse.<Void>success("All orders restored successfully!"));
                        }));
    }

    private Uni<Void> restoreOrdersSequentially(List<Order> orders, int index) {
        return restoreOrdersSequentially(orders, index, new ArrayList<>());
    }

    private Uni<Void> restoreOrdersSequentially(List<Order> orders, int index, List<Order> restored) {
        if (index >= orders.size()) {
            return Uni.createFrom().voidItem();
        }
        Order order = orders.get(index);
        return orderCommandRepository.restore(order.id)
                .chain(current -> {
                    if (current == null) {
                        throw new ResourceNotFoundException("Order could not be restored: " + order.id);
                    }
                    return adjustOrderStock(order.id, -1)
                            .onFailure().recoverWithUni(error -> preserveFailure(error,
                                    orderCommandRepository.trashed(order.id).replaceWithVoid()))
                            .invoke(() -> restored.add(order))
                            .chain(() -> restoreOrdersSequentially(orders, index + 1, restored));
                })
                .onFailure().recoverWithUni(error -> preserveFailure(error, rollbackRestoredOrders(restored)));
    }

    private Uni<Void> rollbackRestoredOrders(List<Order> restored) {
        Uni<Void> rollback = Uni.createFrom().voidItem();
        for (int i = restored.size() - 1; i >= 0; i--) {
            Order order = restored.get(i);
            rollback = rollback
                    .chain(() -> adjustOrderStock(order.id, 1))
                    .chain(() -> orderCommandRepository.trashed(order.id).replaceWithVoid());
        }
        return rollback;
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAll() {
        logger.warn("Permanently deleting ALL trashed orders");

        return tracingMetrics.traceAndMeasure("deleteAllOrdersPermanent", "delete_all_orders_permanent", () -> {
            return orderCommandRepository.deleteAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed orders found");
                        }
                        logger.info("Successfully permanently deleted all trashed orders");
                        return ApiResponse.success("All orders permanently deleted!");
                    });
        });
    }
}