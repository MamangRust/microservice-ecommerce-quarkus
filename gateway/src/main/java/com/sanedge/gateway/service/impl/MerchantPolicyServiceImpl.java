package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantPolicyDto;
import com.sanedge.gateway.service.MerchantPolicyService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantPolicyServiceImpl implements MerchantPolicyService {

    private static final Logger LOG = Logger.getLogger(MerchantPolicyServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant_policy")
    pb.merchant_policy.MutinyMerchantPolicyQueryServiceGrpc.MutinyMerchantPolicyQueryServiceStub merchantPolicyQueryService;

    @GrpcClient("merchant_policy")
    pb.merchant_policy.MutinyMerchantPolicyCommandServiceGrpc.MutinyMerchantPolicyCommandServiceStub merchantPolicyCommandService;

    @Override
    public Uni<MerchantPolicyDto.ApiResponsePaginationPolicy> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.policy.findAll", () -> merchantPolicyQueryService.findAll(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantPolicyDto.ApiResponsePaginationPolicy::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all policies: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.ApiResponsePaginationPolicy> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.policy.findByActive", () -> merchantPolicyQueryService.findByActive(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantPolicyDto.ApiResponsePaginationPolicy::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active policies: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.ApiResponsePaginationPolicy> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.policy.findByTrashed", () -> merchantPolicyQueryService.findByTrashed(
                pb.merchant.MerchantQuery.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(MerchantPolicyDto.ApiResponsePaginationPolicy::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed policies: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.ApiResponsePolicy> findById(int id) {
        return telemetryHelper.traceAndMetric("merchant.policy.findById", () -> merchantPolicyQueryService.findById(
                pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantPolicyDto.ApiResponsePolicy::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find policy " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.ApiResponsePolicy> create(pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest body) {
        return telemetryHelper.traceAndMetric("merchant.policy.create", () -> merchantPolicyCommandService.create(body)
                .map(MerchantPolicyDto.ApiResponsePolicy::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create policy: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.ApiResponsePolicy> update(int id, pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest body) {
        pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest req = pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest.newBuilder(body)
                .setMerchantPolicyId(id)
                .build();
        return telemetryHelper.traceAndMetric("merchant.policy.update", () -> merchantPolicyCommandService.update(req)
                .map(MerchantPolicyDto.ApiResponsePolicy::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update policy " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.ApiResponsePolicy> delete(int id) {
        return telemetryHelper.traceAndMetric("merchant.policy.delete", () -> merchantPolicyCommandService.trashedMerchantPolicies(
                pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantPolicyDto.ApiResponsePolicy::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete policy " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.ApiResponsePolicy> restore(int id) {
        return telemetryHelper.traceAndMetric("merchant.policy.restore", () -> merchantPolicyCommandService.restoreMerchantPolicies(
                pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantPolicyDto.ApiResponsePolicy::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore policy " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("merchant.policy.deletePermanent", () -> merchantPolicyCommandService.deleteMerchantPoliciesPermanent(
                pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(MerchantPolicyDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete policy " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("merchant.policy.restoreAll", () -> merchantPolicyCommandService.restoreAllMerchantPolicies(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantPolicyDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all policies: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<MerchantPolicyDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("merchant.policy.deleteAllPermanent", () -> merchantPolicyCommandService.deleteAllMerchantPoliciesPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(MerchantPolicyDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all policies: " + throwable.getMessage(), throwable)));
    }
}
