package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.ReviewDto;
import com.sanedge.gateway.service.ReviewService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOG = Logger.getLogger(ReviewServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("review")
    pb.review.MutinyReviewQueryServiceGrpc.MutinyReviewQueryServiceStub reviewQueryService;

    @GrpcClient("review")
    pb.review.MutinyReviewCommandServiceGrpc.MutinyReviewCommandServiceStub reviewCommandService;

    @Override
    public Uni<ReviewDto.ApiResponsePaginationReview> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("review.findAll", () -> reviewQueryService.findAll(
                pb.review.ReviewQuery.FindAllReviewRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ReviewDto.ApiResponsePaginationReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all reviews: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponsePaginationReview> findByProduct(int productId, int page, int size, String search) {
        return telemetryHelper.traceAndMetric("review.findByProduct", () -> reviewQueryService.findByProduct(
                pb.review.ReviewQuery.FindAllReviewProductRequest.newBuilder()
                        .setProductId(productId)
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ReviewDto.ApiResponsePaginationReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find reviews by product " + productId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponsePaginationReview> findByMerchant(int merchantId, int page, int size, String search) {
        return telemetryHelper.traceAndMetric("review.findByMerchant", () -> reviewQueryService.findByMerchant(
                pb.review.ReviewQuery.FindAllReviewMerchantRequest.newBuilder()
                        .setMerchantId(merchantId)
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ReviewDto.ApiResponsePaginationReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find reviews by merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponsePaginationReview> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("review.findByActive", () -> reviewQueryService.findByActive(
                pb.review.ReviewQuery.FindAllReviewRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ReviewDto.ApiResponsePaginationReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active reviews: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponsePaginationReview> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("review.findByTrashed", () -> reviewQueryService.findByTrashed(
                pb.review.ReviewQuery.FindAllReviewRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ReviewDto.ApiResponsePaginationReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed reviews: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponseReview> findById(int id) {
        return telemetryHelper.traceAndMetric("review.findById", () -> reviewQueryService.findById(
                pb.review.ReviewCommon.FindByIdReviewRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ReviewDto.ApiResponseReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find review " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponseReview> create(pb.review.ReviewCommand.CreateReviewRequest body) {
        return telemetryHelper.traceAndMetric("review.create", () -> reviewCommandService.create(body)
                .map(ReviewDto.ApiResponseReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create review: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponseReview> update(int id, pb.review.ReviewCommand.UpdateReviewRequest body) {
        pb.review.ReviewCommand.UpdateReviewRequest req = pb.review.ReviewCommand.UpdateReviewRequest.newBuilder(body)
                .setReviewId(id)
                .build();
        return telemetryHelper.traceAndMetric("review.update", () -> reviewCommandService.update(req)
                .map(ReviewDto.ApiResponseReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update review " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponseReview> delete(int id) {
        return telemetryHelper.traceAndMetric("review.delete", () -> reviewCommandService.trashedReview(
                pb.review.ReviewCommon.FindByIdReviewRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ReviewDto.ApiResponseReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete review " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.ApiResponseReview> restore(int id) {
        return telemetryHelper.traceAndMetric("review.restore", () -> reviewCommandService.restoreReview(
                pb.review.ReviewCommon.FindByIdReviewRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ReviewDto.ApiResponseReview::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore review " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("review.deletePermanent", () -> reviewCommandService.deleteReviewPermanent(
                pb.review.ReviewCommon.FindByIdReviewRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ReviewDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete review " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("review.restoreAll", () -> reviewCommandService.restoreAllReview(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(ReviewDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all reviews: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ReviewDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("review.deleteAllPermanent", () -> reviewCommandService.deleteAllReviewPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(ReviewDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all reviews: " + throwable.getMessage(), throwable)));
    }
}
