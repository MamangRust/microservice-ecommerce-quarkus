package com.sanedge.merchant.handler;

import com.google.protobuf.Empty;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import com.sanedge.merchant.service.MerchantDocumentCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.DeleteMerchantDocumentPermanentRequest;
import pb.merchant_document.MerchantDocumentCommand.RestoreMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.TrashedMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;
import pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocumentAll;
import pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocumentDelete;
import pb.merchant_document.MutinyMerchantDocumentCommandServiceGrpc;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantDocumentCommandGrpcHandler
        extends MutinyMerchantDocumentCommandServiceGrpc.MerchantDocumentCommandServiceImplBase {

    @Inject
    MerchantDocumentCommandService merchantDocumentCommandService;

    @Override
    public Uni<ApiResponseMerchantDocument> create(CreateMerchantDocumentRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        com.sanedge.merchant.domain.requests.CreateMerchantDocumentRequest domainReq = new com.sanedge.merchant.domain.requests.CreateMerchantDocumentRequest();
        domainReq.setMerchantId((long) request.getMerchantId());
        domainReq.setDocumentType(request.getDocumentType());
        domainReq.setDocumentUrl(request.getDocumentUrl());

        return merchantDocumentCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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
    public Uni<ApiResponseMerchantDocument> update(UpdateMerchantDocumentRequest request) {
        if (request.getDocumentId() <= 0) {
            return IdValidator.invalid("Document id");
        }
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        com.sanedge.merchant.domain.requests.UpdateMerchantDocumentRequest domainReq = new com.sanedge.merchant.domain.requests.UpdateMerchantDocumentRequest();
        domainReq.setDocumentId((long) request.getDocumentId());
        domainReq.setMerchantId((long) request.getMerchantId());
        domainReq.setDocumentType(request.getDocumentType());
        domainReq.setDocumentUrl(request.getDocumentUrl());
        domainReq.setNote(request.getNote());
        domainReq.setStatus(request.getStatus());

        return merchantDocumentCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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
    public Uni<ApiResponseMerchantDocument> updateStatus(UpdateMerchantDocumentStatusRequest request) {
        if (request.getDocumentId() <= 0) {
            return IdValidator.invalid("Document id");
        }
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        com.sanedge.merchant.domain.requests.UpdateMerchantDocumentStatus domainReq = new com.sanedge.merchant.domain.requests.UpdateMerchantDocumentStatus();
        domainReq.setDocumentId((long) request.getDocumentId());
        domainReq.setMerchantId((long) request.getMerchantId());
        domainReq.setNote(request.getNote());
        domainReq.setStatus(request.getStatus());

        return merchantDocumentCommandService.updateStatus(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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
    public Uni<ApiResponseMerchantDocument> trashed(TrashedMerchantDocumentRequest request) {
        if (request.getDocumentId() <= 0) {
            return IdValidator.invalid("Document id");
        }
        return merchantDocumentCommandService.trash((long) request.getDocumentId())
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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
    public Uni<ApiResponseMerchantDocument> restore(RestoreMerchantDocumentRequest request) {
        if (request.getDocumentId() <= 0) {
            return IdValidator.invalid("Document id");
        }
        return merchantDocumentCommandService.restore((long) request.getDocumentId())
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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
    public Uni<ApiResponseMerchantDocumentDelete> deletePermanent(DeleteMerchantDocumentPermanentRequest request) {
        if (request.getDocumentId() <= 0) {
            return IdValidator.invalid("Document id");
        }
        return merchantDocumentCommandService.deletePermanent((long) request.getDocumentId())
                .map(apiResp -> ApiResponseMerchantDocumentDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseMerchantDocumentAll> restoreAll(Empty request) {
        return merchantDocumentCommandService.restoreAll()
                .map(apiResp -> ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseMerchantDocumentAll> deleteAllPermanent(Empty request) {
        return merchantDocumentCommandService.deleteAllPermanent()
                .map(apiResp -> ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.merchant_document.MerchantDocumentCommon.MerchantDocument toProto(MerchantDocumentResponse r) {
        if (r == null) {
            return pb.merchant_document.MerchantDocumentCommon.MerchantDocument.getDefaultInstance();
        }
        pb.merchant_document.MerchantDocumentCommon.MerchantDocument.Builder builder = pb.merchant_document.MerchantDocumentCommon.MerchantDocument
                .newBuilder();
        if (r.getDocumentId() != null) {
            builder.setDocumentId(r.getDocumentId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId().intValue());
        }
        if (r.getDocumentType() != null) {
            builder.setDocumentType(r.getDocumentType());
        }
        if (r.getDocumentUrl() != null) {
            builder.setDocumentUrl(r.getDocumentUrl());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getNote() != null) {
            builder.setNote(r.getNote());
        }
        if (r.getCreatedAt() != null) {
            builder.setUploadedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant_document.MerchantDocumentCommon.MerchantDocument toProto(MerchantDocumentResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_document.MerchantDocumentCommon.MerchantDocument.getDefaultInstance();
        }
        pb.merchant_document.MerchantDocumentCommon.MerchantDocument.Builder builder = pb.merchant_document.MerchantDocumentCommon.MerchantDocument
                .newBuilder();
        if (r.getDocumentId() != null) {
            builder.setDocumentId(r.getDocumentId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId().intValue());
        }
        if (r.getDocumentType() != null) {
            builder.setDocumentType(r.getDocumentType());
        }
        if (r.getDocumentUrl() != null) {
            builder.setDocumentUrl(r.getDocumentUrl());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getNote() != null) {
            builder.setNote(r.getNote());
        }
        if (r.getCreatedAt() != null) {
            builder.setUploadedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }
}
