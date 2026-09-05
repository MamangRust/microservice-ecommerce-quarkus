package com.sanedge.review.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRelationsDetail {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private String name;
    private String comment;
    private Integer rating;
    private List<ReviewDetail> reviewDetail;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}