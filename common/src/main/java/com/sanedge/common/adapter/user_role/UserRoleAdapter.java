package com.sanedge.common.adapter.user_role;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Empty;
import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.model.UserRole;
import com.sanedge.common.adapter.role.RoleAdapter;
import com.sanedge.common.adapter.support.AdapterException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.role.MutinyRoleCommandServiceGrpc.MutinyRoleCommandServiceStub;
import pb.role.MutinyRoleQueryServiceGrpc.MutinyRoleQueryServiceStub;
import pb.role.RoleCommon.AssignRoleToUserRequest;
import pb.role.RoleCommon.RemoveRoleFromUserRequest;
import pb.role.RoleCommon.UserRoleResponse;
import pb.role.RoleQuery.FindByIdUserRoleRequest;

@ApplicationScoped
public class UserRoleAdapter implements UserRolePort {

    @GrpcClient("role")
    MutinyRoleQueryServiceStub query;

    @GrpcClient("role")
    MutinyRoleCommandServiceStub command;

    @Override
    public Uni<List<Role>> findByUserId(int userId) {
        return query.findByUserId(FindByIdUserRoleRequest.newBuilder().setUserId(userId).build())
                .map(resp -> {
                    List<Role> roles = new ArrayList<>(resp.getDataList().size());
                    for (var role : resp.getDataList()) {
                        roles.add(RoleAdapter.toModel(role));
                    }
                    return roles;
                });
    }

    @Override
    public Uni<UserRole> assignRoleToUser(int userId, int roleId) {
        return command.assignRoleToUser(AssignRoleToUserRequest.newBuilder()
                .setUserId(userId)
                .setRoleId(roleId)
                .build())
                .map(resp -> {
                    if (!resp.hasData()) {
                        throw new AdapterException("Failed to assign role " + roleId + " to user " + userId);
                    }
                    return toModel(resp.getData());
                });
    }

    @Override
    public Uni<Void> removeRoleFromUser(int userId, int roleId) {
        return command.removeRoleFromUser(RemoveRoleFromUserRequest.newBuilder()
                .setUserId(userId)
                .setRoleId(roleId)
                .build())
                .replaceWithVoid();
    }

    static UserRole toModel(UserRoleResponse ur) {
        if (ur == null) {
            return null;
        }
        return new UserRole(
                ur.getUserRoleId(),
                ur.getUserId(),
                ur.getRoleId(),
                com.sanedge.common.adapter.support.ProtoTime.parse(ur.getCreatedAt()),
                com.sanedge.common.adapter.support.ProtoTime.parse(ur.getUpdatedAt()));
    }
}
