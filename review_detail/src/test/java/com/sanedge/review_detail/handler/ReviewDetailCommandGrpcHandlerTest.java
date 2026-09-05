package com.sanedge.review_detail.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.review_detail.domain.response.ReviewDetailResponse;
import com.sanedge.review_detail.domain.response.ReviewDetailResponseDeleteAt;
import com.sanedge.review_detail.service.ReviewDetailService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.review_detail.ReviewDetailCommon.FindByIdReviewDetailRequest;

@ExtendWith(MockitoExtension.class)
class ReviewDetailCommandGrpcHandlerTest {

    @Mock
    private ReviewDetailService reviewDetailService;

    private ReviewDetailCommandGrpcHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new ReviewDetailCommandGrpcHandler();
        java.lang.reflect.Field field = ReviewDetailCommandGrpcHandler.class.getDeclaredField("reviewDetailService");
        field.setAccessible(true);
        field.set(handler, reviewDetailService);
    }

    @Nested
    @DisplayName("Create ReviewDetail Handler Tests")
    class CreateReviewDetailHandlerTests {

        @Test
        @DisplayName("Should create review detail successfully via gRPC handler")
        void shouldCreateReviewDetailSuccessfully() {

            pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest request = pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest
                    .newBuilder()
                    .setReviewId(1)
                    .setType("image")
                    .setUrl("https://example.com/image.jpg")
                    .setCaption("Product image")
                    .build();

            ReviewDetailResponse responseData = ReviewDetailResponse.builder()
                    .id(1)
                    .reviewId(1)
                    .type("image")
                    .url("https://example.com/image.jpg")
                    .caption("Product image")
                    .build();

            ApiResponse<java.util.List<ReviewDetailResponse>> serviceResponse = ApiResponse.success(
                    "Review details created successfully!", java.util.List.of(responseData));

            when(reviewDetailService.create(any()))
                    .thenReturn(Uni.createFrom().item(serviceResponse));

            pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail response = handler.create(request)
                    .await().indefinitely();

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getId()).isEqualTo(1);
            assertThat(response.getData().getReviewId()).isEqualTo(1);
            assertThat(response.getData().getType()).isEqualTo("image");
            assertThat(response.getData().getUrl()).isEqualTo("https://example.com/image.jpg");
        }

        @Test
        @DisplayName("Should handle failure when creating review detail via gRPC handler")
        void shouldHandleFailureWhenCreatingReviewDetail() {

            pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest request = pb.review_detail.ReviewDetailCommand.CreateReviewDetailRequest
                    .newBuilder()
                    .setReviewId(1)
                    .setType("image")
                    .setUrl("https://example.com/image.jpg")
                    .setCaption("Product image")
                    .build();

            RuntimeException exception = new RuntimeException("Database error");
            when(reviewDetailService.create(any()))
                    .thenReturn(Uni.createFrom().failure(exception));

            try {
                handler.create(request).await().indefinitely();
                assertThat(false).isTrue();
            } catch (StatusRuntimeException e) {
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
            }
        }
    }

    @Nested
    @DisplayName("Update ReviewDetail Handler Tests")
    class UpdateReviewDetailHandlerTests {

        @Test
        @DisplayName("Should update review detail successfully via gRPC handler")
        void shouldUpdateReviewDetailSuccessfully() {

            pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest request = pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest
                    .newBuilder()
                    .setReviewDetailId(1)
                    .setType("video")
                    .setUrl("https://example.com/video.mp4")
                    .setCaption("Updated video")
                    .build();

            ReviewDetailResponse responseData = ReviewDetailResponse.builder()
                    .id(1)
                    .reviewId(1)
                    .type("video")
                    .url("https://example.com/video.mp4")
                    .caption("Updated video")
                    .build();

            ApiResponse<java.util.List<ReviewDetailResponse>> serviceResponse = ApiResponse.success(
                    "Review details updated successfully!", java.util.List.of(responseData));

            when(reviewDetailService.update(any()))
                    .thenReturn(Uni.createFrom().item(serviceResponse));

            pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetail response = handler.update(request)
                    .await().indefinitely();

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("success");
            assertThat(response.getData().getType()).isEqualTo("video");
            assertThat(response.getData().getUrl()).isEqualTo("https://example.com/video.mp4");
        }

        @Test
        @DisplayName("Should return NOT_FOUND when review detail not found during update")
        void shouldReturnNotFoundWhenReviewDetailNotFound() {

            pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest request = pb.review_detail.ReviewDetailCommand.UpdateReviewDetailRequest
                    .newBuilder()
                    .setReviewDetailId(999)
                    .setType("video")
                    .setUrl("https://example.com/video.mp4")
                    .setCaption("Updated video")
                    .build();

            ResourceNotFoundException exception = new ResourceNotFoundException("Review detail not found");
            when(reviewDetailService.update(any()))
                    .thenReturn(Uni.createFrom().failure(exception));

            try {
                handler.update(request).await().indefinitely();
                assertThat(false).isTrue();
            } catch (StatusRuntimeException e) {
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("Trash ReviewDetail Handler Tests")
    class TrashReviewDetailHandlerTests {

        @Test
        @DisplayName("Should trash review detail successfully via gRPC handler")
        void shouldTrashReviewDetailSuccessfully() {

            FindByIdReviewDetailRequest request = FindByIdReviewDetailRequest.newBuilder()
                    .setId(1)
                    .build();

            ReviewDetailResponseDeleteAt responseData = ReviewDetailResponseDeleteAt.builder()
                    .id(1)
                    .reviewId(1)
                    .type("image")
                    .url("https://example.com/image.jpg")
                    .caption("Product image")
                    .deletedAt("2024-01-01 00:00:00")
                    .build();

            ApiResponse<ReviewDetailResponseDeleteAt> serviceResponse = ApiResponse.success(
                    "Review detail trashed successfully!", responseData);

            when(reviewDetailService.trash(1))
                    .thenReturn(Uni.createFrom().item(serviceResponse));

            pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt response = handler
                    .trashedReviewDetail(request).await().indefinitely();

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getId()).isEqualTo(1);
            assertThat(response.getData().getDeletedAt().getValue()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return NOT_FOUND when trashing non-existent review detail")
        void shouldReturnNotFoundWhenTrashingNonExistentReviewDetail() {

            FindByIdReviewDetailRequest request = FindByIdReviewDetailRequest.newBuilder()
                    .setId(999)
                    .build();

            ResourceNotFoundException exception = new ResourceNotFoundException(
                    "Review detail not found or already trashed");
            when(reviewDetailService.trash(999))
                    .thenReturn(Uni.createFrom().failure(exception));

            try {
                handler.trashedReviewDetail(request).await().indefinitely();
                assertThat(false).isTrue();
            } catch (StatusRuntimeException e) {
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("Restore ReviewDetail Handler Tests")
    class RestoreReviewDetailHandlerTests {

        @Test
        @DisplayName("Should restore review detail successfully via gRPC handler")
        void shouldRestoreReviewDetailSuccessfully() {

            FindByIdReviewDetailRequest request = FindByIdReviewDetailRequest.newBuilder()
                    .setId(1)
                    .build();

            ReviewDetailResponseDeleteAt responseData = ReviewDetailResponseDeleteAt.builder()
                    .id(1)
                    .reviewId(1)
                    .type("image")
                    .url("https://example.com/image.jpg")
                    .caption("Product image")
                    .build();

            ApiResponse<ReviewDetailResponseDeleteAt> serviceResponse = ApiResponse.success(
                    "Review detail restored successfully!", responseData);

            when(reviewDetailService.restore(1))
                    .thenReturn(Uni.createFrom().item(serviceResponse));

            pb.review_detail.ReviewDetailCommon.ApiResponseReviewDetailDeleteAt response = handler
                    .restoreReviewDetail(request).await().indefinitely();

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("success");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return NOT_FOUND when restoring non-existent review detail")
        void shouldReturnNotFoundWhenRestoringNonExistentReviewDetail() {

            FindByIdReviewDetailRequest request = FindByIdReviewDetailRequest.newBuilder()
                    .setId(999)
                    .build();

            ResourceNotFoundException exception = new ResourceNotFoundException(
                    "Review detail not found or not trashed");
            when(reviewDetailService.restore(999))
                    .thenReturn(Uni.createFrom().failure(exception));

            try {
                handler.restoreReviewDetail(request).await().indefinitely();
                assertThat(false).isTrue();
            } catch (StatusRuntimeException e) {
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("Delete ReviewDetail Handler Tests")
    class DeleteReviewDetailHandlerTests {

        @Test
        @DisplayName("Should delete review detail permanently via gRPC handler")
        void shouldDeleteReviewDetailPermanently() {

            FindByIdReviewDetailRequest request = FindByIdReviewDetailRequest.newBuilder()
                    .setId(1)
                    .build();

            ApiResponse<Void> serviceResponse = ApiResponse.success("Review detail permanently deleted");

            when(reviewDetailService.delete(1))
                    .thenReturn(Uni.createFrom().item(serviceResponse));

            pb.review.ReviewCommon.ApiResponseReviewDelete response = handler.deleteReviewDetailPermanent(request)
                    .await().indefinitely();

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("success");
            assertThat(response.getMessage()).isEqualTo("Review detail permanently deleted");
        }

        @Test
        @DisplayName("Should return NOT_FOUND when deleting non-existent review detail")
        void shouldReturnNotFoundWhenDeletingNonExistentReviewDetail() {

            FindByIdReviewDetailRequest request = FindByIdReviewDetailRequest.newBuilder()
                    .setId(999)
                    .build();

            ResourceNotFoundException exception = new ResourceNotFoundException("Review detail not found");
            when(reviewDetailService.delete(999))
                    .thenReturn(Uni.createFrom().failure(exception));

            try {
                handler.deleteReviewDetailPermanent(request).await().indefinitely();
                assertThat(false).isTrue();
            } catch (StatusRuntimeException e) {
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("Restore All & Delete All Handler Tests")
    class RestoreAllDeleteAllHandlerTests {

        @Test
        @DisplayName("Should restore all review details via gRPC handler")
        void shouldRestoreAllReviewDetails() {

            com.google.protobuf.Empty request = com.google.protobuf.Empty.getDefaultInstance();

            ApiResponse<Void> serviceResponse = ApiResponse.success("All review details restored successfully");

            when(reviewDetailService.restoreAll())
                    .thenReturn(Uni.createFrom().item(serviceResponse));

            pb.review.ReviewCommon.ApiResponseReviewAll response = handler.restoreAllReviewDetail(request).await()
                    .indefinitely();

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("success");
            assertThat(response.getMessage()).isEqualTo("All review details restored successfully");
        }

        @Test
        @DisplayName("Should delete all review details permanently via gRPC handler")
        void shouldDeleteAllReviewDetailsPermanently() {

            com.google.protobuf.Empty request = com.google.protobuf.Empty.getDefaultInstance();

            ApiResponse<Void> serviceResponse = ApiResponse.success("All review details permanently deleted");

            when(reviewDetailService.deleteAll())
                    .thenReturn(Uni.createFrom().item(serviceResponse));

            pb.review.ReviewCommon.ApiResponseReviewAll response = handler.deleteAllReviewDetailPermanent(request)
                    .await().indefinitely();

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("success");
            assertThat(response.getMessage()).isEqualTo("All review details permanently deleted");
        }

        @Test
        @DisplayName("Should handle failure when restoring all review details")
        void shouldHandleFailureWhenRestoringAllReviewDetails() {

            com.google.protobuf.Empty request = com.google.protobuf.Empty.getDefaultInstance();

            RuntimeException exception = new RuntimeException("Database error");
            when(reviewDetailService.restoreAll())
                    .thenReturn(Uni.createFrom().failure(exception));

            try {
                handler.restoreAllReviewDetail(request).await().indefinitely();
                assertThat(false).isTrue();
            } catch (StatusRuntimeException e) {
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
            }
        }

        @Test
        @DisplayName("Should handle failure when deleting all review details")
        void shouldHandleFailureWhenDeletingAllReviewDetails() {

            com.google.protobuf.Empty request = com.google.protobuf.Empty.getDefaultInstance();

            RuntimeException exception = new RuntimeException("Database error");
            when(reviewDetailService.deleteAll())
                    .thenReturn(Uni.createFrom().failure(exception));

            try {
                handler.deleteAllReviewDetailPermanent(request).await().indefinitely();
                assertThat(false).isTrue();
            } catch (StatusRuntimeException e) {
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
            }
        }
    }
}
