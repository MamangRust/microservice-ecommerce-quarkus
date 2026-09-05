package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantAwardDto;
import com.sanedge.gateway.service.MerchantAwardService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantAwardServiceImpl implements MerchantAwardService {

    private static final Logger LOG = Logger.getLogger(MerchantAwardServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant_award")
    pb.merchant_award.MutinyMerchantAwardQueryServiceGrpc.MutinyMerchantAwardQueryServiceStub merchantAwardQueryService;

    @GrpcClient("merchant_award")
    pb.merchant_award.MutinyMerchantAwardCommandServiceGrpc.MutinyMerchantAwardCommandServiceStub merchantAwardCommandService;

    @Override
    public Uni<MerchantAwardDto.ApiResponsePaginationAward> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.award.findAll", () -> merchantAwardQueryService.findAll(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantAwardDto.ApiResponsePaginationAward::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all awards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.ApiResponsePaginationAward> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.award.findByActive", () -> merchantAwardQueryService.findByActive(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantAwardDto.ApiResponsePaginationAward::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active awards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.ApiResponsePaginationAward> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.award.findByTrashed", () -> merchantAwardQueryService.findByTrashed(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantAwardDto.ApiResponsePaginationAward::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed awards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.ApiResponseAward> findById(int id) {
        return telemetryHelper.traceAndMetric("merchant.award.findById", () -> merchantAwardQueryService.findById(
                pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantAwardDto.ApiResponseAward::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find award " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.ApiResponseAward> create(pb.merchant_award.MerchantAwardCommand.CreateMerchantAwardRequest body) {
        return telemetryHelper.traceAndMetric("merchant.award.create", () -> merchantAwardCommandService.create(body)
                .map(MerchantAwardDto.ApiResponseAward::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create award: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.ApiResponseAward> update(int id, pb.merchant_award.MerchantAwardCommand.UpdateMerchantAwardRequest body) {
        pb.merchant_award.MerchantAwardCommand.UpdateMerchantAwardRequest req = pb.merchant_award.MerchantAwardCommand.UpdateMerchantAwardRequest.newBuilder(body)
                .setMerchantCertificationId(id)
                .build();
        return telemetryHelper.traceAndMetric("merchant.award.update", () -> merchantAwardCommandService.update(req)
                .map(MerchantAwardDto.ApiResponseAward::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update award " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.ApiResponseAward> delete(int id) {
        return telemetryHelper.traceAndMetric("merchant.award.delete", () -> merchantAwardCommandService.trashedMerchantAward(
                pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantAwardDto.ApiResponseAward::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete award " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.ApiResponseAward> restore(int id) {
        return telemetryHelper.traceAndMetric("merchant.award.restore", () -> merchantAwardCommandService.restoreMerchantAward(
                pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantAwardDto.ApiResponseAward::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore award " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("merchant.award.deletePermanent", () -> merchantAwardCommandService.deleteMerchantAwardPermanent(
                pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantAwardDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete award " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("merchant.award.restoreAll", () -> merchantAwardCommandService.restoreAllMerchantAward(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantAwardDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all awards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantAwardDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("merchant.award.deleteAllPermanent", () -> merchantAwardCommandService.deleteAllMerchantAwardPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantAwardDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all awards: " + throwable.getMessage(), throwable)));
    }
}
