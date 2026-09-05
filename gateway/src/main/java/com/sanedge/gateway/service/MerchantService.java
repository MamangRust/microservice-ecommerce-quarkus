package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDto;
import io.smallrye.mutiny.Uni;

public interface MerchantService {
    Uni<MerchantDto.ApiResponsePaginationMerchant> listMerchants(int page, int size, String search);
    Uni<MerchantDto.ApiResponseMerchant> getMerchant(int id);
    Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> getActiveMerchants(int page, int size, String search);
    Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> getTrashedMerchants(int page, int size, String search);

    Uni<MerchantDto.ApiResponseMerchant> createMerchant(pb.merchant.MerchantCommand.CreateMerchantRequest body);
    Uni<MerchantDto.ApiResponseMerchant> updateMerchant(int id, pb.merchant.MerchantCommand.UpdateMerchantRequest body);
    Uni<MerchantDto.ApiResponseMerchant> updateMerchantStatus(int id, pb.merchant.MerchantCommand.UpdateMerchantStatusRequest body);
    
    Uni<MerchantDto.ApiResponseMerchantDeleteAt> deleteMerchant(int id);
    Uni<MerchantDto.ApiResponseMerchant> restoreMerchant(int id);
    Uni<MerchantDto.SimpleResponse> deleteMerchantPermanent(int id);
    Uni<MerchantDto.SimpleResponse> restoreAllMerchant();
    Uni<MerchantDto.SimpleResponse> deleteAllMerchantPermanent();
}
