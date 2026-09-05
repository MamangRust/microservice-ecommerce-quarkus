package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.ReviewDetailDto;
import io.smallrye.mutiny.Uni;

public interface ReviewDetailService {
    Uni<ReviewDetailDto.ApiResponsePaginationDetail> findAll(int page, int size, String search);
    Uni<ReviewDetailDto.ApiResponsePaginationDetail> findByActive(int page, int size, String search);
    Uni<ReviewDetailDto.ApiResponsePaginationDetail> findByTrashed(int page, int size, String search);
    Uni<ReviewDetailDto.ApiResponseDetail> findById(int id);

    Uni<ReviewDetailDto.ApiResponseDetail> create(pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest body);
    Uni<ReviewDetailDto.ApiResponseDetail> update(int id, pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest body);
    Uni<ReviewDetailDto.ApiResponseDetail> delete(int id);
    Uni<ReviewDetailDto.ApiResponseDetail> restore(int id);
    Uni<ReviewDetailDto.SimpleResponse> deletePermanent(int id);
    Uni<ReviewDetailDto.SimpleResponse> restoreAll();
    Uni<ReviewDetailDto.SimpleResponse> deleteAllPermanent();
}
