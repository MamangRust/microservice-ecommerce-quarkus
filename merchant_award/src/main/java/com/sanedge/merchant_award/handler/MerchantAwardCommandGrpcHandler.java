package com.sanedge.merchant_award.handler;

import com.sanedge.merchant_award.domain.requests.CreateMerchantAwardRequest;
import com.sanedge.merchant_award.domain.requests.UpdateMerchantAwardRequest;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponseDeleteAt;
import com.sanedge.merchant_award.service.MerchantAwardCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_award.MutinyMerchantAwardCommandServiceGrpc;
import pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward;
import pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt;
import pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantAwardCommandGrpcHandler extends MutinyMerchantAwardCommandServiceGrpc.MerchantAwardCommandServiceImplBase {

    @Inject
    MerchantAwardCommandService merchantAwardCommandService;

    @Override
    public Uni<ApiResponseMerchantAward> create(pb.merchant_award.MerchantAwardCommand.CreateMerchantAwardRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        CreateMerchantAwardRequest domainReq = new CreateMerchantAwardRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setTitle(request.getTitle());
        domainReq.setDescription(request.getDescription());
        domainReq.setIssuedBy(request.getIssuedBy());
        domainReq.setIssueDate(request.getIssueDate());
        domainReq.setExpiryDate(request.getExpiryDate());
        domainReq.setCertificateUrl(request.getCertificateUrl());

        return merchantAwardCommandService.createMerchantAward(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantAward.Builder builder = ApiResponseMerchantAward.newBuilder()
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
    public Uni<ApiResponseMerchantAward> update(pb.merchant_award.MerchantAwardCommand.UpdateMerchantAwardRequest request) {
        if (request.getMerchantCertificationId() <= 0) {
            return IdValidator.invalid("MerchantCertification id");
        }
        UpdateMerchantAwardRequest domainReq = new UpdateMerchantAwardRequest();
        domainReq.setMerchantCertificationId(request.getMerchantCertificationId());
        domainReq.setTitle(request.getTitle());
        domainReq.setDescription(request.getDescription());
        domainReq.setIssuedBy(request.getIssuedBy());
        domainReq.setIssueDate(request.getIssueDate());
        domainReq.setExpiryDate(request.getExpiryDate());
        domainReq.setCertificateUrl(request.getCertificateUrl());

        return merchantAwardCommandService.updateMerchantAward(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantAward.Builder builder = ApiResponseMerchantAward.newBuilder()
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
    public Uni<ApiResponseMerchantAwardDeleteAt> trashedMerchantAward(FindByIdMerchantAwardRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantAwardCommandService.trashedMerchantAward((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantAwardDeleteAt.Builder builder = ApiResponseMerchantAwardDeleteAt.newBuilder()
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
    public Uni<ApiResponseMerchantAwardDeleteAt> restoreMerchantAward(FindByIdMerchantAwardRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantAwardCommandService.restoreMerchantAward((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantAwardDeleteAt.Builder builder = ApiResponseMerchantAwardDeleteAt.newBuilder()
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
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantDelete> deleteMerchantAwardPermanent(FindByIdMerchantAwardRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantAwardCommandService.deleteMerchantAwardPermanent((long) request.getId())
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
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantAll> restoreAllMerchantAward(com.google.protobuf.Empty request) {
        return merchantAwardCommandService.restoreAllMerchantAward()
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<pb.merchant.MerchantCommon.ApiResponseMerchantAll> deleteAllMerchantAwardPermanent(com.google.protobuf.Empty request) {
        return merchantAwardCommandService.deleteAllMerchantAwardPermanent()
                .map(apiResp -> pb.merchant.MerchantCommon.ApiResponseMerchantAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse toProto(MerchantAwardResponse r) {
        if (r == null) {
            return pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse.getDefaultInstance();
        }
        pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse.Builder builder = pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getTitle() != null) {
            builder.setTitle(r.getTitle());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getIssuedBy() != null) {
            builder.setIssuedBy(r.getIssuedBy());
        }
        if (r.getIssueDate() != null) {
            builder.setIssueDate(r.getIssueDate());
        }
        if (r.getExpiryDate() != null) {
            builder.setExpiryDate(r.getExpiryDate());
        }
        if (r.getCertificateUrl() != null) {
            builder.setCertificateUrl(r.getCertificateUrl());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt toProto(MerchantAwardResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt.Builder builder = pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getTitle() != null) {
            builder.setTitle(r.getTitle());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getIssuedBy() != null) {
            builder.setIssuedBy(r.getIssuedBy());
        }
        if (r.getIssueDate() != null) {
            builder.setIssueDate(r.getIssueDate());
        }
        if (r.getExpiryDate() != null) {
            builder.setExpiryDate(r.getExpiryDate());
        }
        if (r.getCertificateUrl() != null) {
            builder.setCertificateUrl(r.getCertificateUrl());
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
