package com.sanedge.common.adapter.role;

import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.role.MutinyRoleQueryServiceGrpc.MutinyRoleQueryServiceStub;
import pb.role.RoleCommon.FindByIdRoleRequest;
import pb.role.RoleCommon.RoleResponse;
import pb.role.RoleQuery.FindByNameRoleRequest;

@ApplicationScoped
public class RoleAdapter implements RolePort {

    @GrpcClient("role")
    MutinyRoleQueryServiceStub client;

    @Override
    public Uni<Role> findById(int roleId) {
        return client.findByIdRole(FindByIdRoleRequest.newBuilder().setRoleId(roleId).build())
                .map(resp -> {
                    if (!resp.hasData()) {
                        throw new ResourceNotFoundException("Role not found: " + roleId);
                    }
                    return toModel(resp.getData());
                });
    }

    @Override
    public Uni<Role> findByName(String name) {
        return client.findByNameRole(FindByNameRoleRequest.newBuilder().setName(name).build())
                .map(resp -> {
                    if (!resp.hasData()) {
                        throw new ResourceNotFoundException("Role not found: " + name);
                    }
                    return toModel(resp.getData());
                });
    }

    public static Role toModel(RoleResponse r) {
        if (r == null) {
            return null;
        }
        return new Role(
                r.getId(),
                r.getName(),
                ProtoTime.parse(r.getCreatedAt()),
                ProtoTime.parse(r.getUpdatedAt()));
    }
}
