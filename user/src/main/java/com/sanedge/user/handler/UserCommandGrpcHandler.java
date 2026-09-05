package com.sanedge.user.handler;

import com.sanedge.user.domain.requests.RegisterRequest;
import com.sanedge.user.service.UserCommandService;
import com.sanedge.user.service.UserQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import pb.user.MutinyUserCommandServiceGrpc;
import pb.user.UserCommon.ApiResponseUser;
import pb.user.UserCommon.ApiResponseUserDeleteAt;
import pb.user.UserCommon.FindByIdUserRequest;
import pb.user.UserCommon.UserResponse;
import pb.user.UserCommon.UserResponseDeleteAt;
import pb.user.UserCommon.ApiResponseUserAll;
import pb.user.UserCommon.ApiResponseUserDelete;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;
import pb.user.UserCommand.VerifyPasswordRequest;
import pb.user.UserCommand.VerifyPasswordResponse;
import com.sanedge.common.utils.IdValidator;

@GrpcService
public class UserCommandGrpcHandler extends MutinyUserCommandServiceGrpc.UserCommandServiceImplBase {
    @Inject
    UserCommandService userCommandService;
    @Inject
    UserQueryService userQueryService;

    @Override
    public Uni<ApiResponseUser> create(CreateUserRequest request) {
        RegisterRequest req = new RegisterRequest();
        req.setFirstname(request.getFirstname());
        req.setLastname(request.getLastname());
        req.setEmail(request.getEmail());
        req.setPassword(request.getPassword());
        req.setConfirmPassword(request.getConfirmPassword());
        return userCommandService.createUser(req)
                .map(res -> buildApiResponse(res.data()));
    }

    @Override
    public Uni<ApiResponseUser> update(UpdateUserRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        com.sanedge.user.domain.requests.UpdateUserRequest req = com.sanedge.user.domain.requests.UpdateUserRequest
                .builder()
                .id(request.getId())
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(request.getPassword())
                .confirmPassword(request.getConfirmPassword())
                .build();
        return userCommandService.updateUser(req)
                .map(res -> buildApiResponse(res.data()));
    }

    @Override
    public Uni<ApiResponseUserDeleteAt> trashedUser(FindByIdUserRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return userCommandService.trashed((long) request.getId())
                .map(res -> buildApiResponseWithDeletedAt(res.data()));
    }

    @Override
    public Uni<ApiResponseUserDeleteAt> restoreUser(FindByIdUserRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return userCommandService.restore((long) request.getId())
                .map(res -> buildApiResponseWithDeletedAt(res.data()));
    }

    @Override
    public Uni<ApiResponseUserDelete> deleteUserPermanent(FindByIdUserRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return userCommandService.deletePermanent((long) request.getId())
                .map(res -> ApiResponseUserDelete.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build());
    }

    @Override
    public Uni<ApiResponseUserAll> restoreAllUser(com.google.protobuf.Empty request) {
        return userCommandService.restoreAllTrashedUsers()
                .map(res -> ApiResponseUserAll.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build());
    }

    @Override
    public Uni<ApiResponseUserAll> deleteAllUserPermanent(com.google.protobuf.Empty request) {
        return userCommandService.deleteAllTrashedUsers()
                .map(res -> ApiResponseUserAll.newBuilder()
                        .setStatus(res.status())
                        .setMessage(res.message())
                        .build());
    }

    @Override
    public Uni<VerifyPasswordResponse> verifyPassword(VerifyPasswordRequest request) {
        return userCommandService.verifyPassword(request.getEmail(), request.getPassword())
                .map(res -> VerifyPasswordResponse.newBuilder()
                        .setValid(true)
                        .setUser(mapToUserResponse(res.data()))
                        .build())
                .onFailure().recoverWithItem(err -> VerifyPasswordResponse.newBuilder()
                        .setValid(false)
                        .build());
    }

    private UserResponse mapToUserResponse(com.sanedge.user.domain.response.UserResponse u) {
        if (u == null) {
            return null;
        }
        return UserResponse.newBuilder()
                .setId(u.getId() != null ? u.getId().intValue() : 0)
                .setFirstname(u.getFirstname() != null ? u.getFirstname() : "")
                .setLastname(u.getLastname() != null ? u.getLastname() : "")
                .setEmail(u.getEmail() != null ? u.getEmail() : "")
                .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
                .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "")
                .build();
    }

    private UserResponseDeleteAt mapToUserResponseDeleteAt(com.sanedge.user.domain.response.UserResponseDeleteAt u) {
        UserResponseDeleteAt.Builder builder = UserResponseDeleteAt.newBuilder()
                .setId(u.getId() != null ? u.getId().intValue() : 0)
                .setFirstname(u.getFirstname() != null ? u.getFirstname() : "")
                .setLastname(u.getLastname() != null ? u.getLastname() : "")
                .setEmail(u.getEmail() != null ? u.getEmail() : "")
                .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
                .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "");
        if (u.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(u.getDeletedAt().toString()));
        }
        return builder.build();
    }

    private ApiResponseUser buildApiResponse(com.sanedge.user.domain.response.UserResponse response) {
        return ApiResponseUser.newBuilder()
                .setStatus("success")
                .setMessage("Success")
                .setData(mapToUserResponse(response))
                .build();
    }

    private ApiResponseUserDeleteAt buildApiResponseWithDeletedAt(
            com.sanedge.user.domain.response.UserResponseDeleteAt response) {
        return ApiResponseUserDeleteAt.newBuilder()
                .setStatus("success")
                .setMessage("Success")
                .setData(mapToUserResponseDeleteAt(response))
                .build();
    }
}
