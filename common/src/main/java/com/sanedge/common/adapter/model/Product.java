package com.sanedge.common.adapter.model;

import java.time.Instant;

/** Domain model product (setara {@code models.Product} di Go). */
public record Product(
        int id,
        int merchantId,
        int categoryId,
        String name,
        String description,
        int price,
        int countInStock,
        String brand,
        int weight,
        float rating,
        String slugProduct,
        String imageProduct,
        Instant createdAt,
        Instant updatedAt) {
}
