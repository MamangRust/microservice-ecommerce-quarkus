package com.sanedge.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transaction.domain.requests.CreateTransactionRequest;
import com.sanedge.transaction.domain.requests.UpdateTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.enums.PaymentStatus;
import com.sanedge.transaction.repository.TransactionCommandRepository;
import com.sanedge.transaction.repository.TransactionQueryRepository;
import com.sanedge.transaction.service.KafkaService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.merchant.MerchantQueryService;
import pb.order.OrderQueryService;
import pb.order_item.OrderItemQueryService;
import pb.shipping_address.ShippingQueryService;
import pb.user.UserQueryService;

@ExtendWith(MockitoExtension.class)
class TransactionCommandServiceImplTest {

        @Mock
        private TransactionQueryRepository transactionQueryRepo;

        @Mock
        private TransactionCommandRepository transactionCommandRepo;

        @Mock
        private MerchantQueryService merchantQueryService;

        @Mock
        private OrderQueryService orderQueryService;

        @Mock
        private OrderItemQueryService orderItemQueryService;

        @Mock
        private ShippingQueryService shippingQueryService;

        @Mock
        private UserQueryService userQueryService;

        @Mock
        private RedisService redisService;

        @Mock
        private KafkaService kafkaService;

        @Mock
        private TracingMetrics tracingMetrics;

        private TransactionCommandServiceImpl service;

        @BeforeEach
        void setUp() throws Exception {
                service = new TransactionCommandServiceImpl(
                                transactionQueryRepo,
                                transactionCommandRepo,
                                redisService,
                                tracingMetrics);

                setField(service, "merchantQueryService", merchantQueryService);
                setField(service, "orderQueryService", orderQueryService);
                setField(service, "orderItemQueryService", orderItemQueryService);
                setField(service, "shippingQueryService", shippingQueryService);
                setField(service, "userQueryService", userQueryService);

                setField(service, "kafkaService", kafkaService);

                lenient().doAnswer(invocation -> {
                        @SuppressWarnings("unchecked")
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

                lenient().doAnswer(invocation -> {
                        @SuppressWarnings("unchecked")
                        Supplier<Uni<?>> supplier = invocation.getArgument(2);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any());

                lenient().when(redisService.deleteReactive(anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                lenient().when(transactionCommandRepo.deleteAllDeleted())
                                .thenReturn(Uni.createFrom().item(true));
                lenient().when(transactionCommandRepo.restoreAllDeleted())
                                .thenReturn(Uni.createFrom().item(true));
        }

        private void setField(Object target, String fieldName, Object value) throws Exception {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
        }

        private Transaction createTestTransaction(Long id, Integer orderId, Integer merchantId, int amount,
                        PaymentStatus status) {
                Transaction t = new Transaction();
                t.setId(id);
                t.setOrderId(orderId);
                t.setMerchantId(merchantId);
                t.setAmount(amount);
                t.setPaymentMethod("CREDIT");
                t.setStatus(status);
                t.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                t.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return t;
        }

        private Transaction createTestTransaction(int id, Integer orderId, Integer merchantId, int amount,
                        PaymentStatus status) {
                return createTestTransaction((long) id, orderId, merchantId, amount, status);
        }

        private CreateTransactionRequest createValidCreateRequest() {
                CreateTransactionRequest req = new CreateTransactionRequest();
                req.setOrderID(1);
                req.setMerchantID(1);
                req.setAmount(200000);
                req.setPaymentMethod("CREDIT");
                return req;
        }

        private UpdateTransactionRequest createValidUpdateRequest() {
                UpdateTransactionRequest req = new UpdateTransactionRequest();
                req.setTransactionID(1);
                req.setOrderID(1);
                req.setMerchantID(1);
                req.setAmount(250000);
                req.setPaymentMethod("DEBIT");
                return req;
        }

        private void mockCreateDependencies() {
                when(merchantQueryService.findById(
                                any(pb.merchant.MerchantCommon.FindByIdMerchantRequest.class)))
                                .thenReturn(Uni.createFrom().item(
                                                pb.merchant.MerchantCommon.ApiResponseMerchant.newBuilder()
                                                                .setData(pb.merchant.MerchantCommon.MerchantResponse
                                                                                .newBuilder()
                                                                                .setId(1)
                                                                                .setUserId(100)
                                                                                .build())
                                                                .build()));

                when(orderQueryService.findById(
                                any(pb.order.OrderCommon.FindByIdOrderRequest.class)))
                                .thenReturn(Uni.createFrom().item(
                                                pb.order.OrderCommon.ApiResponseOrder.newBuilder()
                                                                .setData(pb.order.OrderCommon.OrderResponse
                                                                                .newBuilder()
                                                                                .setId(1)
                                                                                .setUserId(100)
                                                                                .build())
                                                                .build()));

                when(orderItemQueryService.findOrderItemByOrder(
                                any(pb.order_item.OrderItemCommon.FindByIdOrderItemRequest.class)))
                                .thenReturn(Uni.createFrom().item(
                                                pb.order_item.OrderItemCommon.ApiResponsesOrderItem.newBuilder()
                                                                .addData(pb.order_item.OrderItemCommon.OrderItemResponse
                                                                                .newBuilder()
                                                                                .setPrice(50000)
                                                                                .setQuantity(3)
                                                                                .build())
                                                                .build()));

                when(shippingQueryService.findByOrder(
                                any(pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.class)))
                                .thenReturn(Uni.createFrom().item(
                                                pb.shipping_address.ShippingAddressCommon.ApiResponseShipping
                                                                .newBuilder()
                                                                .setData(pb.shipping_address.ShippingAddressCommon.ShippingResponse
                                                                                .newBuilder()
                                                                                .setId(1)
                                                                                .setShippingCost(10000)
                                                                                .build())
                                                                .build()));

                when(userQueryService.findById(
                                any(pb.user.UserCommon.FindByIdUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(
                                                pb.user.UserCommon.ApiResponseUser.newBuilder()
                                                                .setStatus("success")
                                                                .setData(pb.user.UserCommon.UserResponse.newBuilder()
                                                                                .setEmail("test@test.com")
                                                                                .setFirstname("Test")
                                                                                .setLastname("User")
                                                                                .build())
                                                                .build()));

                lenient().when(kafkaService.sendMessage(anyString(), anyString(), any()))
                                .thenReturn(Uni.createFrom().voidItem());
        }

        @Nested
        @DisplayName("create transaction tests")
        class CreateTransactionTests {

                @Test
                @DisplayName("should successfully create transaction on happy path")
                void create_Success() {
                        CreateTransactionRequest req = createValidCreateRequest();
                        mockCreateDependencies();

                        when(transactionCommandRepo.persist(any(Transaction.class)))
                                        .thenAnswer(inv -> {
                                                Transaction t = inv.getArgument(0);
                                                t.setId(1L);
                                                return Uni.createFrom().item(t);
                                        });

                        ApiResponse<TransactionResponse> response = service.create(req)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transaction created successfully");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);

                        verify(redisService, atLeastOnce()).deleteReactive(anyString());
                }
        }

        @Nested
        @DisplayName("update transaction tests")
        class UpdateTransactionTests {

                @Test
                @DisplayName("should fail when transaction not found")
                void update_NotFound() {
                        UpdateTransactionRequest req = createValidUpdateRequest();

                        when(transactionQueryRepo.findTransactionById(1L))
                                        .thenReturn(Uni.createFrom().item(Optional.empty()));

                        assertThatThrownBy(() -> service.update(req).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Transaction not found");
                }

                @Test
                @DisplayName("should successfully update transaction on happy path")
                void update_Success() {
                        UpdateTransactionRequest req = createValidUpdateRequest();
                        Transaction existing = createTestTransaction(1L, 1, 1, 150000, PaymentStatus.PENDING);

                        when(transactionQueryRepo.findTransactionById(1L))
                                        .thenReturn(Uni.createFrom().item(Optional.of(existing)));

                        when(merchantQueryService.findById(
                                        any(pb.merchant.MerchantCommon.FindByIdMerchantRequest.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        pb.merchant.MerchantCommon.ApiResponseMerchant.newBuilder()
                                                                        .setData(pb.merchant.MerchantCommon.MerchantResponse
                                                                                        .newBuilder()
                                                                                        .setId(1)
                                                                                        .setUserId(100)
                                                                                        .build())
                                                                        .build()));

                        when(orderQueryService.findById(
                                        any(pb.order.OrderCommon.FindByIdOrderRequest.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        pb.order.OrderCommon.ApiResponseOrder.newBuilder()
                                                                        .setData(pb.order.OrderCommon.OrderResponse
                                                                                        .newBuilder()
                                                                                        .setId(1)
                                                                                        .setUserId(100)
                                                                                        .build())
                                                                        .build()));

                        when(orderItemQueryService.findOrderItemByOrder(
                                        any(pb.order_item.OrderItemCommon.FindByIdOrderItemRequest.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        pb.order_item.OrderItemCommon.ApiResponsesOrderItem.newBuilder()
                                                                        .addData(pb.order_item.OrderItemCommon.OrderItemResponse
                                                                                        .newBuilder()
                                                                                        .setPrice(50000)
                                                                                        .setQuantity(4)
                                                                                        .build())
                                                                        .build()));

                        when(shippingQueryService.findByOrder(
                                        any(pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        pb.shipping_address.ShippingAddressCommon.ApiResponseShipping
                                                                        .newBuilder()
                                                                        .setData(pb.shipping_address.ShippingAddressCommon.ShippingResponse
                                                                                        .newBuilder()
                                                                                        .setId(1)
                                                                                        .setShippingCost(10000)
                                                                                        .build())
                                                                        .build()));
                        when(transactionCommandRepo.persist(any(Transaction.class)))
                                        .thenAnswer(inv -> {
                                                Transaction t = inv.getArgument(0);
                                                return Uni.createFrom().item(t);
                                        });

                        ApiResponse<TransactionResponse> response = service.update(req)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transaction updated successfully");
                        assertThat(response.data().getId()).isEqualTo(1L);

                        verify(redisService, atLeastOnce()).deleteReactive(anyString());
                }
        }

        @Nested
        @DisplayName("trash transaction tests")
        class TrashTransactionTests {

                @Test
                @DisplayName("should successfully trash existing transaction")
                void trash_Success() {
                        int id = 1;
                        Transaction trashed = createTestTransaction(id, 1, 1, 150000, PaymentStatus.PENDING);

                        when(transactionCommandRepo.trashed((long) id))
                                        .thenReturn(Uni.createFrom().item(trashed));

                        ApiResponse<TransactionResponseDeleteAt> response = service.trash(id)
                                        .await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transaction trashed successfully");
                        assertThat(response.data().getId()).isEqualTo((long) id);
                }

                @Test
                @DisplayName("should fail when transaction not found for trash")
                void trash_NotFound() {
                        when(transactionCommandRepo.trashed(999L))
                                        .thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.trash(999).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Transaction not found or already trashed");
                }
        }

        @Nested
        @DisplayName("restore transaction tests")
        class RestoreTransactionTests {

                @Test
                @DisplayName("should successfully restore trashed transaction")
                void restore_Success() {
                        int id = 1;
                        Transaction restored = createTestTransaction(id, 1, 1, 150000, PaymentStatus.PENDING);

                        when(transactionCommandRepo.restore((long) id))
                                        .thenReturn(Uni.createFrom().item(restored));

                        ApiResponse<TransactionResponseDeleteAt> response = service.restore(id)
                                        .await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transaction restored successfully");
                        assertThat(response.data().getId()).isEqualTo((long) id);
                }

                @Test
                @DisplayName("should fail when transaction is not trashed")
                void restore_NotTrashed() {
                        when(transactionCommandRepo.restore(1L))
                                        .thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.restore(1).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Transaction not found or not trashed");
                }
        }

        @Nested
        @DisplayName("delete by id tests")
        class DeleteByIdTests {

                @Test
                @DisplayName("should successfully permanently delete trashed transaction")
                void deletePermanent_Success() {
                        int id = 1;
                        Transaction trashed = createTestTransaction(id, 1, 1, 150000, PaymentStatus.PENDING);
                        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

                        when(transactionCommandRepo.deletePermanent((long) id))
                                        .thenReturn(Uni.createFrom().item(trashed));

                        ApiResponse<Void> response = service.delete(id).await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transaction permanently deleted");
                }

                @Test
                @DisplayName("should fail when transaction not found")
                void deletePermanent_NotFound() {
                        when(transactionCommandRepo.deletePermanent(999L))
                                        .thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.delete(999).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining("Transaction not found or must be trashed before permanent deletion");
                }
        }

        @Nested
        @DisplayName("delete all trashed tests")
        class DeleteAllTests {

                @Test
                @DisplayName("should successfully delete all trashed transactions")
                void deleteAll_Success() {
                        ApiResponse<Void> response = service.deleteAll().await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("All transactions permanently deleted");
                }

                @Test
                @DisplayName("should fail when no trashed transactions to delete")
                void deleteAll_NoTrashed() {
                        when(transactionCommandRepo.deleteAllDeleted())
                                        .thenReturn(Uni.createFrom().item(false));

                        assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("No trashed transactions found");
                }
        }

        @Nested
        @DisplayName("restore all trashed tests")
        class RestoreAllTests {

                @Test
                @DisplayName("should successfully restore all trashed transactions")
                void restoreAll_Success() {
                        ApiResponse<Void> response = service.restoreAll().await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("All transactions restored successfully");
                }

                @Test
                @DisplayName("should fail when no trashed transactions to restore")
                void restoreAll_NoTrashed() {
                        when(transactionCommandRepo.restoreAllDeleted())
                                        .thenReturn(Uni.createFrom().item(false));

                        assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("No trashed transactions found");
                }
        }
}
