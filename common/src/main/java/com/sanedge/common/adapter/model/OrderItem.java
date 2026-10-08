package com.sanedge.common.adapter.model;

import java.time.Instant;

/** Domain model order item (setara {@code models.OrderItem} di Go). */
public record OrderItem(
        int id,
        int orderId,
        int productId,
        int quantity,
        int price,
        Instant createdAt,
        Instant updatedAt) {
}
