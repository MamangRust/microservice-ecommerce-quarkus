package com.sanedge.review_detail.service;

import java.util.List;

import com.sanedge.review_detail.domain.requests.CreateReviewDetailRequest;
import com.sanedge.review_detail.domain.requests.UpdateReviewDetailRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.review_detail.domain.response.ReviewDetailResponse;
import com.sanedge.review_detail.domain.response.ReviewDetailResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface ReviewDetailService {
    Uni<ApiResponse<List<ReviewDetailResponse>>> create(List<CreateReviewDetailRequest> requests);
    Uni<ApiResponse<List<ReviewDetailResponse>>> update(List<UpdateReviewDetailRequest> requests);
    Uni<ApiResponse<ReviewDetailResponseDeleteAt>> trash(Integer reviewDetailId);
    Uni<ApiResponse<ReviewDetailResponseDeleteAt>> restore(Integer reviewDetailId);
    Uni<ApiResponse<Void>> delete(Integer reviewDetailId);
    Uni<ApiResponse<Void>> restoreAll();
    Uni<ApiResponse<Void>> deleteAll();

    Uni<com.sanedge.common.domain.response.ApiResponsePagination<List<ReviewDetailResponse>>> findAll(int page, int size, String search);
    Uni<com.sanedge.common.domain.response.ApiResponsePagination<List<ReviewDetailResponseDeleteAt>>> findByActive(int page, int size, String search);
    Uni<com.sanedge.common.domain.response.ApiResponsePagination<List<ReviewDetailResponseDeleteAt>>> findByTrashed(int page, int size, String search);
    Uni<ApiResponse<ReviewDetailResponse>> findById(Integer id);
}
