package com.sanedge.review.handler;

import com.sanedge.review.domain.requests.FindAllReview;
import com.sanedge.review.domain.requests.FindAllReviewByMerchant;
import com.sanedge.review.domain.requests.FindAllReviewByProduct;
import com.sanedge.review.domain.response.ReviewDetailResponse;
import com.sanedge.review.domain.response.ReviewRelationsDetailResponse;
import com.sanedge.review.domain.response.ReviewResponse;
import com.sanedge.review.domain.response.ReviewResponseDeleteAt;
import com.sanedge.review.service.ReviewQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.review.MutinyReviewQueryServiceGrpc;
import pb.review.ReviewCommon.ApiResponsePaginationReview;
import pb.review.ReviewCommon.ApiResponsePaginationReviewDeleteAt;
import pb.review.ReviewCommon.ApiResponsePaginationReviewDetail;
import pb.review.ReviewCommon.ApiResponseReview;
import pb.review.ReviewCommon.FindByIdReviewRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class ReviewQueryGrpcHandler extends MutinyReviewQueryServiceGrpc.ReviewQueryServiceImplBase {

    @Inject
    ReviewQueryService reviewQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationReview> findAll(pb.review.ReviewQuery.FindAllReviewRequest request) {
        FindAllReview domainReq = new FindAllReview();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return reviewQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationReview.Builder builder = ApiResponsePaginationReview.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (ReviewResponse r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationReviewDetail> findByProduct(
            pb.review.ReviewQuery.FindAllReviewProductRequest request) {
        if (request.getProductId() <= 0) {
            return IdValidator.invalid("Product id");
        }
        FindAllReviewByProduct domainReq = new FindAllReviewByProduct();
        domainReq.setProductId(request.getProductId());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return reviewQueryService.findByProduct(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationReviewDetail.Builder builder = ApiResponsePaginationReviewDetail.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (ReviewRelationsDetailResponse r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationReviewDetail> findByMerchant(
            pb.review.ReviewQuery.FindAllReviewMerchantRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        FindAllReviewByMerchant domainReq = new FindAllReviewByMerchant();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return reviewQueryService.findByMerchant(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationReviewDetail.Builder builder = ApiResponsePaginationReviewDetail.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (ReviewRelationsDetailResponse r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationReviewDeleteAt> findByActive(pb.review.ReviewQuery.FindAllReviewRequest request) {
        FindAllReview domainReq = new FindAllReview();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return reviewQueryService.findActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationReviewDeleteAt.Builder builder = ApiResponsePaginationReviewDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (ReviewResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationReviewDeleteAt> findByTrashed(pb.review.ReviewQuery.FindAllReviewRequest request) {
        FindAllReview domainReq = new FindAllReview();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return reviewQueryService.findTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationReviewDeleteAt.Builder builder = ApiResponsePaginationReviewDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (ReviewResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponseReview> findById(FindByIdReviewRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return reviewQueryService.findById(request.getId())
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

    private pb.review.ReviewCommon.ReviewsDetailResponse toProto(ReviewRelationsDetailResponse r) {
        if (r == null) {
            return pb.review.ReviewCommon.ReviewsDetailResponse.getDefaultInstance();
        }
        pb.review.ReviewCommon.ReviewsDetailResponse.Builder builder = pb.review.ReviewCommon.ReviewsDetailResponse
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
        if (r.getReviewDetail() != null && !r.getReviewDetail().isEmpty()) {
            builder.setReviewDetail(toProto(r.getReviewDetail().get(0)));
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(r.getDeletedAt());
        }
        return builder.build();
    }

    private pb.review.ReviewCommon.ReviewDetailResponse toProto(ReviewDetailResponse r) {
        if (r == null) {
            return pb.review.ReviewCommon.ReviewDetailResponse.getDefaultInstance();
        }
        return pb.review.ReviewCommon.ReviewDetailResponse.newBuilder()
                .setId(r.getId())
                .setType(r.getType())
                .setUrl(r.getUrl())
                .setCaption(r.getCaption())
                .setCreatedAt(r.getCreatedAt())
                .build();
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
