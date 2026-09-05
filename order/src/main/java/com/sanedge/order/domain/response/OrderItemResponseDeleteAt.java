package com.sanedge.order.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDeleteAt {

    private Long id;
    private Integer orderId;
    private Integer productId;
    private Integer quantity;
    private Integer price;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static OrderItemResponseDeleteAt from(pb.order_item.OrderItemCommon.OrderItemResponseDeleteAt proto) {
        if (proto == null) {
            return null;
        }
        return OrderItemResponseDeleteAt.builder()
                .id((long) proto.getId())
                .orderId(proto.getOrderId())
                .productId(proto.getProductId())
                .quantity(proto.getQuantity())
                .price(proto.getPrice())
                .createdAt(proto.getCreatedAt())
                .updatedAt(proto.getUpdatedAt())
                .deletedAt(proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null)
                .build();
    }
}
