package com.sanedge.review_detail.handler;

import com.sanedge.review_detail.domain.response.ReviewDetailResponse;
import com.sanedge.review_detail.domain.response.ReviewDetailResponseDeleteAt;
import com.sanedge.review_detail.service.ReviewDetailService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.review_detail.MutinyReviewDetailQueryServiceGrpc;
import pb.review_detail.ReviewDetailCommon.ApiResponsePaginationReviewDetails;
import pb.review_detail.ReviewDetailCommon.ApiResponsePaginationReviewDetailsDeleteAt;
import pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail;
import pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class ReviewDetailQueryGrpcHandler extends MutinyReviewDetailQueryServiceGrpc.ReviewDetailQueryServiceImplBase {

    @Inject
    ReviewDetailService reviewDetailService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationReviewDetails> findAll(pb.review.ReviewQuery.FindAllReviewRequest request) {
        return reviewDetailService.findAll(request.getPage(), request.getPageSize(), request.getSearch())
                .map(apiResp -> {
                    ApiResponsePaginationReviewDetails.Builder builder = ApiResponsePaginationReviewDetails.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (ReviewDetailResponse r : apiResp.data()) {
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
    public Uni<ApiResponseReviewDetail> findById(FindByIdReviewDetailRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return reviewDetailService.findById(request.getId())
                .map(apiResp -> {
                    ApiResponseReviewDetail.Builder builder = ApiResponseReviewDetail.newBuilder()
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
    public Uni<ApiResponsePaginationReviewDetailsDeleteAt> findByActive(
            pb.review.ReviewQuery.FindAllReviewRequest request) {
        return reviewDetailService.findByActive(request.getPage(), request.getPageSize(), request.getSearch())
                .map(apiResp -> {
                    ApiResponsePaginationReviewDetailsDeleteAt.Builder builder = ApiResponsePaginationReviewDetailsDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (ReviewDetailResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationReviewDetailsDeleteAt> findByTrashed(
            pb.review.ReviewQuery.FindAllReviewRequest request) {
        return reviewDetailService.findByTrashed(request.getPage(), request.getPageSize(), request.getSearch())
                .map(apiResp -> {
                    ApiResponsePaginationReviewDetailsDeleteAt.Builder builder = ApiResponsePaginationReviewDetailsDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (ReviewDetailResponseDeleteAt r : apiResp.data()) {
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
