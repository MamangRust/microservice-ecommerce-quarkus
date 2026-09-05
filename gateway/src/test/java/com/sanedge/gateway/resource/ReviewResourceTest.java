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

import com.sanedge.gateway.dto.ReviewDto;
import com.sanedge.gateway.service.ReviewService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class ReviewResourceTest {
    @Mock
    ReviewService reviewService;
    private ReviewResource reviewResource;

    @BeforeEach
    void setUp() throws Exception {
        reviewResource = new ReviewResource();
        Field f = ReviewResource.class.getDeclaredField("reviewService");
        f.setAccessible(true);
        f.set(reviewResource, reviewService);
    }

    private ReviewDto.ReviewResponse mk(int id) {
        return new ReviewDto.ReviewResponse(id, 1, 1, "name", "comment", 5, "", "");
    }

    @Test
    void findAll_Success() {
        ReviewDto.ApiResponsePaginationReview dto = new ReviewDto.ApiResponsePaginationReview(
                List.of(mk(1)), "success", "ok");
        lenient().when(reviewService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByProduct_Success() {
        ReviewDto.ApiResponsePaginationReview dto = new ReviewDto.ApiResponsePaginationReview(
                List.of(), "success", "ok");
        lenient().when(reviewService.findByProduct(anyInt(), anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.findByProduct(1, 1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByMerchant_Success() {
        ReviewDto.ApiResponsePaginationReview dto = new ReviewDto.ApiResponsePaginationReview(
                List.of(), "success", "ok");
        lenient().when(reviewService.findByMerchant(anyInt(), anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.findByMerchant(1, 1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        ReviewDto.ApiResponsePaginationReview dto = new ReviewDto.ApiResponsePaginationReview(
                List.of(), "success", "ok");
        lenient().when(reviewService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        ReviewDto.ApiResponsePaginationReview dto = new ReviewDto.ApiResponsePaginationReview(
                List.of(), "success", "ok");
        lenient().when(reviewService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        ReviewDto.ApiResponseReview dto = new ReviewDto.ApiResponseReview(mk(1), "success", "ok");
        lenient().when(reviewService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        ReviewDto.ApiResponseReview dto = new ReviewDto.ApiResponseReview(mk(1), "success", "ok");
        lenient().when(reviewService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.review.ReviewCommand.CreateReviewRequest req = pb.review.ReviewCommand.CreateReviewRequest.newBuilder().build();
        Response r = reviewResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void update_Success() {
        ReviewDto.ApiResponseReview dto = new ReviewDto.ApiResponseReview(mk(1), "success", "ok");
        lenient().when(reviewService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.review.ReviewCommand.UpdateReviewRequest req = pb.review.ReviewCommand.UpdateReviewRequest.newBuilder().build();
        Response r = reviewResource.update(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_Success() {
        ReviewDto.ApiResponseReview dto = new ReviewDto.ApiResponseReview(mk(1), "success", "ok");
        lenient().when(reviewService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.delete(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restore_Success() {
        ReviewDto.ApiResponseReview dto = new ReviewDto.ApiResponseReview(mk(1), "success", "ok");
        lenient().when(reviewService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.restore(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deletePermanent_Success() {
        ReviewDto.SimpleResponse dto = new ReviewDto.SimpleResponse("success", "ok");
        lenient().when(reviewService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.deletePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAll_Success() {
        ReviewDto.SimpleResponse dto = new ReviewDto.SimpleResponse("success", "ok");
        lenient().when(reviewService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.restoreAll().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllPermanent_Success() {
        ReviewDto.SimpleResponse dto = new ReviewDto.SimpleResponse("success", "ok");
        lenient().when(reviewService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = reviewResource.deleteAllPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
