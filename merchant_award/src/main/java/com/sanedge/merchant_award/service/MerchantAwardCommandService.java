package com.sanedge.merchant_award.service;

import com.sanedge.merchant_award.domain.requests.CreateMerchantAwardRequest;
import com.sanedge.merchant_award.domain.requests.UpdateMerchantAwardRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantAwardCommandService {
    Uni<ApiResponse<MerchantAwardResponse>> createMerchantAward(CreateMerchantAwardRequest req);
    Uni<ApiResponse<MerchantAwardResponse>> updateMerchantAward(UpdateMerchantAwardRequest req);
    Uni<ApiResponse<MerchantAwardResponseDeleteAt>> trashedMerchantAward(Long merchantAwardId);
    Uni<ApiResponse<MerchantAwardResponseDeleteAt>> restoreMerchantAward(Long merchantAwardId);
    Uni<ApiResponse<Void>> deleteMerchantAwardPermanent(Long merchantAwardId);
    Uni<ApiResponse<Void>> restoreAllMerchantAward();
    Uni<ApiResponse<Void>> deleteAllMerchantAwardPermanent();
}
