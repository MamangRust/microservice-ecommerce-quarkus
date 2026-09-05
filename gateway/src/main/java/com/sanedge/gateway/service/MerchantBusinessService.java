package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantBusinessDto;
import io.smallrye.mutiny.Uni;

public interface MerchantBusinessService {
    Uni<MerchantBusinessDto.ApiResponsePaginationBusiness> findAll(int page, int size, String search);
    Uni<MerchantBusinessDto.ApiResponsePaginationBusiness> findByActive(int page, int size, String search);
    Uni<MerchantBusinessDto.ApiResponsePaginationBusiness> findByTrashed(int page, int size, String search);
    Uni<MerchantBusinessDto.ApiResponseBusiness> findById(int id);
    Uni<MerchantBusinessDto.ApiResponseBusiness> create(pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest body);
    Uni<MerchantBusinessDto.ApiResponseBusiness> update(int id, pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest body);
    Uni<MerchantBusinessDto.ApiResponseBusiness> delete(int id);
    Uni<MerchantBusinessDto.ApiResponseBusiness> restore(int id);
    Uni<MerchantBusinessDto.SimpleResponse> deletePermanent(int id);
    Uni<MerchantBusinessDto.SimpleResponse> restoreAll();
    Uni<MerchantBusinessDto.SimpleResponse> deleteAllPermanent();
}
