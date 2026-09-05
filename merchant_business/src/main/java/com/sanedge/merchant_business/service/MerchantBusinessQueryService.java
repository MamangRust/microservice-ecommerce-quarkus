package com.sanedge.merchant_business.service;

import java.util.List;

import com.sanedge.merchant_business.domain.requests.FindAllMerchantRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantBusinessQueryService {
    Uni<ApiResponsePagination<List<MerchantBusinessResponse>>> findAll(FindAllMerchantRequest req);
    Uni<ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>>> findByActive(FindAllMerchantRequest req);
    Uni<ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>>> findByTrashed(FindAllMerchantRequest req);
    Uni<ApiResponse<MerchantBusinessResponse>> findById(Long merchantBusinessInfoId);
}
