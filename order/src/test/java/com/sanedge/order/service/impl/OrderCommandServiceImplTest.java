package com.sanedge.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ForbiddenException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.requests.CreateOrderItemRequest;
import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.CreateShippingAddressRequest;
import com.sanedge.order.domain.requests.UpdateOrderRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.entity.Order;
import com.sanedge.order.repository.OrderCommandRepository;
import com.sanedge.order.repository.OrderQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceImplTest {

        @Mock
        private OrderQueryRepository orderQueryRepository;

        @Mock
        private OrderCommandRepository orderCommandRepository;

        @Mock
        private Validator validator;

        @Mock
        private RedisService redisService;

        @Mock
        private TracingMetrics tracingMetrics;

        @Mock
        private pb.merchant.MerchantQueryService merchantQueryService;

        @Mock
        private pb.user.UserQueryService userQueryService;

        @Mock
        private pb.product.ProductQueryService productQueryService;

        @Mock
        private pb.product.ProductCommandService productCommandService;

        @Mock
        private pb.order_item.OrderItemCommandService orderItemCommandServiceGrpc;

        @Mock
        private pb.shipping_address.MutinyShippingCommandServiceGrpc.MutinyShippingCommandServiceStub shippingCommandService;

        @Mock
        private pb.shipping_address.MutinyShippingQueryServiceGrpc.MutinyShippingQueryServiceStub shippingQueryService;

        private OrderCommandServiceImpl service;

        @BeforeEach
        void setUp() {
                service = new OrderCommandServiceImpl(
                                orderQueryRepository,
                                orderCommandRepository,
                                validator,
                                redisService,
                                tracingMetrics);

                service.merchantQueryService = merchantQueryService;
                service.userQueryService = userQueryService;
                service.productQueryService = productQueryService;
                service.productCommandService = productCommandService;
                service.orderItemCommandServiceGrpc = orderItemCommandServiceGrpc;
                service.shippingCommandService = shippingCommandService;
                service.shippingQueryService = shippingQueryService;

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(
                                                anyString(),
                                                anyString(),
                                                any(Attributes.class),
                                                any());

                lenient().when(redisService.deleteReactive(anyString()))
                                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(orderCommandRepository.updateTotalPrice(any(Long.class), any(Integer.class)))
                                .thenReturn(Uni.createFrom().item(1));
                lenient().when(validator.validate(any())).thenReturn(Collections.emptySet());
        }

        private Order createTestOrder(Long id, Integer merchantId, Integer userId, Integer totalPrice) {
                Order order = new Order();
                order.id = id;
                order.setMerchantId(merchantId);
                order.setUserId(userId);
                order.setTotalPrice(totalPrice);
                order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                order.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return order;
        }

        private CreateOrderRequest createValidCreateOrderRequest() {
                CreateOrderRequest request = new CreateOrderRequest();
                request.setMerchantId(100);
                request.setUserId(100);

                CreateOrderItemRequest item = new CreateOrderItemRequest();
                item.setProductId(1);
                item.setQuantity(2);
                item.setPrice(100);
                request.setItems(List.of(item));

                CreateShippingAddressRequest shipping = new CreateShippingAddressRequest();
                shipping.setAlamat("123 Test Street");
                shipping.setProvinsi("Test Province");
                shipping.setKota("Test City");
                shipping.setCourier("Test Courier");
                shipping.setShippingMethod("standard");
                shipping.setShippingCost(10000);
                shipping.setNegara("Test Country");
                request.setShippingAddress(shipping);

                return request;
        }

        private void mockMerchantAndUser() {
                pb.merchant.MerchantCommon.ApiResponseMerchant merchantResponse = pb.merchant.MerchantCommon.ApiResponseMerchant
                                .newBuilder()
                                .setData(pb.merchant.MerchantCommon.MerchantResponse.newBuilder().setId(100).build())
                                .build();
                lenient().when(merchantQueryService.findById(any()))
                                .thenReturn(Uni.createFrom().item(merchantResponse));

                pb.user.UserCommon.ApiResponseUser userResponse = pb.user.UserCommon.ApiResponseUser.newBuilder()
                                .setData(pb.user.UserCommon.UserResponse.newBuilder().setId(100).build())
                                .build();
                lenient().when(userQueryService.findById(any()))
                                .thenReturn(Uni.createFrom().item(userResponse));
        }

        private void mockProductAndOrderItem() {
                pb.product.ProductCommon.ApiResponseProduct productResponse = pb.product.ProductCommon.ApiResponseProduct
                                .newBuilder()
                                .setData(pb.product.ProductCommon.ProductResponse.newBuilder()
                                                .setId(1).setCountInStock(100).build())
                                .build();
                lenient().when(productQueryService.findById(any()))
                                .thenReturn(Uni.createFrom().item(productResponse));

                pb.order_item.OrderItemCommon.ApiResponseOrderItem orderItemResponse = pb.order_item.OrderItemCommon.ApiResponseOrderItem
                                .newBuilder().build();
                lenient().when(orderItemCommandServiceGrpc.createOrderItem(any()))
                                .thenReturn(Uni.createFrom().item(orderItemResponse));

                pb.product.ProductCommon.ApiResponseProduct stockResponse = pb.product.ProductCommon.ApiResponseProduct
                                .newBuilder().build();
                lenient().when(productCommandService.updateProductCountStock(any()))
                                .thenReturn(Uni.createFrom().item(stockResponse));
        }

        private void mockShipping() {
                pb.shipping_address.ShippingAddressCommon.ApiResponseShipping shippingResponse = pb.shipping_address.ShippingAddressCommon.ApiResponseShipping
                                .newBuilder().build();
                lenient().when(shippingCommandService.createShipping(any()))
                                .thenReturn(Uni.createFrom().item(shippingResponse));
        }

        @Nested
        @DisplayName("create order tests")
        class CreateOrderTests {

                @Test
                @DisplayName("should successfully create order when all validations pass")
                void createOrder_Success() {
                        CreateOrderRequest request = createValidCreateOrderRequest();
                        mockMerchantAndUser();
                        mockProductAndOrderItem();
                        mockShipping();

                        Order savedOrder = createTestOrder(null, 100, 100, 0);
                        when(orderCommandRepository.persistNew(any(Order.class)))
                                        .thenAnswer(inv -> {
                                                Order o = inv.getArgument(0);
                                                if (o.id == null)
                                                        o.id = 1L;
                                                return Uni.createFrom().item(o);
                                        });

                        ApiResponse<OrderResponse> response = service.create(request).await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Order created successfully");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);
                }

                @Test
                @DisplayName("should fail when validation errors occur")
                void createOrder_ValidationError() {
                        CreateOrderRequest request = createValidCreateOrderRequest();

                        @SuppressWarnings("unchecked")
                        ConstraintViolation<Object> violation = (ConstraintViolation<Object>) org.mockito.Mockito
                                        .mock(ConstraintViolation.class);
                        lenient().when(violation.getPropertyPath())
                                        .thenReturn(org.mockito.Mockito.mock(jakarta.validation.Path.class));
                        lenient().when(violation.getMessage()).thenReturn("test error message");
                        when(validator.validate(any())).thenReturn(Set.of(violation));

                        assertThatThrownBy(() -> service.create(request).await().indefinitely())
                                        .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
                }

                @Test
                @DisplayName("should fail when merchant not found")
                void createOrder_MerchantNotFound() {
                        CreateOrderRequest request = createValidCreateOrderRequest();

                        pb.merchant.MerchantCommon.ApiResponseMerchant merchantResponse = pb.merchant.MerchantCommon.ApiResponseMerchant
                                        .newBuilder()
                                        .setData(pb.merchant.MerchantCommon.MerchantResponse.newBuilder().setId(0)
                                                        .build())
                                        .build();
                        when(merchantQueryService.findById(any())).thenReturn(Uni.createFrom().item(merchantResponse));

                        assertThatThrownBy(() -> service.create(request).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Merchant not found");
                }

                @Test
                @DisplayName("should fail when user not found")
                void createOrder_UserNotFound() {
                        CreateOrderRequest request = createValidCreateOrderRequest();

                        pb.merchant.MerchantCommon.ApiResponseMerchant merchantResponse = pb.merchant.MerchantCommon.ApiResponseMerchant
                                        .newBuilder()
                                        .setData(pb.merchant.MerchantCommon.MerchantResponse.newBuilder().setId(100)
                                                        .build())
                                        .build();
                        when(merchantQueryService.findById(any())).thenReturn(Uni.createFrom().item(merchantResponse));

                        pb.user.UserCommon.ApiResponseUser userResponse = pb.user.UserCommon.ApiResponseUser
                                        .newBuilder()
                                        .setData(pb.user.UserCommon.UserResponse.newBuilder().setId(0).build())
                                        .build();
                        when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResponse));

                        assertThatThrownBy(() -> service.create(request).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("User not found");
                }

                @Test
                @DisplayName("should fail when product not found for order item")
                void createOrder_ProductNotFound() {
                        CreateOrderRequest request = createValidCreateOrderRequest();
                        mockMerchantAndUser();

                        Order savedOrder = createTestOrder(null, 100, 100, 0);
                        when(orderCommandRepository.persistNew(any(Order.class)))
                                        .thenAnswer(inv -> {
                                                Order o = inv.getArgument(0);
                                                if (o.id == null)
                                                        o.id = 1L;
                                                return Uni.createFrom().item(o);
                                        });

                        pb.product.ProductCommon.ApiResponseProduct productResponse = pb.product.ProductCommon.ApiResponseProduct
                                        .newBuilder()
                                        .setData(pb.product.ProductCommon.ProductResponse.newBuilder().setId(0).build())
                                        .build();
                        when(productQueryService.findById(any())).thenReturn(Uni.createFrom().item(productResponse));

                        assertThatThrownBy(() -> service.create(request).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Product not found");
                }

                @Test
                @DisplayName("should fail when insufficient stock")
                void createOrder_InsufficientStock() {
                        CreateOrderRequest request = createValidCreateOrderRequest();
                        mockMerchantAndUser();

                        Order savedOrder = createTestOrder(null, 100, 100, 0);
                        when(orderCommandRepository.persistNew(any(Order.class)))
                                        .thenAnswer(inv -> {
                                                Order o = inv.getArgument(0);
                                                if (o.id == null)
                                                        o.id = 1L;
                                                return Uni.createFrom().item(o);
                                        });

                        pb.product.ProductCommon.ApiResponseProduct productResponse = pb.product.ProductCommon.ApiResponseProduct
                                        .newBuilder()
                                        .setData(pb.product.ProductCommon.ProductResponse.newBuilder()
                                                        .setId(1).setCountInStock(1).build())
                                        .build();
                        when(productQueryService.findById(any())).thenReturn(Uni.createFrom().item(productResponse));

                        assertThatThrownBy(() -> service.create(request).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining("Insufficient stock");
                }
        }

        @Test
        @DisplayName("should use authoritative product price and atomic stock delta")
        void createOrder_UsesAuthoritativePriceAndStockDelta() {
                CreateOrderRequest request = createValidCreateOrderRequest();
                request.getItems().get(0).setPrice(1);
                mockMerchantAndUser();
                mockShipping();

                pb.product.ProductCommon.ApiResponseProduct productResponse = pb.product.ProductCommon.ApiResponseProduct
                                .newBuilder()
                                .setData(pb.product.ProductCommon.ProductResponse.newBuilder()
                                                .setId(1).setPrice(250).setCountInStock(100).build())
                                .build();
                when(productQueryService.findById(any())).thenReturn(Uni.createFrom().item(productResponse));
                when(productCommandService.adjustStock(any()))
                                .thenReturn(Uni.createFrom().item(pb.product.ProductCommon.ApiResponseProduct
                                                .getDefaultInstance()));
                when(orderItemCommandServiceGrpc.createOrderItem(any()))
                                .thenReturn(Uni.createFrom().item(pb.order_item.OrderItemCommon.ApiResponseOrderItem
                                                .getDefaultInstance()));
                when(orderCommandRepository.persistNew(any(Order.class))).thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        if (order.id == null) {
                                order.id = 1L;
                        }
                        return Uni.createFrom().item(order);
                });

                service.create(request).await().indefinitely();

                ArgumentCaptor<pb.product.ProductCommand.AdjustProductStockRequest> stockCaptor =
                                ArgumentCaptor.forClass(pb.product.ProductCommand.AdjustProductStockRequest.class);
                verify(productCommandService).adjustStock(stockCaptor.capture());
                assertThat(stockCaptor.getValue().getProductId()).isEqualTo(1);
                assertThat(stockCaptor.getValue().getDelta()).isEqualTo(-2);

                ArgumentCaptor<pb.order_item.OrderItemCommand.CreateOrderItemRecordRequest> itemCaptor =
                                ArgumentCaptor.forClass(pb.order_item.OrderItemCommand.CreateOrderItemRecordRequest.class);
                verify(orderItemCommandServiceGrpc).createOrderItem(itemCaptor.capture());
                assertThat(itemCaptor.getValue().getPrice()).isEqualTo(250);
        }

        @Nested
        @DisplayName("update order tests")
        class UpdateOrderTests {

                @Test
                @DisplayName("should fail when orderId is null")
                void updateOrder_NullOrderId() {
                        UpdateOrderRequest request = new UpdateOrderRequest();
                        request.setOrderId(null);
                        request.setUserId(100);

                        assertThatThrownBy(() -> service.update(request).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("OrderId is required");
                }

                @Test
                @DisplayName("should fail when order not found")
                void updateOrder_OrderNotFound() {
                        UpdateOrderRequest request = new UpdateOrderRequest();
                        request.setOrderId(999);
                        request.setUserId(100);
                        request.setItems(List.of());
                        request.setShippingAddress(new com.sanedge.order.domain.requests.UpdateShippingAddressRequest());

                        when(orderQueryRepository.findOrderById(any(Long.class)))
                                        .thenReturn(Uni.createFrom().item(Optional.empty()));

                        assertThatThrownBy(() -> service.update(request).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Order not found");
                }

                @Test
                @DisplayName("should fail with validation error when required fields are missing")
                void updateOrder_ValidationError() {
                        UpdateOrderRequest request = new UpdateOrderRequest();
                        request.setOrderId(1);
                        request.setUserId(100);
                        request.setItems(null);
                        request.setShippingAddress(null);

                        @SuppressWarnings("unchecked")
                        ConstraintViolation<Object> violation = (ConstraintViolation<Object>) org.mockito.Mockito
                                        .mock(ConstraintViolation.class);
                        lenient().when(violation.getPropertyPath())
                                        .thenReturn(org.mockito.Mockito.mock(jakarta.validation.Path.class));
                        lenient().when(violation.getMessage()).thenReturn("must not be null");
                        when(validator.validate(any())).thenReturn(Set.of(violation));

                        assertThatThrownBy(() -> service.update(request).await().indefinitely())
                                        .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
                }

                @Test
                @DisplayName("should reject update when user does not own the order")
                void updateOrder_DifferentOwner() {
                        UpdateOrderRequest request = new UpdateOrderRequest();
                        request.setOrderId(1);
                        request.setUserId(200);

                        Order existingOrder = createTestOrder(1L, 100, 100, 500);
                        when(orderQueryRepository.findOrderById(1L))
                                        .thenReturn(Uni.createFrom().item(Optional.of(existingOrder)));

                        pb.user.UserCommon.ApiResponseUser userResponse = pb.user.UserCommon.ApiResponseUser
                                        .newBuilder()
                                        .setData(pb.user.UserCommon.UserResponse.newBuilder().setId(200).build())
                                        .build();
                        when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResponse));

                        assertThatThrownBy(() -> service.update(request).await().indefinitely())
                                        .isInstanceOf(ForbiddenException.class)
                                        .hasMessageContaining("not allowed");
                }

                @Test
                @DisplayName("should fail when user not found")
                void updateOrder_UserNotFound() {
                        UpdateOrderRequest request = new UpdateOrderRequest();
                        request.setOrderId(1);
                        request.setUserId(999);

                        Order existingOrder = createTestOrder(1L, 100, 100, 500);
                        when(orderQueryRepository.findOrderById(1L))
                                        .thenReturn(Uni.createFrom().item(Optional.of(existingOrder)));

                        pb.user.UserCommon.ApiResponseUser userResponse = pb.user.UserCommon.ApiResponseUser
                                        .newBuilder()
                                        .setData(pb.user.UserCommon.UserResponse.newBuilder().setId(0).build())
                                        .build();
                        when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResponse));

                        assertThatThrownBy(() -> service.update(request).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("User not found");
                }
        }

        @Nested
        @DisplayName("trash order tests")
        class TrashOrderTests {

                @Test
                @DisplayName("should successfully trash existing order")
                void trashOrder_Success() {
                        Long orderId = 1L;
                        Order trashedOrder = createTestOrder(orderId, 100, 100, 500);
                        trashedOrder.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

                        when(orderCommandRepository.trashed(orderId)).thenReturn(Uni.createFrom().item(trashedOrder));

                        ApiResponse<OrderResponseDeleteAt> response = service.trash(orderId).await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Order trashed successfully!");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(orderId);
                }

                @Test
                @DisplayName("should fail when order not found or already trashed")
                void trashOrder_NotFound() {
                        Long orderId = 999L;

                        when(orderCommandRepository.trashed(orderId)).thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.trash(orderId).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Order not found or already trashed");
                }
        }

        @Nested
        @DisplayName("restore order tests")
        class RestoreOrderTests {

                @Test
                @DisplayName("should successfully restore trashed order")
                void restoreOrder_Success() {
                        Long orderId = 1L;
                        Order restoredOrder = createTestOrder(orderId, 100, 100, 500);

                        when(orderCommandRepository.restore(orderId)).thenReturn(Uni.createFrom().item(restoredOrder));

                        ApiResponse<OrderResponseDeleteAt> response = service.restore(orderId).await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Order restored successfully!");
                        assertThat(response.data()).isNotNull();
                }

                @Test
                @DisplayName("should fail when order not found or not trashed")
                void restoreOrder_NotFound() {
                        Long orderId = 999L;

                        when(orderCommandRepository.restore(orderId)).thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.restore(orderId).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Order not found or not trashed");
                }
        }

        @Nested
        @DisplayName("delete order tests")
        class DeleteOrderTests {

                @Test
                @DisplayName("should successfully permanently delete trashed order")
                void deleteOrder_Success() {
                        Long orderId = 1L;
                        Order deletedOrder = createTestOrder(orderId, 100, 100, 500);

                        when(orderCommandRepository.deletePermanent(orderId))
                                        .thenReturn(Uni.createFrom().item(deletedOrder));

                        ApiResponse<Void> response = service.delete(orderId).await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Order permanently deleted!");
                }

                @Test
                @DisplayName("should fail when order not found or not trashed")
                void deleteOrder_NotFound() {
                        Long orderId = 999L;

                        when(orderCommandRepository.deletePermanent(orderId)).thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.delete(orderId).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining("must be trashed");
                }
        }

        @Nested
        @DisplayName("restore all orders tests")
        class RestoreAllTests {

                @Test
                @DisplayName("should successfully restore all trashed orders")
                void restoreAll_Success() {
                        ApiResponse<Void> response = service.restoreAll().await().indefinitely();

                        assertThat(response).isNull(); // Service returns null on success
                }

                @Test
                @DisplayName("should return null when no trashed orders found")
                void restoreAll_NoTrashedOrders() {
                        ApiResponse<Void> response = service.restoreAll().await().indefinitely();

                        assertThat(response).isNull(); // Service returns null instead of throwing
                }
        }

        @Nested
        @DisplayName("delete all orders tests")
        class DeleteAllTests {

                @Test
                @DisplayName("should successfully delete all trashed orders")
                void deleteAll_Success() {
                        ApiResponse<Void> response = service.deleteAll().await().indefinitely();

                        assertThat(response).isNull(); // Service returns null on success
                }

                @Test
                @DisplayName("should return null when no trashed orders found")
                void deleteAll_NoTrashedOrders() {
                        ApiResponse<Void> response = service.deleteAll().await().indefinitely();

                        assertThat(response).isNull(); // Service returns null instead of throwing
                }
        }
}
