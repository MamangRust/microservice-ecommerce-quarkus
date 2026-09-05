package com.sanedge.order_item.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order_item.domain.requests.FindAllOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.service.OrderItemQueryService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderItemQueryGrpcHandlerTest {
    @Mock OrderItemQueryService orderItemQueryService;
    private OrderItemQueryGrpcHandler orderItemQueryGrpcHandler;

    @BeforeEach
    void setUp() throws Exception {
        orderItemQueryGrpcHandler = new OrderItemQueryGrpcHandler();
        Field f = OrderItemQueryGrpcHandler.class.getDeclaredField("orderItemQueryService");
        f.setAccessible(true);
        f.set(orderItemQueryGrpcHandler, orderItemQueryService);
    }

    @Test
    void findAll_Success() {
        ApiResponse<List<OrderItemResponse>> resp = ApiResponse.success("Order items retrieved successfully", List.of());
        lenient().when(orderItemQueryService.findAll(any(FindAllOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(resp));
        var result = orderItemQueryGrpcHandler.findAll(
                pb.order_item.OrderItemQuery.FindAllOrderItemRequest.newBuilder()
                        .setPage(1).setPageSize(10).build()).await().indefinitely();
        assertThat(result).isNotNull();
    }
}
