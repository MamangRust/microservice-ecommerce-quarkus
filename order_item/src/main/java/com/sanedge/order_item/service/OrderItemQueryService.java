package com.sanedge.order_item.service;

import java.util.List;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order_item.domain.requests.FindAllOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import io.smallrye.mutiny.Uni;

public interface OrderItemQueryService {
    Uni<ApiResponse<List<OrderItemResponse>>> findAll(FindAllOrderItemRequest request);
    Uni<ApiResponse<List<OrderItemResponseDeleteAt>>> findActive(FindAllOrderItemRequest request);
    Uni<ApiResponse<List<OrderItemResponseDeleteAt>>> findTrashed(FindAllOrderItemRequest request);
    Uni<ApiResponse<List<OrderItemResponse>>> findByOrder(Long orderId);
}
