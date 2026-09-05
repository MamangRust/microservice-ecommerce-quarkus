package com.sanedge.merchant.handler;

import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.service.MerchantQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant.MerchantCommon.ApiResponseMerchant;
import pb.merchant.MerchantCommon.ApiResponsePaginationMerchant;
import pb.merchant.MerchantCommon.ApiResponsePaginationMerchantDeleteAt;
import pb.merchant.MerchantQuery.FindAllMerchantRequest;
import pb.merchant.MutinyMerchantQueryServiceGrpc;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantQueryGrpcHandler extends MutinyMerchantQueryServiceGrpc.MerchantQueryServiceImplBase {

    @Inject
    MerchantQueryService merchantQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationMerchant> findAll(FindAllMerchantRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchants domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchants();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchant.Builder builder = ApiResponsePaginationMerchant.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantResponse mr : apiResp.data()) {
                            builder.addData(toProto(mr));
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
    public Uni<ApiResponseMerchant> findById(pb.merchant.MerchantCommon.FindByIdMerchantRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantQueryService.findById((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchant.Builder builder = ApiResponseMerchant.newBuilder()
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
    @WithSession
    public Uni<ApiResponsePaginationMerchantDeleteAt> findByActive(FindAllMerchantRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchants domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchants();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDeleteAt.Builder builder = ApiResponsePaginationMerchantDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantResponseDeleteAt mrd : apiResp.data()) {
                            builder.addData(toProto(mrd));
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
    public Uni<ApiResponsePaginationMerchantDeleteAt> findByTrashed(FindAllMerchantRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchants domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchants();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDeleteAt.Builder builder = ApiResponsePaginationMerchantDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantResponseDeleteAt mrd : apiResp.data()) {
                            builder.addData(toProto(mrd));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.merchant.MerchantCommon.MerchantResponse toProto(MerchantResponse r) {
        if (r == null) {
            return pb.merchant.MerchantCommon.MerchantResponse.getDefaultInstance();
        }
        pb.merchant.MerchantCommon.MerchantResponse.Builder builder = pb.merchant.MerchantCommon.MerchantResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId().intValue());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant.MerchantCommon.MerchantResponseDeleteAt toProto(MerchantResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant.MerchantCommon.MerchantResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant.MerchantCommon.MerchantResponseDeleteAt.Builder builder = pb.merchant.MerchantCommon.MerchantResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId().intValue());
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
