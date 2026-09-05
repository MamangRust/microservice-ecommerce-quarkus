package com.sanedge.merchant_award.service;

import java.util.List;

import com.sanedge.merchant_award.domain.requests.FindAllMerchantRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantAwardQueryService {
    Uni<ApiResponsePagination<List<MerchantAwardResponse>>> findAll(FindAllMerchantRequest req);
    Uni<ApiResponsePagination<List<MerchantAwardResponseDeleteAt>>> findByActive(FindAllMerchantRequest req);
    Uni<ApiResponsePagination<List<MerchantAwardResponseDeleteAt>>> findByTrashed(FindAllMerchantRequest req);
    Uni<ApiResponse<MerchantAwardResponse>> findById(Long merchantAwardId);
}
