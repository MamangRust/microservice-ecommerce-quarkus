package com.sanedge.review_detail.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailRecord {
    private Integer detailId;
    private String type;
    private String url;
    private String caption;
    private String createdAt;
}