package com.sanedge.category.handler;

import com.sanedge.category.domain.requests.CreateCategoryRequest;
import com.sanedge.category.domain.requests.UpdateCategoryRequest;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.service.CategoryCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.category.MutinyCategoryCommandServiceGrpc;
import pb.category.CategoryCommon.ApiResponseCategory;
import pb.category.CategoryCommon.ApiResponseCategoryAll;
import pb.category.CategoryCommon.ApiResponseCategoryDelete;
import pb.category.CategoryCommon.ApiResponseCategoryDeleteAt;
import pb.category.CategoryCommon.FindByIdCategoryRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class CategoryCommandGrpcHandler extends MutinyCategoryCommandServiceGrpc.CategoryCommandServiceImplBase {

    @Inject
    CategoryCommandService categoryCommandService;

    @Override
    public Uni<ApiResponseCategory> create(pb.category.CategoryCommand.CreateCategoryRequest request) {
        CreateCategoryRequest domainReq = new CreateCategoryRequest();
        domainReq.setName(request.getName());
        domainReq.setDescription(request.getDescription());
        domainReq.setSlugCategory(request.getSlugCategory());
        domainReq.setImageCategory(request.getImageCategory());

        return categoryCommandService.createCategory(domainReq)
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
                    if (e instanceof com.sanedge.common.exception.ResourceAlreadyExistsException) {
                        return Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseCategory> update(pb.category.CategoryCommand.UpdateCategoryRequest request) {
        if (request.getCategoryId() <= 0) {
            return IdValidator.invalid("Category id");
        }
        UpdateCategoryRequest domainReq = new UpdateCategoryRequest();
        domainReq.setCategoryId(request.getCategoryId());
        domainReq.setName(request.getName());
        domainReq.setDescription(request.getDescription());
        domainReq.setSlugCategory(request.getSlugCategory());
        domainReq.setImageCategory(request.getImageCategory());

        return categoryCommandService.updateCategory(domainReq)
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
    public Uni<ApiResponseCategoryDeleteAt> trashedCategory(FindByIdCategoryRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return categoryCommandService.trashedCategory((long) request.getId())
                .map(apiResp -> {
                    ApiResponseCategoryDeleteAt.Builder builder = ApiResponseCategoryDeleteAt.newBuilder()
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
    public Uni<ApiResponseCategoryDeleteAt> restoreCategory(FindByIdCategoryRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return categoryCommandService.restoreCategory((long) request.getId())
                .map(apiResp -> {
                    ApiResponseCategoryDeleteAt.Builder builder = ApiResponseCategoryDeleteAt.newBuilder()
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
    public Uni<ApiResponseCategoryDelete> deleteCategoryPermanent(FindByIdCategoryRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return categoryCommandService.deleteCategoryPermanent((long) request.getId())
                .map(apiResp -> ApiResponseCategoryDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseCategoryAll> restoreAllCategory(com.google.protobuf.Empty request) {
        return categoryCommandService.restoreAllCategories()
                .map(apiResp -> ApiResponseCategoryAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseCategoryAll> deleteAllCategoryPermanent(com.google.protobuf.Empty request) {
        return categoryCommandService.deleteAllCategoriesPermanent()
                .map(apiResp -> ApiResponseCategoryAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.category.CategoryCommon.CategoryResponse toProto(CategoryResponse r) {
        if (r == null) {
            return pb.category.CategoryCommon.CategoryResponse.getDefaultInstance();
        }
        pb.category.CategoryCommon.CategoryResponse.Builder builder = pb.category.CategoryCommon.CategoryResponse.newBuilder();
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
        pb.category.CategoryCommon.CategoryResponseDeleteAt.Builder builder = pb.category.CategoryCommon.CategoryResponseDeleteAt.newBuilder();
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
}
