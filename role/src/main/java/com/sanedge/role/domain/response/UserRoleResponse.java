package com.sanedge.role.domain.response;

import com.sanedge.role.entity.UserRole;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RegisterForReflection
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleResponse {
    private Long userId;
    private RoleResponse role;

    public static UserRoleResponse from(UserRole userRole) {
        if (userRole == null) {
            return null;
        }
        return UserRoleResponse.builder()
                .userId(userRole.getUserId())
                .role(RoleResponse.from(userRole.getRole()))
                .build();
    }
}
