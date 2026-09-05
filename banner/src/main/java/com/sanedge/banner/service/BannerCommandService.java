package com.sanedge.banner.service;

import com.sanedge.banner.domain.requests.CreateBannerRequest;
import com.sanedge.banner.domain.requests.UpdateBannerRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface BannerCommandService {
    Uni<ApiResponse<BannerResponse>> createBanner(CreateBannerRequest request);
    Uni<ApiResponse<BannerResponse>> updateBanner(UpdateBannerRequest request);
    Uni<ApiResponse<BannerResponseDeleteAt>> trashedBanner(Long bannerId);
    Uni<ApiResponse<BannerResponseDeleteAt>> restoreBanner(Long bannerId);
    Uni<ApiResponse<Void>> deleteBannerPermanent(Long bannerId);
    Uni<ApiResponse<Void>> restoreAllBanner();
    Uni<ApiResponse<Void>> deleteAllBannerPermanent();
}
