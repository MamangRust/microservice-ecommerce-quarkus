package com.sanedge.merchant_detail.handler;

import java.util.stream.Collectors;

import com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponseDeleteAt;
import com.sanedge.merchant_detail.domain.response.MerchantSocialMediaLinkResponse;
import com.sanedge.merchant_detail.service.MerchantDetailQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail;
import pb.merchant_detail.MerchantDetailCommon.ApiResponsePaginationMerchantDetail;
import pb.merchant_detail.MerchantDetailCommon.ApiResponsePaginationMerchantDetailDeleteAt;
import pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest;
import pb.merchant_detail.MutinyMerchantDetailQueryServiceGrpc;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class MerchantDetailQueryGrpcHandler
        extends MutinyMerchantDetailQueryServiceGrpc.MerchantDetailQueryServiceImplBase {

    @Inject
    MerchantDetailQueryService merchantDetailQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationMerchantDetail> findAll(pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantDetailQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDetail.Builder builder = ApiResponsePaginationMerchantDetail
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDetailRelationResponse r : apiResp.data()) {
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
    public Uni<ApiResponseMerchantDetail> findById(FindByIdMerchantDetailRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return merchantDetailQueryService.findById((long) request.getId())
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
    @WithSession
    public Uni<ApiResponsePaginationMerchantDetailDeleteAt> findByActive(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantDetailQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDetailDeleteAt.Builder builder = ApiResponsePaginationMerchantDetailDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDetailRelationResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationMerchantDetailDeleteAt> findByTrashed(
            pb.merchant.MerchantQuery.FindAllMerchantRequest request) {
        FindAllMerchantRequest domainReq = new FindAllMerchantRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return merchantDetailQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDetailDeleteAt.Builder builder = ApiResponsePaginationMerchantDetailDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDetailRelationResponseDeleteAt r : apiResp.data()) {
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

    private pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponse toProto(MerchantDetailRelationResponse r) {
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
        if (r.getSocialMediaLinks() != null) {
            builder.addAllSocialMediaLinks(r.getSocialMediaLinks().stream()
                    .map(this::toProto)
                    .collect(Collectors.toList()));
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
            MerchantDetailRelationResponseDeleteAt r) {
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
        if (r.getSocialMediaLinks() != null) {
            builder.addAllSocialMediaLinks(r.getSocialMediaLinks().stream()
                    .map(this::toProto)
                    .collect(Collectors.toList()));
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

    private pb.merchant_detail.MerchantDetailCommon.MerchantSocialMediaLinkResponse toProto(
            MerchantSocialMediaLinkResponse r) {
        if (r == null) {
            return pb.merchant_detail.MerchantDetailCommon.MerchantSocialMediaLinkResponse.getDefaultInstance();
        }
        pb.merchant_detail.MerchantDetailCommon.MerchantSocialMediaLinkResponse.Builder builder = pb.merchant_detail.MerchantDetailCommon.MerchantSocialMediaLinkResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getPlatform() != null) {
            builder.setPlatform(r.getPlatform());
        }
        if (r.getUrl() != null) {
            builder.setUrl(r.getUrl());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
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
