package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.BannerDto;
import io.smallrye.mutiny.Uni;

public interface BannerService {
    Uni<BannerDto.ApiResponsePaginationBanner> findAll(int page, int size, String search);
    Uni<BannerDto.ApiResponsePaginationBanner> findByActive(int page, int size, String search);
    Uni<BannerDto.ApiResponsePaginationBanner> findByTrashed(int page, int size, String search);
    Uni<BannerDto.ApiResponseBanner> findById(int id);
    Uni<BannerDto.ApiResponseBanner> createBanner(pb.banner.BannerCommand.CreateBannerRequest req);
    Uni<BannerDto.ApiResponseBanner> updateBanner(int id, pb.banner.BannerCommand.UpdateBannerRequest req);
    Uni<BannerDto.ApiResponseBanner> deleteBanner(int id);
    Uni<BannerDto.ApiResponseBanner> restoreBanner(int id);
    Uni<BannerDto.SimpleResponse> deleteBannerPermanent(int id);
    Uni<BannerDto.SimpleResponse> restoreAllBanners();
    Uni<BannerDto.SimpleResponse> deleteAllBannersPermanent();
}
