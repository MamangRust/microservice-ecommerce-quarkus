package com.sanedge.merchant_detail.service;

import java.util.List;

import com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantDetailQueryService {
    Uni<ApiResponsePagination<List<MerchantDetailRelationResponse>>> findAll(FindAllMerchantRequest req);
    Uni<ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>>> findByActive(FindAllMerchantRequest req);
    Uni<ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>>> findByTrashed(FindAllMerchantRequest req);
    Uni<ApiResponse<MerchantDetailRelationResponse>> findById(Long merchantID);
}
