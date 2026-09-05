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
public class OrderResponseDeleteAt {

    private Long id;
    private Integer merchantId;
    private Integer userId;
    private Integer totalPrice;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static OrderResponseDeleteAt from(Order entity) {
        if (entity == null) {
            return null;
        }
        return OrderResponseDeleteAt.builder()
                .id(entity.id)
                .merchantId(entity.getMerchantId())
                .userId(entity.getUserId())
                .totalPrice(entity.getTotalPrice())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .deletedAt(entity.getDeletedAt() != null ? entity.getDeletedAt().toString() : null)
                .build();
    }
}
