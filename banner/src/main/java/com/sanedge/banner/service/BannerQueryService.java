package com.sanedge.banner.service;

import java.util.List;

import com.sanedge.banner.domain.requests.FindAllBannerRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface BannerQueryService {
    Uni<ApiResponsePagination<List<BannerResponse>>> findAllPaginated(FindAllBannerRequest request);
    Uni<ApiResponsePagination<List<BannerResponseDeleteAt>>> findActivePaginated(FindAllBannerRequest request);
    Uni<ApiResponsePagination<List<BannerResponseDeleteAt>>> findTrashedPaginated(FindAllBannerRequest request);
    Uni<ApiResponse<BannerResponse>> findById(Long id);
}
