package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantAwardDto;
import io.smallrye.mutiny.Uni;

public interface MerchantAwardService {
    Uni<MerchantAwardDto.ApiResponsePaginationAward> findAll(int page, int size, String search);
    Uni<MerchantAwardDto.ApiResponsePaginationAward> findByActive(int page, int size, String search);
    Uni<MerchantAwardDto.ApiResponsePaginationAward> findByTrashed(int page, int size, String search);
    Uni<MerchantAwardDto.ApiResponseAward> findById(int id);
    Uni<MerchantAwardDto.ApiResponseAward> create(pb.merchant_award.MerchantAwardCommand.CreateMerchantAwardRequest body);
    Uni<MerchantAwardDto.ApiResponseAward> update(int id, pb.merchant_award.MerchantAwardCommand.UpdateMerchantAwardRequest body);
    Uni<MerchantAwardDto.ApiResponseAward> delete(int id);
    Uni<MerchantAwardDto.ApiResponseAward> restore(int id);
    Uni<MerchantAwardDto.SimpleResponse> deletePermanent(int id);
    Uni<MerchantAwardDto.SimpleResponse> restoreAll();
    Uni<MerchantAwardDto.SimpleResponse> deleteAllPermanent();
}
