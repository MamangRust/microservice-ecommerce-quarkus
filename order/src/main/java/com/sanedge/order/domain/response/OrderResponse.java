package com.sanedge.order.domain.response;

import com.sanedge.order.entity.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Integer merchantId;
    private Integer userId;
    private Integer totalPrice;
    private String createdAt;
    private String updatedAt;

    public static OrderResponse from(Order entity) {
        if (entity == null) {
            return null;
        }
        return OrderResponse.builder()
                .id(entity.id)
                .merchantId(entity.getMerchantId())
                .userId(entity.getUserId())
                .totalPrice(entity.getTotalPrice())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();
    }
}
