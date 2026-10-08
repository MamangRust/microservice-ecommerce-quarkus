package com.sanedge.order_item.service;

import java.util.List;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.order_item.domain.requests.FindAllOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface OrderItemQueryService {
    Uni<ApiResponsePagination<List<OrderItemResponse>>> findAll(FindAllOrderItemRequest request);

    Uni<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> findActive(FindAllOrderItemRequest request);

    Uni<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> findTrashed(FindAllOrderItemRequest request);

    Uni<ApiResponse<List<OrderItemResponse>>> findByOrder(Long orderId);
}
