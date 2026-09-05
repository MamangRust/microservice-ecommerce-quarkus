package com.sanedge.merchant_business.handler;

import com.sanedge.merchant_business.domain.requests.FindAllMerchantRequest;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;
import com.sanedge.merchant_business.service.MerchantBusinessQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness;
import pb.merchant_business.MerchantBusinessCommon.ApiResponsePaginationMerchantBusiness;
import pb.merchant_business.MerchantBusinessCommon.ApiResponsePaginationMerchantBusinessDeleteAt;
import pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest;
import pb.merchant_business.MutinyMerchantBusinessQueryServiceGrpc;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantBusinessQueryGrpcHandler
        extends MutinyMerchantBusinessQueryServiceGrpc.MerchantBusinessQueryServiceImplBase {

    @Inject
    MerchantBusinessQueryService merchantBusinessQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationMerchantBusiness> findAll(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantBusinessQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantBusiness.Builder builder = ApiResponsePaginationMerchantBusiness
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantBusinessResponse r : apiResp.data()) {
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
    public Uni<ApiResponseMerchantBusiness> findById(FindByIdMerchantBusinessRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantBusinessQueryService.findById((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantBusiness.Builder builder = ApiResponseMerchantBusiness.newBuilder()
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
    public Uni<ApiResponsePaginationMerchantBusinessDeleteAt> findByActive(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantBusinessQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantBusinessDeleteAt.Builder builder = ApiResponsePaginationMerchantBusinessDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantBusinessResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationMerchantBusinessDeleteAt> findByTrashed(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantBusinessQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantBusinessDeleteAt.Builder builder = ApiResponsePaginationMerchantBusinessDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantBusinessResponseDeleteAt r : apiResp.data()) {
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

    private pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse toProto(MerchantBusinessResponse r) {
        if (r == null) {
            return pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse.getDefaultInstance();
        }
        pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse.Builder builder = pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getBusinessType() != null) {
            builder.setBusinessType(r.getBusinessType());
        }
        if (r.getTaxId() != null) {
            builder.setTaxId(r.getTaxId());
        }
        if (r.getEstablishedYear() != null) {
            builder.setEstablishedYear(r.getEstablishedYear());
        }
        if (r.getNumberOfEmployees() != null) {
            builder.setNumberOfEmployees(r.getNumberOfEmployees());
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

    private pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt toProto(
            MerchantBusinessResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt.Builder builder = pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getBusinessType() != null) {
            builder.setBusinessType(r.getBusinessType());
        }
        if (r.getTaxId() != null) {
            builder.setTaxId(r.getTaxId());
        }
        if (r.getEstablishedYear() != null) {
            builder.setEstablishedYear(r.getEstablishedYear());
        }
        if (r.getNumberOfEmployees() != null) {
            builder.setNumberOfEmployees(r.getNumberOfEmployees());
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
