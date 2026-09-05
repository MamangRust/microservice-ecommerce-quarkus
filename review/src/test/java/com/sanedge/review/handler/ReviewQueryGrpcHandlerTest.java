package com.sanedge.review.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.review.domain.requests.FindAllReview;
import com.sanedge.review.service.ReviewQueryService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class ReviewQueryGrpcHandlerTest {
    @Mock ReviewQueryService reviewQueryService;
    private ReviewQueryGrpcHandler reviewQueryGrpcHandler;

    @BeforeEach
    void setUp() throws Exception {
        reviewQueryGrpcHandler = new ReviewQueryGrpcHandler();
        Field f = ReviewQueryGrpcHandler.class.getDeclaredField("reviewQueryService");
        f.setAccessible(true);
        f.set(reviewQueryGrpcHandler, reviewQueryService);
    }

    @Test
    void findAll_Success() {
        ApiResponsePagination<java.util.List<com.sanedge.review.domain.response.ReviewResponse>> resp =
                new ApiResponsePagination<>("success", "ok", java.util.List.of(), null);
        lenient().when(reviewQueryService.findAll(any(FindAllReview.class)))
                .thenReturn(Uni.createFrom().item(resp));
        var result = reviewQueryGrpcHandler.findAll(pb.review.ReviewQuery.FindAllReviewRequest.newBuilder().setPage(1).setPageSize(10).build()).await().indefinitely();
        assertThat(result).isNotNull();
    }

    @Test
    void findById_Success() {
        lenient().when(reviewQueryService.findById(any(Integer.class)))
                .thenAnswer(i -> Uni.createFrom().item(ApiResponse.success("ok", null)));
        var result = reviewQueryGrpcHandler.findById(pb.review.ReviewCommon.FindByIdReviewRequest.newBuilder().setId(1).build()).await().indefinitely();
        assertThat(result).isNotNull();
    }
}
