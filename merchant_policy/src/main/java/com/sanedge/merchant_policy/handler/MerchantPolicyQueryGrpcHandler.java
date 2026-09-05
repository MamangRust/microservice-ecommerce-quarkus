package com.sanedge.merchant_policy.handler;

import com.sanedge.merchant_policy.domain.requests.FindAllMerchantRequest;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;
import com.sanedge.merchant_policy.service.MerchantPolicyQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPolicies;
import pb.merchant_policy.MerchantPolicyCommon.ApiResponsePaginationMerchantPolicies;
import pb.merchant_policy.MerchantPolicyCommon.ApiResponsePaginationMerchantPoliciesDeleteAt;
import pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest;
import pb.merchant_policy.MutinyMerchantPolicyQueryServiceGrpc;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantPolicyQueryGrpcHandler
        extends MutinyMerchantPolicyQueryServiceGrpc.MerchantPolicyQueryServiceImplBase {

    @Inject
    MerchantPolicyQueryService merchantPolicyQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationMerchantPolicies> findAll(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantPolicyQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantPolicies.Builder builder = ApiResponsePaginationMerchantPolicies
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantPoliciesResponse r : apiResp.data()) {
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
    public Uni<ApiResponseMerchantPolicies> findById(FindByIdMerchantPoliciesRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantPolicyQueryService.findById((long) request.getId())
                .map(apiResp -> {
                    ApiResponseMerchantPolicies.Builder builder = ApiResponseMerchantPolicies.newBuilder()
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
    public Uni<ApiResponsePaginationMerchantPoliciesDeleteAt> findByActive(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantPolicyQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantPoliciesDeleteAt.Builder builder = ApiResponsePaginationMerchantPoliciesDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantPoliciesResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationMerchantPoliciesDeleteAt> findByTrashed(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantPolicyQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantPoliciesDeleteAt.Builder builder = ApiResponsePaginationMerchantPoliciesDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantPoliciesResponseDeleteAt r : apiResp.data()) {
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

    private pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse toProto(MerchantPoliciesResponse r) {
        if (r == null) {
            return pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse.getDefaultInstance();
        }
        pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse.Builder builder = pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getPolicyType() != null) {
            builder.setPolicyType(r.getPolicyType());
        }
        if (r.getTitle() != null) {
            builder.setTitle(r.getTitle());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt toProto(
            MerchantPoliciesResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt.Builder builder = pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getPolicyType() != null) {
            builder.setPolicyType(r.getPolicyType());
        }
        if (r.getTitle() != null) {
            builder.setTitle(r.getTitle());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
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
