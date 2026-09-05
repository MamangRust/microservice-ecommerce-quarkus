package com.sanedge.review.service;

import com.sanedge.review.domain.requests.CreateReviewRequest;
import com.sanedge.review.domain.requests.UpdateReviewRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.review.domain.response.ReviewResponse;
import com.sanedge.review.domain.response.ReviewResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface ReviewCommandService {
    Uni<ApiResponse<ReviewResponse>> create(CreateReviewRequest request);
    Uni<ApiResponse<ReviewResponse>> update(UpdateReviewRequest request);
    Uni<ApiResponse<ReviewResponseDeleteAt>> trash(Integer id);
    Uni<ApiResponse<ReviewResponseDeleteAt>> restore(Integer id);
    Uni<ApiResponse<Void>> delete(Integer id);
    Uni<ApiResponse<Void>> restoreAll();
    Uni<ApiResponse<Void>> deleteAll();
}
