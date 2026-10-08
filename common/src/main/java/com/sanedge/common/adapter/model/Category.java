package com.sanedge.common.adapter.model;

import java.time.Instant;

/** Domain model category (setara {@code models.Category} di Go). */
public record Category(
        int id,
        String name,
        String description,
        String slugCategory,
        String imageCategory,
        Instant createdAt,
        Instant updatedAt) {
}
