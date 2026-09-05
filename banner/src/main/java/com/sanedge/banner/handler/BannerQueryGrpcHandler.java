package com.sanedge.banner.handler;

import com.sanedge.banner.domain.requests.FindAllBannerRequest;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;
import com.sanedge.banner.service.BannerQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.banner.BannerCommon.ApiResponseBanner;
import pb.banner.BannerCommon.ApiResponsePaginationBanner;
import pb.banner.BannerCommon.ApiResponsePaginationBannerDeleteAt;
import pb.banner.BannerCommon.FindByIdBannerRequest;
import pb.banner.MutinyBannerQueryServiceGrpc;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class BannerQueryGrpcHandler extends MutinyBannerQueryServiceGrpc.BannerQueryServiceImplBase {

    @Inject
    BannerQueryService bannerQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationBanner> findAll(pb.banner.BannerQuery.FindAllBannerRequest request) {
        FindAllBannerRequest domainReq = new FindAllBannerRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return bannerQueryService.findAllPaginated(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationBanner.Builder builder = ApiResponsePaginationBanner.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (BannerResponse r : apiResp.data()) {
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
    public Uni<ApiResponseBanner> findById(FindByIdBannerRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return bannerQueryService.findById((long) request.getId())
                .map(apiResp -> {
                    ApiResponseBanner.Builder builder = ApiResponseBanner.newBuilder()
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

    @Override
    @WithSession
    public Uni<ApiResponsePaginationBannerDeleteAt> findByActive(pb.banner.BannerQuery.FindAllBannerRequest request) {
        FindAllBannerRequest domainReq = new FindAllBannerRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return bannerQueryService.findActivePaginated(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationBannerDeleteAt.Builder builder = ApiResponsePaginationBannerDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (BannerResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationBannerDeleteAt> findByTrashed(pb.banner.BannerQuery.FindAllBannerRequest request) {
        FindAllBannerRequest domainReq = new FindAllBannerRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return bannerQueryService.findTrashedPaginated(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationBannerDeleteAt.Builder builder = ApiResponsePaginationBannerDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (BannerResponseDeleteAt r : apiResp.data()) {
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

    private pb.banner.BannerCommon.BannerResponse toProto(BannerResponse r) {
        if (r == null) {
            return pb.banner.BannerCommon.BannerResponse.getDefaultInstance();
        }
        pb.banner.BannerCommon.BannerResponse.Builder builder = pb.banner.BannerCommon.BannerResponse.newBuilder();
        if (r.getId() != null) {
            builder.setBannerId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getStartDate() != null) {
            builder.setStartDate(r.getStartDate());
        }
        if (r.getEndDate() != null) {
            builder.setEndDate(r.getEndDate());
        }
        if (r.getStartTime() != null) {
            builder.setStartTime(r.getStartTime());
        }
        if (r.getEndTime() != null) {
            builder.setEndTime(r.getEndTime());
        }
        if (r.getIsActive() != null) {
            builder.setIsActive(r.getIsActive());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.banner.BannerCommon.BannerResponseDeleteAt toProto(BannerResponseDeleteAt r) {
        if (r == null) {
            return pb.banner.BannerCommon.BannerResponseDeleteAt.getDefaultInstance();
        }
        pb.banner.BannerCommon.BannerResponseDeleteAt.Builder builder = pb.banner.BannerCommon.BannerResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setBannerId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getStartDate() != null) {
            builder.setStartDate(r.getStartDate());
        }
        if (r.getEndDate() != null) {
            builder.setEndDate(r.getEndDate());
        }
        if (r.getStartTime() != null) {
            builder.setStartTime(r.getStartTime());
        }
        if (r.getEndTime() != null) {
            builder.setEndTime(r.getEndTime());
        }
        if (r.getIsActive() != null) {
            builder.setIsActive(r.getIsActive());
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
