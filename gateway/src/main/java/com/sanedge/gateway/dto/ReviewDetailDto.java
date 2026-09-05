package com.sanedge.gateway.dto;

import java.util.List;

public class ReviewDetailDto {

    public record ReviewDetailsResponse(
            int id,
            int reviewId,
            String type,
            String url,
            String caption,
            String createdAt,
            String updatedAt) {
        public static ReviewDetailsResponse from(pb.review_detail.ReviewDetailCommon.ReviewDetailsResponse proto) {
            return new ReviewDetailsResponse(
                    proto.getId(),
                    proto.getReviewId(),
                    proto.getType(),
                    proto.getUrl(),
                    proto.getCaption(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }

        public static ReviewDetailsResponse from(pb.review_detail.ReviewDetailCommon.ReviewDetailsResponseDeleteAt proto) {
            return new ReviewDetailsResponse(
                    proto.getId(),
                    proto.getReviewId(),
                    proto.getType(),
                    proto.getUrl(),
                    proto.getCaption(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    public record ApiResponsePaginationDetail(
            String status,
            String message,
            List<ReviewDetailsResponse> data) {
        public static ApiResponsePaginationDetail from(pb.review_detail.ReviewDetailCommon.ApiResponsePaginationReviewDetails proto) {
            return new ApiResponsePaginationDetail(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(ReviewDetailsResponse::from).toList());
        }

        public static ApiResponsePaginationDetail from(pb.review_detail.ReviewDetailCommon.ApiResponsePaginationReviewDetailsDeleteAt proto) {
            return new ApiResponsePaginationDetail(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(ReviewDetailsResponse::from).toList());
        }
    }

    public record ApiResponseDetail(
            String status,
            String message,
            ReviewDetailsResponse data) {
        public static ApiResponseDetail from(pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail proto) {
            return new ApiResponseDetail(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? ReviewDetailsResponse.from(proto.getData()) : null);
        }

        public static ApiResponseDetail from(pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt proto) {
            return new ApiResponseDetail(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? ReviewDetailsResponse.from(proto.getData()) : null);
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.review.ReviewCommon.ApiResponseReviewDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.review.ReviewCommon.ApiResponseReviewAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
