package com.sanedge.user.handler;

import java.util.stream.Collectors;

import com.sanedge.user.domain.requests.FindAllUsers;
import com.sanedge.user.service.UserQueryService;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import pb.user.MutinyUserQueryServiceGrpc;
import pb.user.UserCommon.ApiResponsePaginationUser;
import pb.user.UserCommon.ApiResponsePaginationUserDeleteAt;
import pb.user.UserCommon.ApiResponseUser;
import pb.user.UserCommon.FindByIdUserRequest;
import pb.user.UserCommon.UserResponse;
import pb.user.UserCommon.UserResponseDeleteAt;
import pb.user.UserQuery.FindAllUserRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
public class UserQueryGrpcHandler extends MutinyUserQueryServiceGrpc.UserQueryServiceImplBase {

    @Inject
    UserQueryService userQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationUser> findAll(FindAllUserRequest request) {
        FindAllUsers req = new FindAllUsers();
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());
        req.setSearch(request.getSearch());

        return userQueryService.findAllPaginated(req)
                .map(res -> {
                    ApiResponsePaginationUser.Builder builder = ApiResponsePaginationUser.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());

                    if (res.data() != null) {
                        builder.addAllData(res.data().stream()
                                .map(this::mapToUserResponse)
                                .collect(Collectors.toList()));
                    }

                    if (res.pagination() != null) {
                        builder.setPagination(toProto(res.pagination()));
                    }
                    return builder.build();
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponseUser> findById(FindByIdUserRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return userQueryService.findById((long) request.getId())
                .map(res -> {
                    ApiResponseUser.Builder builder = ApiResponseUser.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.setData(mapToUserResponse(res.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationUserDeleteAt> findByActive(FindAllUserRequest request) {
        FindAllUsers req = new FindAllUsers();
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());
        req.setSearch(request.getSearch());

        return userQueryService.findActivePaginated(req)
                .map(res -> {
                    ApiResponsePaginationUserDeleteAt.Builder builder = ApiResponsePaginationUserDeleteAt.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.addAllData(res.data().stream()
                                .map(this::mapToUserResponseDeleteAt)
                                .collect(Collectors.toList()));
                    }
                    if (res.pagination() != null) {
                        builder.setPagination(toProto(res.pagination()));
                    }
                    return builder.build();
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationUserDeleteAt> findByTrashed(FindAllUserRequest request) {
        FindAllUsers req = new FindAllUsers();
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());
        req.setSearch(request.getSearch());

        return userQueryService.findTrashedPaginated(req)
                .map(res -> {
                    ApiResponsePaginationUserDeleteAt.Builder builder = ApiResponsePaginationUserDeleteAt.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.addAllData(res.data().stream()
                                .map(this::mapToUserResponseDeleteAt)
                                .collect(Collectors.toList()));
                    }
                    if (res.pagination() != null) {
                        builder.setPagination(toProto(res.pagination()));
                    }
                    return builder.build();
                });
    }

    private pb.Api.PaginationMeta toProto(com.sanedge.common.domain.response.PaginationMeta pagination) {
        return pb.Api.PaginationMeta.newBuilder()
                .setCurrentPage(pagination.currentPage())
                .setPageSize(pagination.pageSize())
                .setTotalPages(pagination.totalPages())
                .setTotalRecords(pagination.totalRecords())
                .build();
    }

    private UserResponse mapToUserResponse(com.sanedge.user.domain.response.UserResponse u) {
        return UserResponse.newBuilder()
                .setId(u.getId().intValue())
                .setFirstname(u.getFirstname())
                .setLastname(u.getLastname())
                .setEmail(u.getEmail())
                .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
                .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "")
                .build();
    }

    private UserResponseDeleteAt mapToUserResponseDeleteAt(com.sanedge.user.domain.response.UserResponseDeleteAt u) {
        UserResponseDeleteAt.Builder builder = UserResponseDeleteAt.newBuilder()
                .setId(u.getId().intValue())
                .setFirstname(u.getFirstname())
                .setLastname(u.getLastname())
                .setEmail(u.getEmail())
                .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
                .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "");
        if (u.getDeletedAt() != null) {
            builder.setDeletedAt(
                    com.google.protobuf.StringValue.newBuilder().setValue(u.getDeletedAt().toString()).build());
        }
        return builder.build();
    }
}
