package com.sanedge.common.adapter.model;

import java.time.Instant;

/** Domain model merchant (setara {@code models.Merchant} di Go). */
public record Merchant(
        int id,
        int userId,
        String name,
        String description,
        String address,
        String contactEmail,
        String contactPhone,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
