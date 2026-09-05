package com.sanedge.merchant_detail.service;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailResponseDeleteAt;

import io.smallrye.mutiny.Uni;
import pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest;
import pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest;

public interface MerchantDetailCommandService {
    Uni<ApiResponse<MerchantDetailResponse>> createMerchant(CreateMerchantDetailRequest req);

    Uni<ApiResponse<MerchantDetailResponse>> updateMerchant(UpdateMerchantDetailRequest req);

    Uni<ApiResponse<MerchantDetailResponseDeleteAt>> trashedMerchant(Long merchantID);

    Uni<ApiResponse<MerchantDetailResponseDeleteAt>> restoreMerchant(Long merchantID);

    Uni<ApiResponse<Void>> deleteMerchantPermanent(Long merchantID);

    Uni<ApiResponse<Void>> restoreAllMerchant();

    Uni<ApiResponse<Void>> deleteAllMerchantPermanent();
}
