package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.BannerDto;
import com.sanedge.gateway.service.BannerService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BannerServiceImpl implements BannerService {

    private static final Logger LOG = Logger.getLogger(BannerServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("banner")
    pb.banner.MutinyBannerQueryServiceGrpc.MutinyBannerQueryServiceStub bannerQueryService;

    @GrpcClient("banner")
    pb.banner.MutinyBannerCommandServiceGrpc.MutinyBannerCommandServiceStub bannerCommandService;

    @Override
    public Uni<BannerDto.ApiResponsePaginationBanner> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("banner.findAll", () -> bannerQueryService.findAll(
                pb.banner.BannerQuery.FindAllBannerRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(BannerDto.ApiResponsePaginationBanner::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all banners: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.ApiResponsePaginationBanner> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("banner.findByActive", () -> bannerQueryService.findByActive(
                pb.banner.BannerQuery.FindAllBannerRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(BannerDto.ApiResponsePaginationBanner::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active banners: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.ApiResponsePaginationBanner> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("banner.findByTrashed", () -> bannerQueryService.findByTrashed(
                pb.banner.BannerQuery.FindAllBannerRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(BannerDto.ApiResponsePaginationBanner::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed banners: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.ApiResponseBanner> findById(int id) {
        return telemetryHelper.traceAndMetric("banner.findById", () -> bannerQueryService.findById(
                pb.banner.BannerCommon.FindByIdBannerRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(BannerDto.ApiResponseBanner::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find banner by id " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.ApiResponseBanner> createBanner(pb.banner.BannerCommand.CreateBannerRequest req) {
        return telemetryHelper.traceAndMetric("banner.create", () -> bannerCommandService.create(req)
                .map(BannerDto.ApiResponseBanner::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create banner: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.ApiResponseBanner> updateBanner(int id, pb.banner.BannerCommand.UpdateBannerRequest req) {
        pb.banner.BannerCommand.UpdateBannerRequest updated = pb.banner.BannerCommand.UpdateBannerRequest.newBuilder(req)
                .setBannerId(id)
                .build();
        return telemetryHelper.traceAndMetric("banner.update", () -> bannerCommandService.update(updated)
                .map(BannerDto.ApiResponseBanner::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update banner " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.ApiResponseBanner> deleteBanner(int id) {
        return telemetryHelper.traceAndMetric("banner.delete", () -> bannerCommandService.trash(
                pb.banner.BannerCommon.FindByIdBannerRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(BannerDto.ApiResponseBanner::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash banner " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.ApiResponseBanner> restoreBanner(int id) {
        return telemetryHelper.traceAndMetric("banner.restore", () -> bannerCommandService.restore(
                pb.banner.BannerCommon.FindByIdBannerRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(BannerDto.ApiResponseBanner::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore banner " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.SimpleResponse> deleteBannerPermanent(int id) {
        return telemetryHelper.traceAndMetric("banner.deletePermanent", () -> bannerCommandService.deletePermanent(
                pb.banner.BannerCommon.FindByIdBannerRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(BannerDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to delete banner permanently " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.SimpleResponse> restoreAllBanners() {
        return telemetryHelper.traceAndMetric("banner.restoreAll", () -> bannerCommandService.restoreAll(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(BannerDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all banners: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<BannerDto.SimpleResponse> deleteAllBannersPermanent() {
        return telemetryHelper.traceAndMetric("banner.deleteAllPermanent", () -> bannerCommandService.deleteAll(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(BannerDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all banners: " + throwable.getMessage(), throwable)));
    }
}
