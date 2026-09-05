package com.sanedge.order.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.service.OrderQueryService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.order.OrderCommon;
import pb.order.OrderQuery;

@ExtendWith(MockitoExtension.class)
class OrderQueryGrpcHandlerTest {

    @Mock
    private OrderQueryService orderQueryService;

    private OrderQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderQueryGrpcHandler();
        handler.orderQueryService = orderQueryService;
    }

    private OrderResponse createMockResponse(Long id) {
        return OrderResponse.builder()
                .id(id)
                .merchantId(100)
                .userId(100)
                .totalPrice(500)
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();
    }

    private OrderResponseDeleteAt createMockResponseDeleteAt(Long id) {
        return OrderResponseDeleteAt.builder()
                .id(id)
                .merchantId(100)
                .userId(100)
                .totalPrice(500)
                .build();
    }

    @Test
    void findAll_success_returnsPaginationResponse() {
        OrderQuery.FindAllOrderRequest request = OrderQuery.FindAllOrderRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        List<OrderResponse> data = List.of(createMockResponse(1L));
        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
        ApiResponsePagination<List<OrderResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Orders retrieved successfully", data, meta);

        when(orderQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponsePaginationOrder response = handler.findAll(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
    }

    @Test
    void findAll_failure_returnsInternalError() {
        OrderQuery.FindAllOrderRequest request = OrderQuery.FindAllOrderRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        when(orderQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        StatusRuntimeException ex = null;
        try {
            handler.findAll(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }
        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    void findById_success_returnsOrder() {
        OrderCommon.FindByIdOrderRequest request = OrderCommon.FindByIdOrderRequest.newBuilder()
                .setId(1)
                .build();

        ApiResponse<OrderResponse> apiResp = ApiResponse.success("Order retrieved successfully",
                createMockResponse(1L));
        when(orderQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponseOrder response = handler.findById(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.hasData()).isTrue();
    }

    @Test
    void findById_notFound_returnsNotFoundStatus() {
        OrderCommon.FindByIdOrderRequest request = OrderCommon.FindByIdOrderRequest.newBuilder()
                .setId(999)
                .build();

        when(orderQueryService.findById(anyLong()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Order not found")));

        StatusRuntimeException ex = null;
        try {
            handler.findById(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }
        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    void findByActive_success_returnsPaginationResponse() {
        OrderQuery.FindAllOrderRequest request = OrderQuery.FindAllOrderRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        List<OrderResponseDeleteAt> data = List.of(createMockResponseDeleteAt(1L));
        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
        ApiResponsePagination<List<OrderResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Active orders retrieved successfully", data, meta);

        when(orderQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponsePaginationOrderDeleteAt response = handler.findByActive(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
    }

    @Test
    void findByTrashed_success_returnsPaginationResponse() {
        OrderQuery.FindAllOrderRequest request = OrderQuery.FindAllOrderRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        List<OrderResponseDeleteAt> data = List.of(createMockResponseDeleteAt(1L));
        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
        ApiResponsePagination<List<OrderResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Trashed orders retrieved successfully", data, meta);

        when(orderQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

        OrderCommon.ApiResponsePaginationOrderDeleteAt response = handler.findByTrashed(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
    }
}
