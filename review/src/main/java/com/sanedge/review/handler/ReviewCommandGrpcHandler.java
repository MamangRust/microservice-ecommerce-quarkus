package com.sanedge.review.handler;

import com.sanedge.review.domain.requests.CreateReviewRequest;
import com.sanedge.review.domain.requests.UpdateReviewRequest;
import com.sanedge.review.domain.response.ReviewResponse;
import com.sanedge.review.domain.response.ReviewResponseDeleteAt;
import com.sanedge.review.service.ReviewCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.review.MutinyReviewCommandServiceGrpc;
import pb.review.ReviewCommon.ApiResponseReview;
import pb.review.ReviewCommon.ApiResponseReviewDeleteAt;
import pb.review.ReviewCommon.ApiResponseReviewDelete;
import pb.review.ReviewCommon.ApiResponseReviewAll;
import pb.review.ReviewCommon.FindByIdReviewRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class ReviewCommandGrpcHandler extends MutinyReviewCommandServiceGrpc.ReviewCommandServiceImplBase {

    @Inject
    ReviewCommandService reviewCommandService;

    @Override
    public Uni<ApiResponseReview> create(pb.review.ReviewCommand.CreateReviewRequest request) {
        if (request.getUserId() <= 0) {
            return IdValidator.invalid("User id");
        }
        if (request.getProductId() <= 0) {
            return IdValidator.invalid("Product id");
        }
        CreateReviewRequest domainReq = new CreateReviewRequest();
        domainReq.setUserId(request.getUserId());
        domainReq.setProductId(request.getProductId());
        domainReq.setName(request.getName());
        domainReq.setComment(request.getComment());
        domainReq.setRating(request.getRating());

        return reviewCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseReview.Builder builder = ApiResponseReview.newBuilder()
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
    public Uni<ApiResponseReview> update(pb.review.ReviewCommand.UpdateReviewRequest request) {
        if (request.getReviewId() <= 0) {
            return IdValidator.invalid("Review id");
        }
        UpdateReviewRequest domainReq = new UpdateReviewRequest();
        domainReq.setReviewId(request.getReviewId());
        domainReq.setName(request.getName());
        domainReq.setComment(request.getComment());
        domainReq.setRating(request.getRating());

        return reviewCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseReview.Builder builder = ApiResponseReview.newBuilder()
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
    public Uni<ApiResponseReviewDeleteAt> trashedReview(FindByIdReviewRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return reviewCommandService.trash(request.getId())
                .map(apiResp -> {
                    ApiResponseReviewDeleteAt.Builder builder = ApiResponseReviewDeleteAt.newBuilder()
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
    public Uni<ApiResponseReviewDeleteAt> restoreReview(FindByIdReviewRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return reviewCommandService.restore(request.getId())
                .map(apiResp -> {
                    ApiResponseReviewDeleteAt.Builder builder = ApiResponseReviewDeleteAt.newBuilder()
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
    public Uni<ApiResponseReviewDelete> deleteReviewPermanent(FindByIdReviewRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return reviewCommandService.delete(request.getId())
                .map(apiResp -> ApiResponseReviewDelete.newBuilder()
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
    public Uni<ApiResponseReviewAll> restoreAllReview(com.google.protobuf.Empty request) {
        return reviewCommandService.restoreAll()
                .map(apiResp -> ApiResponseReviewAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseReviewAll> deleteAllReviewPermanent(com.google.protobuf.Empty request) {
        return reviewCommandService.deleteAll()
                .map(apiResp -> ApiResponseReviewAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.review.ReviewCommon.ReviewResponse toProto(ReviewResponse r) {
        if (r == null) {
            return pb.review.ReviewCommon.ReviewResponse.getDefaultInstance();
        }
        pb.review.ReviewCommon.ReviewResponse.Builder builder = pb.review.ReviewCommon.ReviewResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId());
        }
        if (r.getProductId() != null) {
            builder.setProductId(r.getProductId());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getComment() != null) {
            builder.setComment(r.getComment());
        }
        if (r.getRating() != null) {
            builder.setRating(r.getRating());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.review.ReviewCommon.ReviewResponseDeleteAt toProto(ReviewResponseDeleteAt r) {
        if (r == null) {
            return pb.review.ReviewCommon.ReviewResponseDeleteAt.getDefaultInstance();
        }
        pb.review.ReviewCommon.ReviewResponseDeleteAt.Builder builder = pb.review.ReviewCommon.ReviewResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId());
        }
        if (r.getProductId() != null) {
            builder.setProductId(r.getProductId());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getComment() != null) {
            builder.setComment(r.getComment());
        }
        if (r.getRating() != null) {
            builder.setRating(r.getRating());
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
