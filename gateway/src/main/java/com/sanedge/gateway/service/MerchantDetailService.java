package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDetailDto;
import io.smallrye.mutiny.Uni;

public interface MerchantDetailService {
    Uni<MerchantDetailDto.ApiResponsePaginationDetail> findAll(int page, int size, String search);
    Uni<MerchantDetailDto.ApiResponsePaginationDetail> findByActive(int page, int size, String search);
    Uni<MerchantDetailDto.ApiResponsePaginationDetail> findByTrashed(int page, int size, String search);
    Uni<MerchantDetailDto.ApiResponseDetail> findById(int id);
    Uni<MerchantDetailDto.ApiResponseDetail> create(pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest body);
    Uni<MerchantDetailDto.ApiResponseDetail> update(int id, pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest body);
    Uni<MerchantDetailDto.ApiResponseDetail> delete(int id);
    Uni<MerchantDetailDto.ApiResponseDetail> restore(int id);
    Uni<MerchantDetailDto.SimpleResponse> deletePermanent(int id);
    Uni<MerchantDetailDto.SimpleResponse> restoreAll();
    Uni<MerchantDetailDto.SimpleResponse> deleteAllPermanent();
}
