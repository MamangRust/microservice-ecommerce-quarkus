package com.sanedge.order_item.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order_item.domain.requests.CreateOrderItemRequest;
import com.sanedge.order_item.domain.requests.UpdateOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import com.sanedge.order_item.service.OrderItemCommandService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.order_item.OrderItemCommand;
import pb.order_item.OrderItemCommon;

@ExtendWith(MockitoExtension.class)
class OrderItemCommandGrpcHandlerTest {

    @Mock
    private OrderItemCommandService orderItemCommandService;

    private OrderItemCommandGrpcHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new OrderItemCommandGrpcHandler();
        Field f = OrderItemCommandGrpcHandler.class.getDeclaredField("orderItemCommandService");
        f.setAccessible(true);
        f.set(handler, orderItemCommandService);
    }

    @Test
    @DisplayName("createOrderItem - should return success response when order item created successfully")
    void createOrderItem_Success() {
        OrderItemCommand.CreateOrderItemRecordRequest request = OrderItemCommand.CreateOrderItemRecordRequest
                .newBuilder()
                .setOrderId(1)
                .setProductId(100)
                .setQuantity(2)
                .setPrice(5000)
                .build();

        OrderItemResponse mockResponse = OrderItemResponse.builder()
                .id(1L)
                .orderId(1)
                .productId(100)
                .quantity(2)
                .price(5000)
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();

        ApiResponse<OrderItemResponse> apiResponse = ApiResponse.success("Order item created successfully",
                mockResponse);

        when(orderItemCommandService.create(any(CreateOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommon.ApiResponseOrderItem response = handler.createOrderItem(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Order item created successfully");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getOrderId()).isEqualTo(1);
        assertThat(response.getData().getProductId()).isEqualTo(100);
        assertThat(response.getData().getQuantity()).isEqualTo(2);
        assertThat(response.getData().getPrice()).isEqualTo(5000);
    }

    @Test
    @DisplayName("createOrderItem - should return NOT_FOUND when ResourceNotFoundException thrown")
    void createOrderItem_ResourceNotFound() {
        OrderItemCommand.CreateOrderItemRecordRequest request = OrderItemCommand.CreateOrderItemRecordRequest
                .newBuilder()
                .setOrderId(1)
                .setProductId(100)
                .setQuantity(2)
                .setPrice(5000)
                .build();

        when(orderItemCommandService.create(any(CreateOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Order not found")));

        StatusRuntimeException exception = null;
        try {
            handler.createOrderItem(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("createOrderItem - should return INTERNAL when generic exception thrown")
    void createOrderItem_InternalError() {
        OrderItemCommand.CreateOrderItemRecordRequest request = OrderItemCommand.CreateOrderItemRecordRequest
                .newBuilder()
                .setOrderId(1)
                .setProductId(100)
                .setQuantity(2)
                .setPrice(5000)
                .build();

        when(orderItemCommandService.create(any(CreateOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

        StatusRuntimeException exception = null;
        try {
            handler.createOrderItem(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    @DisplayName("updateOrderItem - should return success response when order item updated successfully")
    void updateOrderItem_Success() {
        OrderItemCommand.UpdateOrderItemRecordRequest request = OrderItemCommand.UpdateOrderItemRecordRequest
                .newBuilder()
                .setOrderItemId(1)
                .setQuantity(5)
                .setPrice(7500)
                .build();

        OrderItemResponse mockResponse = OrderItemResponse.builder()
                .id(1L)
                .orderId(1)
                .productId(100)
                .quantity(5)
                .price(7500)
                .createdAt("2024-01-01")
                .updatedAt("2024-01-02")
                .build();

        ApiResponse<OrderItemResponse> apiResponse = ApiResponse.success("Order item updated successfully",
                mockResponse);

        when(orderItemCommandService.update(any(UpdateOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommon.ApiResponseOrderItem response = handler.updateOrderItem(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Order item updated successfully");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().getQuantity()).isEqualTo(5);
        assertThat(response.getData().getPrice()).isEqualTo(7500);
    }

    @Test
    @DisplayName("updateOrderItem - should return NOT_FOUND when order item not found")
    void updateOrderItem_ResourceNotFound() {
        OrderItemCommand.UpdateOrderItemRecordRequest request = OrderItemCommand.UpdateOrderItemRecordRequest
                .newBuilder()
                .setOrderItemId(999)
                .setQuantity(5)
                .setPrice(7500)
                .build();

        when(orderItemCommandService.update(any(UpdateOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Order item not found")));

        StatusRuntimeException exception = null;
        try {
            handler.updateOrderItem(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("trashOrderItem - should return success response when order item trashed successfully")
    void trashOrderItem_Success() {
        OrderItemCommon.FindByIdOrderItemRequest request = OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                .setId(1)
                .build();

        OrderItemResponseDeleteAt mockResponse = OrderItemResponseDeleteAt.builder()
                .id(1L)
                .orderId(1)
                .productId(100)
                .quantity(2)
                .price(5000)
                .deletedAt("2024-01-02T10:00:00")
                .build();

        ApiResponse<OrderItemResponseDeleteAt> apiResponse = ApiResponse.success("Order item trashed successfully",
                mockResponse);

        when(orderItemCommandService.trash(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommon.ApiResponseOrderItem response = handler.trashOrderItem(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Order item trashed successfully");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("trashOrderItem - should return NOT_FOUND when order item not found")
    void trashOrderItem_NotFound() {
        OrderItemCommon.FindByIdOrderItemRequest request = OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                .setId(999)
                .build();

        when(orderItemCommandService.trash(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Order item not found")));

        StatusRuntimeException exception = null;
        try {
            handler.trashOrderItem(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("restoreOrderItem - should return success response when order item restored successfully")
    void restoreOrderItem_Success() {
        OrderItemCommon.FindByIdOrderItemRequest request = OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                .setId(1)
                .build();

        OrderItemResponseDeleteAt mockResponse = OrderItemResponseDeleteAt.builder()
                .id(1L)
                .orderId(1)
                .productId(100)
                .quantity(2)
                .price(5000)
                .build();

        ApiResponse<OrderItemResponseDeleteAt> apiResponse = ApiResponse.success("Order item restored successfully",
                mockResponse);

        when(orderItemCommandService.restore(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommon.ApiResponseOrderItem response = handler.restoreOrderItem(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Order item restored successfully");
    }

    @Test
    @DisplayName("restoreOrderItem - should return NOT_FOUND when order item not in trash")
    void restoreOrderItem_NotFound() {
        OrderItemCommon.FindByIdOrderItemRequest request = OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                .setId(999)
                .build();

        when(orderItemCommandService.restore(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Order item not found in trash")));

        StatusRuntimeException exception = null;
        try {
            handler.restoreOrderItem(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("deleteOrderItemPermanent - should return success when order item deleted permanently")
    void deleteOrderItemPermanent_Success() {
        OrderItemCommon.FindByIdOrderItemRequest request = OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                .setId(1)
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.success("Order item permanently deleted");

        when(orderItemCommandService.deletePermanent(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommon.ApiResponseOrderItemDelete response = handler.deleteOrderItemPermanent(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Order item permanently deleted");
    }

    @Test
    @DisplayName("deleteOrderItemPermanent - should return INVALID_ARGUMENT when order item not found or not trashed")
    void deleteOrderItemPermanent_NotFound() {
        OrderItemCommon.FindByIdOrderItemRequest request = OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                .setId(999)
                .build();

        when(orderItemCommandService.deletePermanent(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.InvalidRequestException(
                                "Order item not found or must be trashed before permanent deletion")));

        StatusRuntimeException exception = null;
        try {
            handler.deleteOrderItemPermanent(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    @DisplayName("restoreAllOrdersItem - should return success when all order items restored")
    void restoreAllOrdersItem_Success() {
        Empty request = Empty.getDefaultInstance();

        ApiResponse<Void> apiResponse = ApiResponse.success("All order items restored");

        when(orderItemCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommon.ApiResponseOrderItemAll response = handler.restoreAllOrdersItem(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All order items restored");
    }

    @Test
    @DisplayName("restoreAllOrdersItem - should return NOT_FOUND when no trashed order items found")
    void restoreAllOrdersItem_NotFound() {
        Empty request = Empty.getDefaultInstance();

        when(orderItemCommandService.restoreAll())
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("No trashed order items found")));

        StatusRuntimeException exception = null;
        try {
            handler.restoreAllOrdersItem(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("deleteAllPermanentOrdersItem - should return success when all order items deleted")
    void deleteAllPermanentOrdersItem_Success() {
        Empty request = Empty.getDefaultInstance();

        ApiResponse<Void> apiResponse = ApiResponse.success("All trashed order items permanently deleted");

        when(orderItemCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommon.ApiResponseOrderItemAll response = handler.deleteAllPermanentOrdersItem(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All trashed order items permanently deleted");
    }

    @Test
    @DisplayName("deleteAllPermanentOrdersItem - should return NOT_FOUND when no trashed order items found")
    void deleteAllPermanentOrdersItem_NotFound() {
        Empty request = Empty.getDefaultInstance();

        when(orderItemCommandService.deleteAll())
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("No trashed order items found")));

        StatusRuntimeException exception = null;
        try {
            handler.deleteAllPermanentOrdersItem(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("deleteOrderItemByOrderPermanent - should return success when order items deleted")
    void deleteOrderItemByOrderPermanent_Success() {
        OrderItemCommon.FindByIdOrderItemRequest request = OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                .setId(1)
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.success("Order items permanently deleted for order id: 1");

        when(orderItemCommandService.deleteByOrderPermanent(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommon.ApiResponseOrderItemDelete response = handler.deleteOrderItemByOrderPermanent(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Order items permanently deleted for order id: 1");
    }

    @Test
    @DisplayName("deleteOrderItemByOrderPermanent - should return NOT_FOUND when no order items found")
    void deleteOrderItemByOrderPermanent_NotFound() {
        OrderItemCommon.FindByIdOrderItemRequest request = OrderItemCommon.FindByIdOrderItemRequest.newBuilder()
                .setId(999)
                .build();

        when(orderItemCommandService.deleteByOrderPermanent(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException(
                                "No order items found for order id: 999")));

        StatusRuntimeException exception = null;
        try {
            handler.deleteOrderItemByOrderPermanent(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("calculateTotalPrice - should return success with total price")
    void calculateTotalPrice_Success() {
        OrderItemCommand.CalculateTotalPriceRequest request = OrderItemCommand.CalculateTotalPriceRequest.newBuilder()
                .setOrderId(1)
                .build();

        ApiResponse<Integer> apiResponse = ApiResponse.success("Total price calculated", 15000);

        when(orderItemCommandService.calculateTotalPrice(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        OrderItemCommand.CalculateTotalPriceResponse response = handler.calculateTotalPrice(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Total price calculated");
        assertThat(response.getTotalPrice()).isEqualTo(15000);
    }

    @Test
    @DisplayName("calculateTotalPrice - should return INTERNAL error when calculation fails")
    void calculateTotalPrice_Error() {
        OrderItemCommand.CalculateTotalPriceRequest request = OrderItemCommand.CalculateTotalPriceRequest.newBuilder()
                .setOrderId(1)
                .build();

        when(orderItemCommandService.calculateTotalPrice(1L))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Calculation error")));

        StatusRuntimeException exception = null;
        try {
            handler.calculateTotalPrice(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    @DisplayName("createOrderItem - should correctly map request fields to domain object")
    void createOrderItem_RequestMapping() {
        OrderItemCommand.CreateOrderItemRecordRequest request = OrderItemCommand.CreateOrderItemRecordRequest
                .newBuilder()
                .setOrderId(5)
                .setProductId(200)
                .setQuantity(3)
                .setPrice(10000)
                .build();

        OrderItemResponse mockResponse = OrderItemResponse.builder()
                .id(1L)
                .orderId(5)
                .productId(200)
                .quantity(3)
                .price(10000)
                .build();

        ApiResponse<OrderItemResponse> apiResponse = ApiResponse.success("Order item created", mockResponse);

        when(orderItemCommandService.create(any(CreateOrderItemRequest.class)))
                .thenAnswer(invocation -> {
                    CreateOrderItemRequest domainReq = invocation.getArgument(0);

                    assertThat(domainReq.getOrderId()).isEqualTo(5);
                    assertThat(domainReq.getProductId()).isEqualTo(200);
                    assertThat(domainReq.getQuantity()).isEqualTo(3);
                    assertThat(domainReq.getPrice()).isEqualTo(10000);
                    return Uni.createFrom().item(apiResponse);
                });

        OrderItemCommon.ApiResponseOrderItem response = handler.createOrderItem(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
    }
}
