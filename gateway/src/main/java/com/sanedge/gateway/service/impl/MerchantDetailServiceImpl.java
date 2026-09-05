package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantDetailDto;
import com.sanedge.gateway.service.MerchantDetailService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantDetailServiceImpl implements MerchantDetailService {

    private static final Logger LOG = Logger.getLogger(MerchantDetailServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant_detail")
    pb.merchant_detail.MutinyMerchantDetailQueryServiceGrpc.MutinyMerchantDetailQueryServiceStub merchantDetailQueryService;

    @GrpcClient("merchant_detail")
    pb.merchant_detail.MutinyMerchantDetailCommandServiceGrpc.MutinyMerchantDetailCommandServiceStub merchantDetailCommandService;

    @Override
    public Uni<MerchantDetailDto.ApiResponsePaginationDetail> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.detail.findAll", () -> merchantDetailQueryService.findAll(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDetailDto.ApiResponsePaginationDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all details: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.ApiResponsePaginationDetail> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.detail.findByActive", () -> merchantDetailQueryService.findByActive(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDetailDto.ApiResponsePaginationDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active details: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.ApiResponsePaginationDetail> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.detail.findByTrashed", () -> merchantDetailQueryService.findByTrashed(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDetailDto.ApiResponsePaginationDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed details: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.ApiResponseDetail> findById(int id) {
        return telemetryHelper.traceAndMetric("merchant.detail.findById", () -> merchantDetailQueryService.findById(
                pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.ApiResponseDetail> create(pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest body) {
        return telemetryHelper.traceAndMetric("merchant.detail.create", () -> merchantDetailCommandService.create(body)
                .map(MerchantDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create detail: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.ApiResponseDetail> update(int id, pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest body) {
        pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest req = pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest.newBuilder(body)
                .setMerchantDetailId(id)
                .build();
        return telemetryHelper.traceAndMetric("merchant.detail.update", () -> merchantDetailCommandService.update(req)
                .map(MerchantDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.ApiResponseDetail> delete(int id) {
        return telemetryHelper.traceAndMetric("merchant.detail.delete", () -> merchantDetailCommandService.trashedMerchantDetail(
                pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.ApiResponseDetail> restore(int id) {
        return telemetryHelper.traceAndMetric("merchant.detail.restore", () -> merchantDetailCommandService.restoreMerchantDetail(
                pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("merchant.detail.deletePermanent", () -> merchantDetailCommandService.deleteMerchantDetailPermanent(
                pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantDetailDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("merchant.detail.restoreAll", () -> merchantDetailCommandService.restoreAllMerchantDetail(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantDetailDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all details: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDetailDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("merchant.detail.deleteAllPermanent", () -> merchantDetailCommandService.deleteAllMerchantDetailPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantDetailDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all details: " + throwable.getMessage(), throwable)));
    }
}
