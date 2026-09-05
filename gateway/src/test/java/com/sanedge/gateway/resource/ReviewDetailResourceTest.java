package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.ReviewDetailDto;
import com.sanedge.gateway.service.ReviewDetailService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class ReviewDetailResourceTest {
    @Mock
    ReviewDetailService reviewDetailService;
    private ReviewDetailResource reviewDetailResource;

    @BeforeEach
    void setUp() throws Exception {
        reviewDetailResource = new ReviewDetailResource();
        Field f = ReviewDetailResource.class.getDeclaredField("reviewDetailService");
        f.setAccessible(true);
        f.set(reviewDetailResource, reviewDetailService);
    }

    private ReviewDetailDto.ReviewDetailsResponse mk(int id) {
        return new ReviewDetailDto.ReviewDetailsResponse(id, 1, "image", "", "", "", "");
    }

    @Test
    void findAll_Success() {

        ReviewDetailDto.ApiResponsePaginationDetail dto = new ReviewDetailDto.ApiResponsePaginationDetail(
                "success", "ok", List.of(mk(1)));
        lenient().when(reviewDetailService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        ReviewDetailDto.ApiResponsePaginationDetail dto = new ReviewDetailDto.ApiResponsePaginationDetail(
                "success", "ok", List.of());
        lenient().when(reviewDetailService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        ReviewDetailDto.ApiResponsePaginationDetail dto = new ReviewDetailDto.ApiResponsePaginationDetail(
                "success", "ok", List.of());
        lenient().when(reviewDetailService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {

        ReviewDetailDto.ApiResponseDetail dto = new ReviewDetailDto.ApiResponseDetail(
                "success", "ok", mk(1));
        lenient().when(reviewDetailService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createReviewDetail_Success_Returns201() {
        ReviewDetailDto.ApiResponseDetail dto = new ReviewDetailDto.ApiResponseDetail(
                "success", "ok", mk(1));
        lenient().when(reviewDetailService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest req = pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest.newBuilder().build();
        Response r = reviewDetailResource.createReviewDetail(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateReviewDetail_Success() {
        ReviewDetailDto.ApiResponseDetail dto = new ReviewDetailDto.ApiResponseDetail("success", "ok", mk(1));
        lenient().when(reviewDetailService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest req = pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest.newBuilder().build();
        Response r = reviewDetailResource.updateReviewDetail(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteReviewDetail_Success() {
        ReviewDetailDto.ApiResponseDetail dto = new ReviewDetailDto.ApiResponseDetail("success", "ok", mk(1));
        lenient().when(reviewDetailService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.deleteReviewDetail(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreReviewDetail_Success() {
        ReviewDetailDto.ApiResponseDetail dto = new ReviewDetailDto.ApiResponseDetail("success", "ok", mk(1));
        lenient().when(reviewDetailService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.restoreReviewDetail(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteReviewDetailPermanent_Success() {
        ReviewDetailDto.SimpleResponse dto = new ReviewDetailDto.SimpleResponse("success", "ok");
        lenient().when(reviewDetailService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.deleteReviewDetailPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllReviewDetails_Success() {
        ReviewDetailDto.SimpleResponse dto = new ReviewDetailDto.SimpleResponse("success", "ok");
        lenient().when(reviewDetailService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.restoreAllReviewDetails().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllReviewDetailsPermanent_Success() {
        ReviewDetailDto.SimpleResponse dto = new ReviewDetailDto.SimpleResponse("success", "ok");
        lenient().when(reviewDetailService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewDetailResource.deleteAllReviewDetailsPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
