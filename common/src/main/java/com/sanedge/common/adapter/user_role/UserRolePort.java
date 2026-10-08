package com.sanedge.common.adapter.user_role;

import java.util.List;

import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.model.UserRole;

import io.smallrye.mutiny.Uni;

/**
 * Port user-role: resolve role milik user + (un)assign. Setara
 * {@code user_role.QueryRepository}/{@code CommandRepository} di Go. Di proto
 * Quarkus, keduanya dilayani oleh RoleQueryService/RoleCommandService.
 */
public interface UserRolePort {

    Uni<List<Role>> findByUserId(int userId);

    Uni<UserRole> assignRoleToUser(int userId, int roleId);

    Uni<Void> removeRoleFromUser(int userId, int roleId);
}
