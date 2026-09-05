package com.sanedge.review.domain.response;

import com.sanedge.review.entity.Review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDeleteAt {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private String name;
    private String comment;
    private Integer rating;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static ReviewResponseDeleteAt from(Review review) {
        return ReviewResponseDeleteAt.builder()
                .id(review.id != null ? review.id.intValue() : 0)
                .userId(review.getUserId())
                .productId(review.getProductId())
                .name(review.getName())
                .comment(review.getComment())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : null)
                .updatedAt(review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null)
                .deletedAt(review.getDeletedAt() != null ? review.getDeletedAt().toString() : null)
                .build();
    }
}
