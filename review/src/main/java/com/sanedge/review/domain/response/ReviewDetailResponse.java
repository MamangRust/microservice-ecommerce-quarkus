package com.sanedge.review.domain.response;

import java.util.List;
import java.util.stream.Collectors;

import com.sanedge.review.entity.ReviewDetail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailResponse {
    private int id;
    private int reviewId;
    private String type;
    private String url;
    private String caption;
    private String createdAt;
    private String updatedAt;

    public static ReviewDetailResponse from(ReviewDetail reviewDetail) {
        return ReviewDetailResponse.builder()
                .id(reviewDetail.id != null ? reviewDetail.id.intValue() : 0)
                .reviewId(reviewDetail.getReviewId())
                .type(reviewDetail.getType())
                .url(reviewDetail.getUrl())
                .caption(reviewDetail.getCaption())
                .createdAt(reviewDetail.getCreatedAt() != null ? reviewDetail.getCreatedAt().toString() : null)
                .updatedAt(reviewDetail.getUpdatedAt() != null ? reviewDetail.getUpdatedAt().toString() : null)
                .build();
    }

    public static List<ReviewDetailResponse> fromList(List<ReviewDetail> reviewDetails) {
        return reviewDetails.stream()
                .map(ReviewDetailResponse::from)
                .collect(Collectors.toList());
    }
}
