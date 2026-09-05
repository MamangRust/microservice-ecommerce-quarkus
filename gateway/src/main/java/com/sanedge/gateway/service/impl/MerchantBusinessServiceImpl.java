package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantBusinessDto;
import com.sanedge.gateway.service.MerchantBusinessService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantBusinessServiceImpl implements MerchantBusinessService {

    private static final Logger LOG = Logger.getLogger(MerchantBusinessServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant_business")
    pb.merchant_business.MutinyMerchantBusinessQueryServiceGrpc.MutinyMerchantBusinessQueryServiceStub merchantBusinessQueryService;

    @GrpcClient("merchant_business")
    pb.merchant_business.MutinyMerchantBusinessCommandServiceGrpc.MutinyMerchantBusinessCommandServiceStub merchantBusinessCommandService;

    @Override
    public Uni<MerchantBusinessDto.ApiResponsePaginationBusiness> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.business.findAll", () -> merchantBusinessQueryService.findAll(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantBusinessDto.ApiResponsePaginationBusiness::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all businesses: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.ApiResponsePaginationBusiness> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.business.findByActive", () -> merchantBusinessQueryService.findByActive(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantBusinessDto.ApiResponsePaginationBusiness::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active businesses: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.ApiResponsePaginationBusiness> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.business.findByTrashed", () -> merchantBusinessQueryService.findByTrashed(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantBusinessDto.ApiResponsePaginationBusiness::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed businesses: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.ApiResponseBusiness> findById(int id) {
        return telemetryHelper.traceAndMetric("merchant.business.findById", () -> merchantBusinessQueryService.findById(
                pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantBusinessDto.ApiResponseBusiness::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find business " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.ApiResponseBusiness> create(pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest body) {
        return telemetryHelper.traceAndMetric("merchant.business.create", () -> merchantBusinessCommandService.create(body)
                .map(MerchantBusinessDto.ApiResponseBusiness::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create business: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.ApiResponseBusiness> update(int id, pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest body) {
        pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest req = pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest.newBuilder(body)
                .setMerchantBusinessInfoId(id)
                .build();
        return telemetryHelper.traceAndMetric("merchant.business.update", () -> merchantBusinessCommandService.update(req)
                .map(MerchantBusinessDto.ApiResponseBusiness::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update business " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.ApiResponseBusiness> delete(int id) {
        return telemetryHelper.traceAndMetric("merchant.business.delete", () -> merchantBusinessCommandService.trashedMerchantBusiness(
                pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantBusinessDto.ApiResponseBusiness::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete business " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.ApiResponseBusiness> restore(int id) {
        return telemetryHelper.traceAndMetric("merchant.business.restore", () -> merchantBusinessCommandService.restoreMerchantBusiness(
                pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantBusinessDto.ApiResponseBusiness::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore business " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("merchant.business.deletePermanent", () -> merchantBusinessCommandService.deleteMerchantBusinessPermanent(
                pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantBusinessDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete business " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("merchant.business.restoreAll", () -> merchantBusinessCommandService.restoreAllMerchantBusiness(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantBusinessDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all businesses: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantBusinessDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("merchant.business.deleteAllPermanent", () -> merchantBusinessCommandService.deleteAllMerchantBusinessPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantBusinessDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all businesses: " + throwable.getMessage(), throwable)));
    }
}
