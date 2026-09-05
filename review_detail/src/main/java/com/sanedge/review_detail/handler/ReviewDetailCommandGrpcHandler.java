package com.sanedge.review_detail.handler;

import java.util.List;

import com.sanedge.review_detail.domain.requests.CreateReviewDetailRequest;
import com.sanedge.review_detail.domain.requests.UpdateReviewDetailRequest;
import com.sanedge.review_detail.domain.response.ReviewDetailResponse;
import com.sanedge.review_detail.domain.response.ReviewDetailResponseDeleteAt;
import com.sanedge.review_detail.service.ReviewDetailService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.review_detail.MutinyReviewDetailCommandServiceGrpc;
import pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail;
import pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt;
import pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class ReviewDetailCommandGrpcHandler
        extends MutinyReviewDetailCommandServiceGrpc.ReviewDetailCommandServiceImplBase {

    @Inject
    ReviewDetailService reviewDetailService;

    @Override
    public Uni<ApiResponseReviewDetail> create(pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest request) {
        if (request.getReviewId() <= 0) {
            return IdValidator.invalid("Review id");
        }
        CreateReviewDetailRequest domainReq = new CreateReviewDetailRequest();
        domainReq.setReviewId(request.getReviewId());
        domainReq.setType(request.getType());
        domainReq.setFile(request.getUrl());
        domainReq.setCaption(request.getCaption());

        return reviewDetailService.create(List.of(domainReq))
                .map(apiResp -> {
                    ApiResponseReviewDetail.Builder builder = ApiResponseReviewDetail.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null && !apiResp.data().isEmpty()) {
                        builder.setData(toProto(apiResp.data().get(0)));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseReviewDetail> update(pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest request) {
        if (request.getReviewDetailId() <= 0) {
            return IdValidator.invalid("ReviewDetail id");
        }
        UpdateReviewDetailRequest domainReq = new UpdateReviewDetailRequest();
        domainReq.setReviewDetailId(request.getReviewDetailId());
        domainReq.setType(request.getType());
        domainReq.setFile(request.getUrl());
        domainReq.setCaption(request.getCaption());

        return reviewDetailService.update(List.of(domainReq))
                .map(apiResp -> {
                    ApiResponseReviewDetail.Builder builder = ApiResponseReviewDetail.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null && !apiResp.data().isEmpty()) {
                        builder.setData(toProto(apiResp.data().get(0)));
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
    public Uni<ApiResponseReviewDetailDeleteAt> trashedReviewDetail(FindByIdReviewDetailRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return reviewDetailService.trash(request.getId())
                .map(apiResp -> {
                    ApiResponseReviewDetailDeleteAt.Builder builder = ApiResponseReviewDetailDeleteAt.newBuilder()
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
    public Uni<ApiResponseReviewDetailDeleteAt> restoreReviewDetail(FindByIdReviewDetailRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return reviewDetailService.restore(request.getId())
                .map(apiResp -> {
                    ApiResponseReviewDetailDeleteAt.Builder builder = ApiResponseReviewDetailDeleteAt.newBuilder()
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
    public Uni<pb.review.ReviewCommon.ApiResponseReviewDelete> deleteReviewDetailPermanent(
            FindByIdReviewDetailRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return reviewDetailService.delete(request.getId())
                .map(apiResp -> pb.review.ReviewCommon.ApiResponseReviewDelete.newBuilder()
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
    public Uni<pb.review.ReviewCommon.ApiResponseReviewAll> restoreAllReviewDetail(com.google.protobuf.Empty request) {
        return reviewDetailService.restoreAll()
                .map(apiResp -> pb.review.ReviewCommon.ApiResponseReviewAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<pb.review.ReviewCommon.ApiResponseReviewAll> deleteAllReviewDetailPermanent(
            com.google.protobuf.Empty request) {
        return reviewDetailService.deleteAll()
                .map(apiResp -> pb.review.ReviewCommon.ApiResponseReviewAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.review_detail.ReviewDetailCommon.ReviewDetailsResponse toProto(ReviewDetailResponse r) {
        if (r == null) {
            return pb.review_detail.ReviewDetailCommon.ReviewDetailsResponse.getDefaultInstance();
        }
        return pb.review_detail.ReviewDetailCommon.ReviewDetailsResponse.newBuilder()
                .setId(r.getId())
                .setReviewId(r.getReviewId())
                .setType(r.getType() != null ? r.getType() : "")
                .setUrl(r.getUrl() != null ? r.getUrl() : "")
                .setCaption(r.getCaption() != null ? r.getCaption() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    private pb.review_detail.ReviewDetailCommon.ReviewDetailsResponseDeleteAt toProto(ReviewDetailResponseDeleteAt r) {
        if (r == null) {
            return pb.review_detail.ReviewDetailCommon.ReviewDetailsResponseDeleteAt.getDefaultInstance();
        }
        pb.review_detail.ReviewDetailCommon.ReviewDetailsResponseDeleteAt.Builder builder = pb.review_detail.ReviewDetailCommon.ReviewDetailsResponseDeleteAt
                .newBuilder()
                .setId(r.getId())
                .setReviewId(r.getReviewId())
                .setType(r.getType() != null ? r.getType() : "")
                .setUrl(r.getUrl() != null ? r.getUrl() : "")
                .setCaption(r.getCaption() != null ? r.getCaption() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
