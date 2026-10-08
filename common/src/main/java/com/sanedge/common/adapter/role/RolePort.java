package com.sanedge.common.adapter.role;

import com.sanedge.common.adapter.model.Role;

import io.smallrye.mutiny.Uni;

/**
 * Port baca role. Setara {@code role.QueryRepository} di Go. Penugasan role
 * ada di {@link com.sanedge.common.adapter.user_role.UserRolePort}.
 */
public interface RolePort {

    /** @throws com.sanedge.common.exception.ResourceNotFoundException bila tidak ada. */
    Uni<Role> findById(int roleId);

    /** @throws com.sanedge.common.exception.ResourceNotFoundException bila tidak ada. */
    Uni<Role> findByName(String name);
}
