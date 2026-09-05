package com.sanedge.merchant_business.service;

import com.sanedge.merchant_business.domain.requests.CreateMerchantBusinessRequest;
import com.sanedge.merchant_business.domain.requests.UpdateMerchantBusinessRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantBusinessCommandService {
    Uni<ApiResponse<MerchantBusinessResponse>> createMerchantBusiness(CreateMerchantBusinessRequest req);
    Uni<ApiResponse<MerchantBusinessResponse>> updateMerchantBusiness(UpdateMerchantBusinessRequest req);
    Uni<ApiResponse<MerchantBusinessResponseDeleteAt>> trashedMerchantBusiness(Long merchantBusinessInfoId);
    Uni<ApiResponse<MerchantBusinessResponseDeleteAt>> restoreMerchantBusiness(Long merchantBusinessInfoId);
    Uni<ApiResponse<Void>> deleteMerchantBusinessPermanent(Long merchantBusinessInfoId);
    Uni<ApiResponse<Void>> restoreAllMerchantBusiness();
    Uni<ApiResponse<Void>> deleteAllMerchantBusinessPermanent();
}
