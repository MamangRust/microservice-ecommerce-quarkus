package com.sanedge.merchant_detail.handler;

import com.sanedge.merchant_detail.domain.response.MerchantDetailResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailResponseDeleteAt;
import com.sanedge.merchant_detail.service.MerchantDetailCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_detail.MutinyMerchantDetailCommandServiceGrpc;
import pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail;
import pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt;
import pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantDetailCommandGrpcHandler
        extends MutinyMerchantDetailCommandServiceGrpc.MerchantDetailCommandServiceImplBase {

    @Inject
    MerchantDetailCommandService merchantDetailCommandService;

    @Override
    public Uni<ApiResponseMerchantDetail> create(
            pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest request) {
        return merchantDetailCommandService.createMerchant(request)
                .map(apiResp -> {
                    ApiResponseMerchantDetail.Builder builder = ApiResponseMerchantDetail.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseMerchantDetail> update(
            pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest request) {
        if (request.getMerchantDetailId() <= 0) {
            return IdValidator.invalid("MerchantDetail id");
        }
        return merchantDetailCommandService.updateMerchant(request)
                .map(apiResp -> {
                    ApiResponseMerchantDetail.Builder builder = ApiResponseMerchantDetail.newBuilder()
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
    public Uni<ApiResponseMerchantDetailDeleteAt> trashedMerchantDetail(FindByIdMerchantDetailRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantDetailCommandService.trashedMerchant((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantDetailDeleteAt.Builder builder = ApiResponseMerchantDetailDeleteAt.newBuilder()
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
    public Uni<ApiResponseMerchantDetailDeleteAt> restoreMerchantDetail(FindByIdMerchantDetailRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantDetailCommandService.restoreMerchant((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantDetailDeleteAt.Builder builder = ApiResponseMerchantDetailDeleteAt.newBuilder()
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
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantDelete> deleteMerchantDetailPermanent(
            FindByIdMerchantDetailRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantDetailCommandService.deleteMerchantPermanent((long) request.getId())
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantDelete.newBuilder()
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
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantAll> restoreAllMerchantDetail(
            com.google.protobuf.Empty request) {
        return merchantDetailCommandService.restoreAllMerchant()
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantAll> deleteAllMerchantDetailPermanent(
            com.google.protobuf.Empty request) {
        return merchantDetailCommandService.deleteAllMerchantPermanent()
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponse toProto(MerchantDetailResponse r) {
        if (r == null) {
            return pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponse.getDefaultInstance();
        }
        pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponse.Builder builder = pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getDisplayName() != null) {
            builder.setDisplayName(r.getDisplayName());
        }
        if (r.getCoverImageUrl() != null) {
            builder.setCoverImageUrl(r.getCoverImageUrl());
        }
        if (r.getLogoUrl() != null) {
            builder.setLogoUrl(r.getLogoUrl());
        }
        if (r.getShortDescription() != null) {
            builder.setShortDescription(r.getShortDescription());
        }
        if (r.getWebsiteUrl() != null) {
            builder.setWebsiteUrl(r.getWebsiteUrl());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponseDeleteAt toProto(
            MerchantDetailResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponseDeleteAt.Builder builder = pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getDisplayName() != null) {
            builder.setDisplayName(r.getDisplayName());
        }
        if (r.getCoverImageUrl() != null) {
            builder.setCoverImageUrl(r.getCoverImageUrl());
        }
        if (r.getLogoUrl() != null) {
            builder.setLogoUrl(r.getLogoUrl());
        }
        if (r.getShortDescription() != null) {
            builder.setShortDescription(r.getShortDescription());
        }
        if (r.getWebsiteUrl() != null) {
            builder.setWebsiteUrl(r.getWebsiteUrl());
        }
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
}
