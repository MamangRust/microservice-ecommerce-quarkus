package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.ReviewDto;
import io.smallrye.mutiny.Uni;

public interface ReviewService {
    Uni<ReviewDto.ApiResponsePaginationReview> findAll(int page, int size, String search);
    Uni<ReviewDto.ApiResponsePaginationReview> findByProduct(int productId, int page, int size, String search);
    Uni<ReviewDto.ApiResponsePaginationReview> findByMerchant(int merchantId, int page, int size, String search);
    Uni<ReviewDto.ApiResponsePaginationReview> findByActive(int page, int size, String search);
    Uni<ReviewDto.ApiResponsePaginationReview> findByTrashed(int page, int size, String search);
    Uni<ReviewDto.ApiResponseReview> findById(int id);
    Uni<ReviewDto.ApiResponseReview> create(pb.review.ReviewCommand.CreateReviewRequest body);
    Uni<ReviewDto.ApiResponseReview> update(int id, pb.review.ReviewCommand.UpdateReviewRequest body);
    Uni<ReviewDto.ApiResponseReview> delete(int id);
    Uni<ReviewDto.ApiResponseReview> restore(int id);
    Uni<ReviewDto.SimpleResponse> deletePermanent(int id);
    Uni<ReviewDto.SimpleResponse> restoreAll();
    Uni<ReviewDto.SimpleResponse> deleteAllPermanent();
}
