package com.sanedge.role.handler;

import com.google.protobuf.Empty;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.service.RoleCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.role.MutinyRoleCommandServiceGrpc;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;
import pb.role.RoleCommon.ApiResponseRole;
import pb.role.RoleCommon.ApiResponseRoleAll;
import pb.role.RoleCommon.ApiResponseRoleDelete;
import pb.role.RoleCommon.ApiResponseRoleDeleteAt;
import pb.role.RoleCommon.FindByIdRoleRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class RoleCommandGrpcHandler extends MutinyRoleCommandServiceGrpc.RoleCommandServiceImplBase {
    @Inject
    RoleCommandService roleCommandService;

    @Override
    public Uni<ApiResponseRole> createRole(CreateRoleRequest request) {
        com.sanedge.role.domain.requests.CreateRoleRequest domainReq = new com.sanedge.role.domain.requests.CreateRoleRequest();
        domainReq.setName(request.getName());
        return roleCommandService.create(domainReq)
                .map(res -> buildApiResponse(res.data()))
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseRole> updateRole(UpdateRoleRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        com.sanedge.role.domain.requests.UpdateRoleRequest domainReq = new com.sanedge.role.domain.requests.UpdateRoleRequest();
        domainReq.setRoleId(request.getId());
        domainReq.setName(request.getName());
        return roleCommandService.update(domainReq)
                .map(res -> buildApiResponse(res.data()))
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseRoleDeleteAt> trashedRole(FindByIdRoleRequest request) {
        if (request.getRoleId() <= 0) {
            return IdValidator.invalid("Role id");
        }
        return roleCommandService.trash((long) request.getRoleId())
                .map(res -> buildApiResponseWithDeletedAt(res.data()))
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseRoleDeleteAt> restoreRole(FindByIdRoleRequest request) {
        if (request.getRoleId() <= 0) {
            return IdValidator.invalid("Role id");
        }
        return roleCommandService.restore((long) request.getRoleId())
                .map(res -> buildApiResponseWithDeletedAt(res.data()))
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseRoleDelete> deleteRolePermanent(FindByIdRoleRequest request) {
        if (request.getRoleId() <= 0) {
            return IdValidator.invalid("Role id");
        }
        return roleCommandService.deletePermanent((long) request.getRoleId())
                .map(res -> ApiResponseRoleDelete.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseRoleAll> restoreAllRole(Empty request) {
        return roleCommandService.restoreAllTrashedRoles()
                .map(res -> ApiResponseRoleAll.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseRoleAll> deleteAllRolePermanent(Empty request) {
        return roleCommandService.deleteAllTrashedRoles()
                .map(res -> ApiResponseRoleAll.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<pb.role.RoleCommon.ApiResponseUserRole> assignRoleToUser(
            pb.role.RoleCommon.AssignRoleToUserRequest request) {
        if (request.getUserId() <= 0) {
            return IdValidator.invalid("User id");
        }
        if (request.getRoleId() <= 0) {
            return IdValidator.invalid("Role id");
        }
        return roleCommandService.assignRoleToUser((long) request.getUserId(), (long) request.getRoleId())
                .map(res -> {
                    pb.role.RoleCommon.UserRoleResponse.Builder builder = pb.role.RoleCommon.UserRoleResponse
                            .newBuilder()
                            .setUserId(res.data() != null && res.data().getUserId() != null
                                    ? res.data().getUserId().intValue()
                                    : 0);
                    if (res.data() != null && res.data().getRole() != null) {
                        builder.setRoleId(res.data().getRole().getId());
                    }
                    return pb.role.RoleCommon.ApiResponseUserRole.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message())
                            .setData(builder)
                            .build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<Empty> removeRoleFromUser(pb.role.RoleCommon.RemoveRoleFromUserRequest request) {
        if (request.getUserId() <= 0) {
            return IdValidator.invalid("User id");
        }
        if (request.getRoleId() <= 0) {
            return IdValidator.invalid("Role id");
        }
        return roleCommandService.removeRoleFromUser((long) request.getUserId(), (long) request.getRoleId())
                .map(res -> Empty.getDefaultInstance())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.role.RoleCommon.RoleResponse toProto(RoleResponse r) {
        if (r == null) {
            return pb.role.RoleCommon.RoleResponse.getDefaultInstance();
        }
        pb.role.RoleCommon.RoleResponse.Builder builder = pb.role.RoleCommon.RoleResponse.newBuilder()
                .setId(r.getId())
                .setName(r.getName());
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.role.RoleCommon.RoleResponseDeleteAt toProto(RoleResponseDeleteAt r) {
        if (r == null) {
            return pb.role.RoleCommon.RoleResponseDeleteAt.getDefaultInstance();
        }
        pb.role.RoleCommon.RoleResponseDeleteAt.Builder builder = pb.role.RoleCommon.RoleResponseDeleteAt.newBuilder()
                .setId(r.getId())
                .setName(r.getName());
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }

    private ApiResponseRole buildApiResponse(RoleResponse response) {
        return ApiResponseRole.newBuilder()
                .setStatus("success")
                .setMessage("Success")
                .setData(toProto(response))
                .build();
    }

    private ApiResponseRoleDeleteAt buildApiResponseWithDeletedAt(RoleResponseDeleteAt response) {
        return ApiResponseRoleDeleteAt.newBuilder()
                .setStatus("success")
                .setMessage("Success")
                .setData(toProto(response))
                .build();
    }
}
