package com.sanedge.review.service;

import java.util.List;

import com.sanedge.review.domain.requests.FindAllReview;
import com.sanedge.review.domain.requests.FindAllReviewByMerchant;
import com.sanedge.review.domain.requests.FindAllReviewByProduct;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.review.domain.response.ReviewRelationsDetailResponse;
import com.sanedge.review.domain.response.ReviewResponse;
import com.sanedge.review.domain.response.ReviewResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface ReviewQueryService {
    Uni<ApiResponsePagination<List<ReviewResponse>>> findAll(FindAllReview req);
    Uni<ApiResponsePagination<List<ReviewResponseDeleteAt>>> findActive(FindAllReview req);
    Uni<ApiResponsePagination<List<ReviewResponseDeleteAt>>> findTrashed(FindAllReview req);
    Uni<ApiResponsePagination<List<ReviewRelationsDetailResponse>>> findByMerchant(FindAllReviewByMerchant req);
    Uni<ApiResponsePagination<List<ReviewRelationsDetailResponse>>> findByProduct(FindAllReviewByProduct req);
    Uni<ApiResponse<ReviewResponse>> findById(Integer reviewId);
}
