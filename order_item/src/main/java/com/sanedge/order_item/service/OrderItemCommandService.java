package com.sanedge.order_item.service;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order_item.domain.requests.CreateOrderItemRequest;
import com.sanedge.order_item.domain.requests.UpdateOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import io.smallrye.mutiny.Uni;

public interface OrderItemCommandService {
    Uni<ApiResponse<OrderItemResponse>> create(CreateOrderItemRequest request);
    Uni<ApiResponse<OrderItemResponse>> update(UpdateOrderItemRequest request);
    Uni<ApiResponse<OrderItemResponseDeleteAt>> trash(Long id);
    Uni<ApiResponse<OrderItemResponseDeleteAt>> restore(Long id);
    Uni<ApiResponse<Void>> deletePermanent(Long id);
    Uni<ApiResponse<Void>> restoreAll();
    Uni<ApiResponse<Void>> deleteAll();
    Uni<ApiResponse<Void>> deleteByOrderPermanent(Long orderId);
    Uni<ApiResponse<Void>> deleteByOrderRollback(Long orderId);
    Uni<ApiResponse<Integer>> calculateTotalPrice(Long orderId);
}
