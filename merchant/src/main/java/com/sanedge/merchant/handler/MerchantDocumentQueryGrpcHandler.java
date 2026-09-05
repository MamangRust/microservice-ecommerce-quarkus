package com.sanedge.merchant.handler;

import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import com.sanedge.merchant.service.MerchantDocumentQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentCommon.ApiResponsePaginationMerchantDocument;
import pb.merchant_document.MerchantDocumentCommon.ApiResponsePaginationMerchantDocumentAt;
import pb.merchant_document.MerchantDocumentQuery.FindAllMerchantDocumentsRequest;
import pb.merchant_document.MerchantDocumentQuery.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MutinyMerchantDocumentQueryServiceGrpc;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantDocumentQueryGrpcHandler
        extends MutinyMerchantDocumentQueryServiceGrpc.MerchantDocumentQueryServiceImplBase {

    @Inject
    MerchantDocumentQueryService merchantDocumentQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationMerchantDocument> findAll(FindAllMerchantDocumentsRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchantDocuments domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchantDocuments();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantDocumentQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDocument.Builder builder = ApiResponsePaginationMerchantDocument
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDocumentResponse r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationMerchantDocument> findAllActive(FindAllMerchantDocumentsRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchantDocuments domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchantDocuments();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantDocumentQueryService.findAllActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDocument.Builder builder = ApiResponsePaginationMerchantDocument
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDocumentResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProtoMerchantDocument(r));
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
    public Uni<ApiResponsePaginationMerchantDocumentAt> findAllTrashed(FindAllMerchantDocumentsRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchantDocuments domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchantDocuments();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantDocumentQueryService.findAllTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDocumentAt.Builder builder = ApiResponsePaginationMerchantDocumentAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDocumentResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponseMerchantDocument> findById(FindMerchantDocumentByIdRequest request) {
        if (request.getDocumentId() <= 0) {
            return IdValidator.invalid("Document id");
        }
        return merchantDocumentQueryService.findById((long) request.getDocumentId())
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof jakarta.ws.rs.NotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
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

    private pb.merchant_document.MerchantDocumentCommon.MerchantDocument toProtoMerchantDocument(
            MerchantDocumentResponseDeleteAt r) {
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

    private pb.merchant_document.MerchantDocumentCommon.MerchantDocumentDeleteAt toProto(
            MerchantDocumentResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_document.MerchantDocumentCommon.MerchantDocumentDeleteAt.getDefaultInstance();
        }
        pb.merchant_document.MerchantDocumentCommon.MerchantDocumentDeleteAt.Builder builder = pb.merchant_document.MerchantDocumentCommon.MerchantDocumentDeleteAt
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
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
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
