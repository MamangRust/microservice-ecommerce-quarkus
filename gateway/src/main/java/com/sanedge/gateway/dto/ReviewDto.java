package com.sanedge.gateway.dto;

import java.util.List;

public class ReviewDto {
    public record ReviewResponse(
            int id,
            int userId,
            int productId,
            String name,
            String comment,
            int rating,
            String createdAt,
            String updatedAt) {
        public static ReviewResponse from(pb.review.ReviewCommon.ReviewResponse proto) {
            return new ReviewResponse(
                    proto.getId(),
                    proto.getUserId(),
                    proto.getProductId(),
                    proto.getName(),
                    proto.getComment(),
                    proto.getRating(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static ReviewResponse from(pb.review.ReviewCommon.ReviewResponseDeleteAt proto) {
            return new ReviewResponse(
                    proto.getId(),
                    proto.getUserId(),
                    proto.getProductId(),
                    proto.getName(),
                    proto.getComment(),
                    proto.getRating(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static ReviewResponse from(pb.review.ReviewCommon.ReviewsDetailResponse proto) {
            return new ReviewResponse(
                    proto.getId(),
                    proto.getUserId(),
                    proto.getProductId(),
                    proto.getName(),
                    proto.getComment(),
                    proto.getRating(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllReviewResponse(
            List<ReviewResponse> data,
            String status,
            String message) {
        public static FindAllReviewResponse from(pb.review.ReviewCommon.ApiResponsePaginationReview proto) {
            return new FindAllReviewResponse(
                    proto.getDataList().stream().map(ReviewResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllReviewResponse from(pb.review.ReviewCommon.ApiResponsePaginationReviewDeleteAt proto) {
            return new FindAllReviewResponse(
                    proto.getDataList().stream().map(ReviewResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllReviewResponse from(pb.review.ReviewCommon.ApiResponsePaginationReviewDetail proto) {
            return new FindAllReviewResponse(
                    proto.getDataList().stream().map(ReviewResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdReviewResponse(
            ReviewResponse data,
            String status,
            String message) {
        public static FindByIdReviewResponse from(pb.review.ReviewCommon.ApiResponseReview proto) {
            return new FindByIdReviewResponse(
                    proto.hasData() ? ReviewResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdReviewResponse from(pb.review.ReviewCommon.ApiResponseReviewDeleteAt proto) {
            return new FindByIdReviewResponse(
                    proto.hasData() ? ReviewResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateReviewRequest(
            int userId,
            int productId,
            String name,
            String comment,
            int rating) {}

    public record CreateReviewResponse(
            ReviewResponse data,
            String status,
            String message) {
        public static CreateReviewResponse from(pb.review.ReviewCommon.ApiResponseReview proto) {
            return new CreateReviewResponse(
                    proto.hasData() ? ReviewResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateReviewRequest(
            int userId,
            int productId,
            String name,
            String comment,
            int rating) {}

    public record UpdateReviewResponse(
            ReviewResponse data,
            String status,
            String message) {
        public static UpdateReviewResponse from(pb.review.ReviewCommon.ApiResponseReview proto) {
            return new UpdateReviewResponse(
                    proto.hasData() ? ReviewResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.review.ReviewCommon.ApiResponseReviewDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.review.ReviewCommon.ApiResponseReviewAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponsePaginationReview(
            List<ReviewResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationReview from(pb.review.ReviewCommon.ApiResponsePaginationReview proto) {
            return new ApiResponsePaginationReview(
                    proto.getDataList().stream().map(ReviewResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationReview from(pb.review.ReviewCommon.ApiResponsePaginationReviewDeleteAt proto) {
            return new ApiResponsePaginationReview(
                    proto.getDataList().stream().map(ReviewResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationReview from(pb.review.ReviewCommon.ApiResponsePaginationReviewDetail proto) {
            return new ApiResponsePaginationReview(
                    proto.getDataList().stream().map(ReviewResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseReview(
            ReviewResponse data,
            String status,
            String message) {
        public static ApiResponseReview from(pb.review.ReviewCommon.ApiResponseReview proto) {
            return new ApiResponseReview(
                    proto.hasData() ? ReviewResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseReview from(pb.review.ReviewCommon.ApiResponseReviewDeleteAt proto) {
            return new ApiResponseReview(
                    proto.hasData() ? ReviewResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.review.ReviewCommon.ApiResponseReviewDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.review.ReviewCommon.ApiResponseReviewAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
