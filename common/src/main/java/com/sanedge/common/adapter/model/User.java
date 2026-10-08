package com.sanedge.common.adapter.model;

import java.time.Instant;

/**
 * Domain model user (setara {@code models.User} di Go). {@code password} hanya
 * terisi pada hasil lookup {@code findByEmail} (untuk verifikasi kredensial);
 * {@code null} pada lookup lain.
 */
public record User(
        int id,
        String firstname,
        String lastname,
        String email,
        String password,
        Instant createdAt,
        Instant updatedAt) {
}
