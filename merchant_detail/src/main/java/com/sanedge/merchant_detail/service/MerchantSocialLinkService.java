package com.sanedge.merchant_detail.service;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_detail.domain.response.MerchantSocialMediaLinkResponse;
import com.sanedge.merchant_detail.domain.response.MerchantSocialMediaLinkResponseDeleteAt;

import io.smallrye.mutiny.Uni;
import pb.MerchantSocialLinkCommand.CreateMerchantSocialRequest;
import pb.MerchantSocialLinkCommand.UpdateMerchantSocialRequest;

public interface MerchantSocialLinkService {
    Uni<ApiResponse<MerchantSocialMediaLinkResponse>> create(CreateMerchantSocialRequest request);

    Uni<ApiResponse<MerchantSocialMediaLinkResponse>> update(UpdateMerchantSocialRequest request);

    Uni<ApiResponse<MerchantSocialMediaLinkResponseDeleteAt>> trash(Integer id);

    Uni<ApiResponse<MerchantSocialMediaLinkResponseDeleteAt>> restore(Integer id);

    Uni<ApiResponse<Boolean>> delete(Integer id);

    Uni<ApiResponse<Boolean>> restoreAll();

    Uni<ApiResponse<Boolean>> deleteAll();
}
