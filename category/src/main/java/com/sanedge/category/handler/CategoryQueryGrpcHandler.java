package com.sanedge.category.handler;

import com.sanedge.category.domain.requests.FindAllCategoryRequest;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.service.CategoryQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import pb.category.MutinyCategoryQueryServiceGrpc;
import pb.category.CategoryCommon.ApiResponseCategory;
import pb.category.CategoryCommon.ApiResponsePaginationCategory;
import pb.category.CategoryCommon.ApiResponsePaginationCategoryDeleteAt;
import pb.category.CategoryCommon.FindByIdCategoryRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class CategoryQueryGrpcHandler extends MutinyCategoryQueryServiceGrpc.CategoryQueryServiceImplBase {

    @Inject
    CategoryQueryService categoryQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationCategory> findAll(pb.category.CategoryQuery.FindAllCategoryRequest request) {
        FindAllCategoryRequest domainReq = new FindAllCategoryRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return categoryQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCategory.Builder builder = ApiResponsePaginationCategory.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (CategoryResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponseCategory> findById(FindByIdCategoryRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return categoryQueryService.findById((long) request.getId())
                .map(apiResp -> {
                    ApiResponseCategory.Builder builder = ApiResponseCategory.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationCategoryDeleteAt> findByActive(
            pb.category.CategoryQuery.FindAllCategoryRequest request) {
        FindAllCategoryRequest domainReq = new FindAllCategoryRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return categoryQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCategoryDeleteAt.Builder builder = ApiResponsePaginationCategoryDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (CategoryResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationCategoryDeleteAt> findByTrashed(
            pb.category.CategoryQuery.FindAllCategoryRequest request) {
        FindAllCategoryRequest domainReq = new FindAllCategoryRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return categoryQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCategoryDeleteAt.Builder builder = ApiResponsePaginationCategoryDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (CategoryResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.category.CategoryCommon.CategoryResponse toProto(CategoryResponse r) {
        if (r == null) {
            return pb.category.CategoryCommon.CategoryResponse.getDefaultInstance();
        }
        pb.category.CategoryCommon.CategoryResponse.Builder builder = pb.category.CategoryCommon.CategoryResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getSlugCategory() != null) {
            builder.setSlugCategory(r.getSlugCategory());
        }
        if (r.getImageCategory() != null) {
            builder.setImageCategory(r.getImageCategory());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        return builder.build();
    }

    private pb.category.CategoryCommon.CategoryResponseDeleteAt toProto(CategoryResponseDeleteAt r) {
        if (r == null) {
            return pb.category.CategoryCommon.CategoryResponseDeleteAt.getDefaultInstance();
        }
        pb.category.CategoryCommon.CategoryResponseDeleteAt.Builder builder = pb.category.CategoryCommon.CategoryResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getSlugCategory() != null) {
            builder.setSlugCategory(r.getSlugCategory());
        }
        if (r.getImageCategory() != null) {
            builder.setImageCategory(r.getImageCategory());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt().toString()));
        }
        return builder.build();
    }

    private pb.Api.PaginationMeta toProto(com.sanedge.common.domain.response.PaginationMeta m) {
        if (m == null) {
            return pb.Api.PaginationMeta.getDefaultInstance();
        }
        return pb.Api.PaginationMeta.newBuilder()
                .setCurrentPage(m.currentPage())
                .setPageSize(m.pageSize())
                .setTotalPages(m.totalPages())
                .setTotalRecords(m.totalRecords())
                .build();
    }
}
