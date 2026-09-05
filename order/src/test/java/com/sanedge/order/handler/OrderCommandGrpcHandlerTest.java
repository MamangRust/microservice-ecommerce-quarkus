package com.sanedge.order.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.service.OrderCommandService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.order.OrderCommand;
import pb.order.OrderCommon;
import pb.order.OrderCommon.FindByIdOrderRequest;

@ExtendWith(MockitoExtension.class)
class OrderCommandGrpcHandlerTest {

    @Mock
    private OrderCommandService orderCommandService;

    private OrderCommandGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderCommandGrpcHandler();
        handler.orderCommandService = orderCommandService;
    }

    private OrderResponse createOrderResponse(Long id, Integer totalPrice) {
        return OrderResponse.builder()
                .id(id)
                .merchantId(100)
                .userId(100)
                .totalPrice(totalPrice)
                .createdAt("2024-01-01 00:00:00.0")
                .updatedAt("2024-01-01 00:00:00.0")
                .build();
    }

    private OrderResponseDeleteAt createOrderResponseDeleteAt(Long id) {
        return OrderResponseDeleteAt.builder()
                .id(id)
                .merchantId(100)
                .userId(100)
                .totalPrice(500)
                .createdAt("2024-01-01 00:00:00.0")
                .updatedAt("2024-01-01 00:00:00.0")
                .deletedAt("2024-01-02 00:00:00.0")
                .build();
    }

    private OrderCommand.CreateOrderRequest createValidProtoCreateRequest() {
        OrderCommand.CreateOrderRequest request = OrderCommand.CreateOrderRequest.newBuilder()
                .setMerchantId(100)
                .setUserId(100)
                .addItems(OrderCommand.CreateOrderItemRequest.newBuilder()
                        .setProductId(1).setQuantity(2).setPrice(100).build())
                .setShipping(pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest.newBuilder()
                        .setOrderId(0)
                        .setAlamat("123 Test St")
                        .setProvinsi("Test Prov")
                        .setKota("Test City")
                        .setCourier("Test Courier")
                        .setShippingMethod("standard")
                        .setShippingCost(10000)
                        .setNegara("Test Country")
                        .build())
                .build();
        return request;
    }

    @Test
    @DisplayName("create - should return ApiResponseOrder on success")
    void create_Success() {
        OrderCommand.CreateOrderRequest request = createValidProtoCreateRequest();

        OrderResponse data = createOrderResponse(1L, 200);
        ApiResponse<OrderResponse> apiResp = new ApiResponse<>("success", "Order created successfully", data);

        when(orderCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponseOrder response = handler.create(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Order created successfully");
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getTotalPrice()).isEqualTo(200);
    }

    @Test
    @DisplayName("create - should return NOT_FOUND when ResourceNotFoundException thrown")
    void create_NotFound() {
        OrderCommand.CreateOrderRequest request = createValidProtoCreateRequest();

        when(orderCommandService.create(any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Merchant not found")));

        StatusRuntimeException ex = null;
        try {
            handler.create(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        assertThat(ex.getStatus().getDescription()).contains("Merchant not found");
    }

    @Test
    @DisplayName("create - should return INTERNAL on generic exception")
    void create_InternalError() {
        OrderCommand.CreateOrderRequest request = createValidProtoCreateRequest();

        when(orderCommandService.create(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        StatusRuntimeException ex = null;
        try {
            handler.create(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    @DisplayName("update - should return ApiResponseOrder on success")
    void update_Success() {
        OrderCommand.UpdateOrderRequest request = OrderCommand.UpdateOrderRequest.newBuilder()
                .setOrderId(1)
                .setUserId(100)
                .addItems(OrderCommand.UpdateOrderItemRequest.newBuilder()
                        .setOrderItemId(10).setProductId(1).setQuantity(3).setPrice(150).build())
                .setShipping(pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest.newBuilder()
                        .setShippingId(5)
                        .setOrderId(1)
                        .setAlamat("Updated St")
                        .setCourier("Updated Courier")
                        .setShippingMethod("express")
                        .setShippingCost(20000)
                        .build())
                .build();

        OrderResponse data = createOrderResponse(1L, 450);
        ApiResponse<OrderResponse> apiResp = new ApiResponse<>("success", "Order updated successfully", data);

        when(orderCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponseOrder response = handler.update(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("update - should return NOT_FOUND when order not found")
    void update_NotFound() {
        OrderCommand.UpdateOrderRequest request = OrderCommand.UpdateOrderRequest.newBuilder()
                .setOrderId(999)
                .setUserId(100)
                .build();

        when(orderCommandService.update(any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Order not found")));

        StatusRuntimeException ex = null;
        try {
            handler.update(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("trashedOrder - should return ApiResponseOrderDeleteAt on success")
    void trashedOrder_Success() {
        FindByIdOrderRequest request = FindByIdOrderRequest.newBuilder().setId(100).build();

        OrderResponseDeleteAt data = createOrderResponseDeleteAt(100L);
        ApiResponse<OrderResponseDeleteAt> apiResp = new ApiResponse<>("success", "Order trashed successfully!", data);

        when(orderCommandService.trash(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponseOrderDeleteAt response = handler.trashedOrder(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(100);
        assertThat(response.getData().hasDeletedAt()).isTrue();
        assertThat(response.getData().getDeletedAt().getValue()).isEqualTo("2024-01-02 00:00:00.0");
    }

    @Test
    @DisplayName("trashedOrder - should return INVALID_ARGUMENT when id <= 0")
    void trashedOrder_InvalidId_ReturnsInvalidArgument() {
        FindByIdOrderRequest request = FindByIdOrderRequest.newBuilder().setId(0).build();

        StatusRuntimeException ex = null;
        try {
            handler.trashedOrder(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
        assertThat(ex.getStatus().getDescription()).contains("must be a positive integer");
    }

    @Test
    @DisplayName("update - should return INVALID_ARGUMENT when orderId <= 0")
    void update_InvalidOrderId_ReturnsInvalidArgument() {
        OrderCommand.UpdateOrderRequest request = OrderCommand.UpdateOrderRequest.newBuilder()
                .setOrderId(0)
                .setUserId(100)
                .build();

        StatusRuntimeException ex = null;
        try {
            handler.update(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
        assertThat(ex.getStatus().getDescription()).contains("must be a positive integer");
    }

    @Test
    @DisplayName("create - should return INVALID_ARGUMENT when item productId <= 0")
    void create_InvalidItemProductId_ReturnsInvalidArgument() {
        OrderCommand.CreateOrderRequest request = OrderCommand.CreateOrderRequest.newBuilder()
                .setMerchantId(100)
                .setUserId(100)
                .addItems(OrderCommand.CreateOrderItemRequest.newBuilder()
                        .setProductId(0).setQuantity(2).setPrice(100).build())
                .build();

        StatusRuntimeException ex = null;
        try {
            handler.create(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
        assertThat(ex.getStatus().getDescription()).contains("Product id");
    }

    @Test
    @DisplayName("update - should return INVALID_ARGUMENT when item productId <= 0")
    void update_InvalidItemProductId_ReturnsInvalidArgument() {
        OrderCommand.UpdateOrderRequest request = OrderCommand.UpdateOrderRequest.newBuilder()
                .setOrderId(1)
                .setUserId(100)
                .addItems(OrderCommand.UpdateOrderItemRequest.newBuilder()
                        .setOrderItemId(10).setProductId(0).setQuantity(3).setPrice(150).build())
                .build();

        StatusRuntimeException ex = null;
        try {
            handler.update(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
        assertThat(ex.getStatus().getDescription()).contains("Product id");
    }

    @Test
    @DisplayName("update - should return INVALID_ARGUMENT when shipping shippingId <= 0")
    void update_InvalidShippingId_ReturnsInvalidArgument() {
        OrderCommand.UpdateOrderRequest request = OrderCommand.UpdateOrderRequest.newBuilder()
                .setOrderId(1)
                .setUserId(100)
                .setShipping(pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest.newBuilder()
                        .setShippingId(0)
                        .setOrderId(1)
                        .build())
                .build();

        StatusRuntimeException ex = null;
        try {
            handler.update(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
        assertThat(ex.getStatus().getDescription()).contains("Shipping id");
    }

    @Test
    @DisplayName("trashedOrder - should return NOT_FOUND when order not found")
    void trashedOrder_NotFound() {
        FindByIdOrderRequest request = FindByIdOrderRequest.newBuilder().setId(999).build();

        when(orderCommandService.trash(anyLong()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Order not found")));

        StatusRuntimeException ex = null;
        try {
            handler.trashedOrder(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("restoreOrder - should return ApiResponseOrderDeleteAt on success")
    void restoreOrder_Success() {
        FindByIdOrderRequest request = FindByIdOrderRequest.newBuilder().setId(100).build();

        OrderResponseDeleteAt data = OrderResponseDeleteAt.builder()
                .id(100L).merchantId(100).userId(100).totalPrice(500)
                .createdAt("2024-01-01 00:00:00.0")
                .updatedAt("2024-01-02 00:00:00.0")
                .build();
        ApiResponse<OrderResponseDeleteAt> apiResp = new ApiResponse<>("success", "Order restored successfully!", data);

        when(orderCommandService.restore(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponseOrderDeleteAt response = handler.restoreOrder(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(100);
    }

    @Test
    @DisplayName("restoreOrder - should return NOT_FOUND when order not found")
    void restoreOrder_NotFound() {
        FindByIdOrderRequest request = FindByIdOrderRequest.newBuilder().setId(999).build();

        when(orderCommandService.restore(anyLong()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Order not found or not trashed")));

        StatusRuntimeException ex = null;
        try {
            handler.restoreOrder(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("deleteOrderPermanent - should return ApiResponseOrderDelete on success")
    void deleteOrderPermanent_Success() {
        FindByIdOrderRequest request = FindByIdOrderRequest.newBuilder().setId(100).build();

        ApiResponse<Void> apiResp = ApiResponse.success("Order permanently deleted!");
        when(orderCommandService.delete(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponseOrderDelete response = handler.deleteOrderPermanent(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Order permanently deleted!");
    }

    @Test
    @DisplayName("deleteOrderPermanent - should return INTERNAL when order not found")
    void deleteOrderPermanent_InternalError() {
        FindByIdOrderRequest request = FindByIdOrderRequest.newBuilder().setId(999).build();

        when(orderCommandService.delete(anyLong()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Must be trashed first")));

        StatusRuntimeException ex = null;
        try {
            handler.deleteOrderPermanent(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    @DisplayName("restoreAllOrder - should return ApiResponseOrderAll on success")
    void restoreAllOrder_Success() {
        ApiResponse<Void> apiResp = ApiResponse.success("All orders restored successfully!");
        when(orderCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponseOrderAll response = handler.restoreAllOrder(Empty.getDefaultInstance()).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All orders restored successfully!");
    }

    @Test
    @DisplayName("restoreAllOrder - should return INTERNAL on failure")
    void restoreAllOrder_InternalError() {
        when(orderCommandService.restoreAll())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        StatusRuntimeException ex = null;
        try {
            handler.restoreAllOrder(Empty.getDefaultInstance()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    @DisplayName("deleteAllOrderPermanent - should return ApiResponseOrderAll on success")
    void deleteAllOrderPermanent_Success() {
        ApiResponse<Void> apiResp = ApiResponse.success("All orders permanently deleted!");
        when(orderCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponseOrderAll response = handler.deleteAllOrderPermanent(Empty.getDefaultInstance()).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All orders permanently deleted!");
    }

    @Test
    @DisplayName("deleteAllOrderPermanent - should return INTERNAL on failure")
    void deleteAllOrderPermanent_InternalError() {
        when(orderCommandService.deleteAll())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        StatusRuntimeException ex = null;
        try {
            handler.deleteAllOrderPermanent(Empty.getDefaultInstance()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }
}
