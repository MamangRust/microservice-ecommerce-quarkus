package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.ReviewDetailDto;
import com.sanedge.gateway.service.ReviewDetailService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReviewDetailServiceImpl implements ReviewDetailService {

    private static final Logger LOG = Logger.getLogger(ReviewDetailServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("review_detail")
    pb.review_detail.MutinyReviewDetailQueryServiceGrpc.MutinyReviewDetailQueryServiceStub reviewDetailQueryService;

    @GrpcClient("review_detail")
    pb.review_detail.MutinyReviewDetailCommandServiceGrpc.MutinyReviewDetailCommandServiceStub reviewDetailCommandService;

    @Override
    public Uni<ReviewDetailDto.ApiResponsePaginationDetail> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("review.detail.findAll", () -> reviewDetailQueryService.findAll(
                pb.review.ReviewQuery.FindAllReviewRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ReviewDetailDto.ApiResponsePaginationDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all details: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.ApiResponsePaginationDetail> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("review.detail.findByActive", () -> reviewDetailQueryService.findByActive(
                pb.review.ReviewQuery.FindAllReviewRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ReviewDetailDto.ApiResponsePaginationDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active details: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.ApiResponsePaginationDetail> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("review.detail.findByTrashed", () -> reviewDetailQueryService.findByTrashed(
                pb.review.ReviewQuery.FindAllReviewRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ReviewDetailDto.ApiResponsePaginationDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed details: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.ApiResponseDetail> findById(int id) {
        return telemetryHelper.traceAndMetric("review.detail.findById", () -> reviewDetailQueryService.findById(
                pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ReviewDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.ApiResponseDetail> create(pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest body) {
        return telemetryHelper.traceAndMetric("review.detail.create", () -> reviewDetailCommandService.create(body)
                .map(ReviewDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create detail: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.ApiResponseDetail> update(int id, pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest body) {
        pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest req = pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest.newBuilder(body)
                .setReviewDetailId(id)
                .build();
        return telemetryHelper.traceAndMetric("review.detail.update", () -> reviewDetailCommandService.update(req)
                .map(ReviewDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.ApiResponseDetail> delete(int id) {
        return telemetryHelper.traceAndMetric("review.detail.delete", () -> reviewDetailCommandService.trashedReviewDetail(
                pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ReviewDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.ApiResponseDetail> restore(int id) {
        return telemetryHelper.traceAndMetric("review.detail.restore", () -> reviewDetailCommandService.restoreReviewDetail(
                pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ReviewDetailDto.ApiResponseDetail::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("review.detail.deletePermanent", () -> reviewDetailCommandService.deleteReviewDetailPermanent(
                pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ReviewDetailDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete detail " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("review.detail.restoreAll", () -> reviewDetailCommandService.restoreAllReviewDetail(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(ReviewDetailDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all details: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDetailDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("review.detail.deleteAllPermanent", () -> reviewDetailCommandService.deleteAllReviewDetailPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(ReviewDetailDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all details: " + throwable.getMessage(), throwable)));
    }
}
