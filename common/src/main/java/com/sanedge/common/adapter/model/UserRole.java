package com.sanedge.common.adapter.model;

import java.time.Instant;

/** Domain model user-role (setara {@code models.UserRole} di Go). */
public record UserRole(
        int userRoleId,
        int userId,
        int roleId,
        Instant createdAt,
        Instant updatedAt) {
}
