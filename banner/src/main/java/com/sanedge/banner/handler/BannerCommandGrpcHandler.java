package com.sanedge.banner.handler;

import com.sanedge.banner.domain.requests.CreateBannerRequest;
import com.sanedge.banner.domain.requests.UpdateBannerRequest;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;
import com.sanedge.banner.service.BannerCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.banner.MutinyBannerCommandServiceGrpc;
import pb.banner.BannerCommon.ApiResponseBanner;
import pb.banner.BannerCommon.ApiResponseBannerAll;
import pb.banner.BannerCommon.ApiResponseBannerDelete;
import pb.banner.BannerCommon.ApiResponseBannerDeleteAt;
import pb.banner.BannerCommon.FindByIdBannerRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class BannerCommandGrpcHandler extends MutinyBannerCommandServiceGrpc.BannerCommandServiceImplBase {

    @Inject
    BannerCommandService bannerCommandService;

    @Override
    public Uni<ApiResponseBanner> create(pb.banner.BannerCommand.CreateBannerRequest request) {
        CreateBannerRequest domainReq = new CreateBannerRequest();
        domainReq.setName(request.getName());
        domainReq.setStartDate(request.getStartDate());
        domainReq.setEndDate(request.getEndDate());
        domainReq.setStartTime(request.getStartTime());
        domainReq.setEndTime(request.getEndTime());
        domainReq.setIsActive(request.getIsActive());

        return bannerCommandService.createBanner(domainReq)
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
                    if (e instanceof com.sanedge.common.exception.ResourceAlreadyExistsException) {
                        return Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseBanner> update(pb.banner.BannerCommand.UpdateBannerRequest request) {
        if (request.getBannerId() <= 0) {
            return IdValidator.invalid("Banner id");
        }
        UpdateBannerRequest domainReq = new UpdateBannerRequest();
        domainReq.setId((long) request.getBannerId());
        domainReq.setName(request.getName());
        domainReq.setStartDate(request.getStartDate());
        domainReq.setEndDate(request.getEndDate());
        domainReq.setStartTime(request.getStartTime());
        domainReq.setEndTime(request.getEndTime());
        domainReq.setIsActive(request.getIsActive());

        return bannerCommandService.updateBanner(domainReq)
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
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseBannerDeleteAt> trash(FindByIdBannerRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return bannerCommandService.trashedBanner((long) request.getId())
                .map(apiResp -> {
                    ApiResponseBannerDeleteAt.Builder builder = ApiResponseBannerDeleteAt.newBuilder()
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
    public Uni<ApiResponseBannerDeleteAt> restore(FindByIdBannerRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return bannerCommandService.restoreBanner((long) request.getId())
                .map(apiResp -> {
                    ApiResponseBannerDeleteAt.Builder builder = ApiResponseBannerDeleteAt.newBuilder()
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
    public Uni<ApiResponseBannerDelete> deletePermanent(FindByIdBannerRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return bannerCommandService.deleteBannerPermanent((long) request.getId())
                .map(apiResp -> ApiResponseBannerDelete.newBuilder()
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
    public Uni<ApiResponseBannerAll> restoreAll(com.google.protobuf.Empty request) {
        return bannerCommandService.restoreAllBanner()
                .map(apiResp -> ApiResponseBannerAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseBannerAll> deleteAll(com.google.protobuf.Empty request) {
        return bannerCommandService.deleteAllBannerPermanent()
                .map(apiResp -> ApiResponseBannerAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
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
        pb.banner.BannerCommon.BannerResponseDeleteAt.Builder builder = pb.banner.BannerCommon.BannerResponseDeleteAt.newBuilder();
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
}
