package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantDto;
import com.sanedge.gateway.service.MerchantService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantServiceImpl implements MerchantService {

    private static final Logger LOG = Logger.getLogger(MerchantServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;

    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantCommandServiceGrpc.MutinyMerchantCommandServiceStub merchantCommandService;

    @Override
    public Uni<MerchantDto.ApiResponsePaginationMerchant> listMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.list", () -> merchantQueryService.findAll(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDto.ApiResponsePaginationMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> getMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.get", () -> merchantQueryService.findById(
                pb.merchant.MerchantCommon.FindByIdMerchantRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> getActiveMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.getActiveMerchants", () -> merchantQueryService.findByActive(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDto.ApiResponsePaginationMerchantDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get active merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> getTrashedMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.getTrashedMerchants", () -> merchantQueryService.findByTrashed(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantDto.ApiResponsePaginationMerchantDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get trashed merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> createMerchant(pb.merchant.MerchantCommand.CreateMerchantRequest body) {
        return telemetryHelper.traceAndMetric("merchant.create", () -> merchantCommandService.create(body)
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> updateMerchant(int id, pb.merchant.MerchantCommand.UpdateMerchantRequest body) {
        pb.merchant.MerchantCommand.UpdateMerchantRequest req = pb.merchant.MerchantCommand.UpdateMerchantRequest.newBuilder(body)
                .setMerchantId(id)
                .build();
        return telemetryHelper.traceAndMetric("merchant.update", () -> merchantCommandService.update(req)
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> updateMerchantStatus(int id, pb.merchant.MerchantCommand.UpdateMerchantStatusRequest body) {
        pb.merchant.MerchantCommand.UpdateMerchantStatusRequest req = pb.merchant.MerchantCommand.UpdateMerchantStatusRequest.newBuilder(body)
                .setMerchantId(id)
                .build();
        return telemetryHelper.traceAndMetric("merchant.updateStatus", () -> merchantCommandService.updateStatus(req)
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update status of merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchantDeleteAt> deleteMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.delete", () -> merchantCommandService.trashedMerchant(
                pb.merchant.MerchantCommon.FindByIdMerchantRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantDto.ApiResponseMerchantDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.ApiResponseMerchant> restoreMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.restore", () -> merchantCommandService.restoreMerchant(
                pb.merchant.MerchantCommon.FindByIdMerchantRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantDto.ApiResponseMerchant::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.SimpleResponse> deleteMerchantPermanent(int id) {
        return telemetryHelper.traceAndMetric("merchant.deletePermanent", () -> merchantCommandService.deleteMerchantPermanent(
                pb.merchant.MerchantCommon.FindByIdMerchantRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete merchant " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.SimpleResponse> restoreAllMerchant() {
        return telemetryHelper.traceAndMetric("merchant.restoreAll", () -> merchantCommandService.restoreAllMerchant(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantDto.SimpleResponse> deleteAllMerchantPermanent() {
        return telemetryHelper.traceAndMetric("merchant.deleteAllPermanent", () -> merchantCommandService.deleteAllMerchantPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all merchants: " + throwable.getMessage(), throwable)));
    }
}
