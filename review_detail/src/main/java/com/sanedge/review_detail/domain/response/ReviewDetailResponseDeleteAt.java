package com.sanedge.review_detail.domain.response;

import com.sanedge.review_detail.entity.ReviewDetail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailResponseDeleteAt {
    private int id;
    private int reviewId;
    private String type;
    private String url;
    private String caption;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static ReviewDetailResponseDeleteAt from(ReviewDetail reviewDetail) {
        return ReviewDetailResponseDeleteAt.builder()
                .id(reviewDetail.id != null ? reviewDetail.id.intValue() : 0)
                .reviewId(reviewDetail.getReviewId())
                .type(reviewDetail.getType())
                .url(reviewDetail.getUrl())
                .caption(reviewDetail.getCaption())
                .createdAt(reviewDetail.getCreatedAt() != null ? reviewDetail.getCreatedAt().toString() : null)
                .updatedAt(reviewDetail.getUpdatedAt() != null ? reviewDetail.getUpdatedAt().toString() : null)
                .deletedAt(reviewDetail.getDeletedAt() != null ? reviewDetail.getDeletedAt().toString() : null)
                .build();
    }
}
