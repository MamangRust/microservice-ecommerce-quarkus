package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class ReviewDetailServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.review_detail.MutinyReviewDetailQueryServiceGrpc.MutinyReviewDetailQueryServiceStub reviewDetailQueryService;
    @Mock
    private pb.review_detail.MutinyReviewDetailCommandServiceGrpc.MutinyReviewDetailCommandServiceStub reviewDetailCommandService;

    private ReviewDetailServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = ReviewDetailServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Uni<?>> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        service = new ReviewDetailServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("reviewDetailQueryService", reviewDetailQueryService);
        inject("reviewDetailCommandService", reviewDetailCommandService);
    }

    @Test
    void findById_PropagatesDetailResponse() {
        pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail proto = pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(reviewDetailQueryService.findById(any(pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesDetailResponse() {
        pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail proto = pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest req = pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest.newBuilder().build();
        lenient().when(reviewDetailCommandService.create(any(pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt proto = pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(reviewDetailCommandService.trashedReviewDetail(any(pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt proto = pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(reviewDetailCommandService.restoreReviewDetail(any(pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
