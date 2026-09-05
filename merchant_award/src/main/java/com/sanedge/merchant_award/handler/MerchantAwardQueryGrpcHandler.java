package com.sanedge.merchant_award.handler;

import com.sanedge.merchant_award.domain.requests.FindAllMerchantRequest;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponseDeleteAt;
import com.sanedge.merchant_award.service.MerchantAwardQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward;
import pb.merchant_award.MerchantAwardCommon.ApiResponsePaginationMerchantAward;
import pb.merchant_award.MerchantAwardCommon.ApiResponsePaginationMerchantAwardDeleteAt;
import pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest;
import pb.merchant_award.MutinyMerchantAwardQueryServiceGrpc;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantAwardQueryGrpcHandler
        extends MutinyMerchantAwardQueryServiceGrpc.MerchantAwardQueryServiceImplBase {

    @Inject
    MerchantAwardQueryService merchantAwardQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationMerchantAward> findAll(pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantAwardQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantAward.Builder builder = ApiResponsePaginationMerchantAward.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantAwardResponse r : apiResp.data()) {
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
    public Uni<ApiResponseMerchantAward> findById(FindByIdMerchantAwardRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantAwardQueryService.findById((long) request.getId())
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
    @WithSession
    public Uni<ApiResponsePaginationMerchantAwardDeleteAt> findByActive(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantAwardQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantAwardDeleteAt.Builder builder = ApiResponsePaginationMerchantAwardDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantAwardResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationMerchantAwardDeleteAt> findByTrashed(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantAwardQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantAwardDeleteAt.Builder builder = ApiResponsePaginationMerchantAwardDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantAwardResponseDeleteAt r : apiResp.data()) {
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

    private pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse toProto(MerchantAwardResponse r) {
        if (r == null) {
            return pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse.getDefaultInstance();
        }
        pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse.Builder builder = pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse
                .newBuilder();
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

    private pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt toProto(
            MerchantAwardResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt.Builder builder = pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt
                .newBuilder();
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
