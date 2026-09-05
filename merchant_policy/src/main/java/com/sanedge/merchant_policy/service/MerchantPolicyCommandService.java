package com.sanedge.merchant_policy.service;

import com.sanedge.merchant_policy.domain.requests.CreateMerchantPolicyRequest;
import com.sanedge.merchant_policy.domain.requests.UpdateMerchantPolicyRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantPolicyCommandService {
    Uni<ApiResponse<MerchantPoliciesResponse>> create(CreateMerchantPolicyRequest request);
    Uni<ApiResponse<MerchantPoliciesResponse>> update(UpdateMerchantPolicyRequest request);
    Uni<ApiResponse<MerchantPoliciesResponseDeleteAt>> trash(Long id);
    Uni<ApiResponse<MerchantPoliciesResponseDeleteAt>> restore(Long id);
    Uni<ApiResponse<Void>> delete(Long id);
    Uni<ApiResponse<Void>> restoreAll();
    Uni<ApiResponse<Void>> deleteAll();
}
