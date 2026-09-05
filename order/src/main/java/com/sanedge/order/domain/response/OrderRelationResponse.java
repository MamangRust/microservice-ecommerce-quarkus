package com.sanedge.order.domain.response;

import java.util.List;

import com.sanedge.order.entity.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRelationResponse {
    private Long orderId;
    private Integer userId;
    private Integer merchantId;
    private Integer totalPrice;
    private List<OrderItemResponse> orderItems;

    public static OrderRelationResponse from(Order order, List<OrderItemResponse> orderItems) {
        if (order == null) {
            return null;
        }
        return OrderRelationResponse.builder()
                .orderId(order.id)
                .userId(order.getUserId())
                .merchantId(order.getMerchantId())
                .totalPrice(order.getTotalPrice())
                .orderItems(orderItems != null ? orderItems : List.of())
                .build();
    }
}
