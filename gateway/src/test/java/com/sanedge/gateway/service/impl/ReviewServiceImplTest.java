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
class ReviewServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.review.MutinyReviewQueryServiceGrpc.MutinyReviewQueryServiceStub reviewQueryService;
    @Mock
    private pb.review.MutinyReviewCommandServiceGrpc.MutinyReviewCommandServiceStub reviewCommandService;

    private ReviewServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = ReviewServiceImpl.class.getDeclaredField(name);
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
        service = new ReviewServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("reviewQueryService", reviewQueryService);
        inject("reviewCommandService", reviewCommandService);
    }

    @Test
    void findById_PropagatesReviewResponse() {
        pb.review.ReviewCommon.ApiResponseReview proto = pb.review.ReviewCommon.ApiResponseReview.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(reviewQueryService.findById(any(pb.review.ReviewCommon.FindByIdReviewRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesReviewResponse() {
        pb.review.ReviewCommon.ApiResponseReview proto = pb.review.ReviewCommon.ApiResponseReview.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.review.ReviewCommand.CreateReviewRequest req = pb.review.ReviewCommand.CreateReviewRequest.newBuilder().build();
        lenient().when(reviewCommandService.create(any(pb.review.ReviewCommand.CreateReviewRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.review.ReviewCommon.ApiResponseReviewDeleteAt proto = pb.review.ReviewCommon.ApiResponseReviewDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(reviewCommandService.trashedReview(any(pb.review.ReviewCommon.FindByIdReviewRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }

    @Test
    void deletePermanent_PropagatesSimpleResponse() {
        pb.review.ReviewCommon.ApiResponseReviewDelete proto = pb.review.ReviewCommon.ApiResponseReviewDelete.newBuilder()
                .setStatus("success").setMessage("deleted").build();
        lenient().when(reviewCommandService.deleteReviewPermanent(any(pb.review.ReviewCommon.FindByIdReviewRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deletePermanent(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.review.ReviewCommon.ApiResponseReviewDeleteAt proto = pb.review.ReviewCommon.ApiResponseReviewDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(reviewCommandService.restoreReview(any(pb.review.ReviewCommon.FindByIdReviewRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
