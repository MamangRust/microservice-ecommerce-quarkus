package com.sanedge.merchant_policy.service;

import java.util.List;

import com.sanedge.merchant_policy.domain.requests.FindAllMerchantRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantPolicyQueryService {
    Uni<ApiResponsePagination<List<MerchantPoliciesResponse>>> findAll(FindAllMerchantRequest req);
    Uni<ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>>> findByActive(FindAllMerchantRequest req);
    Uni<ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>>> findByTrashed(FindAllMerchantRequest req);
    Uni<ApiResponse<MerchantPoliciesResponse>> findById(Long id);
}
