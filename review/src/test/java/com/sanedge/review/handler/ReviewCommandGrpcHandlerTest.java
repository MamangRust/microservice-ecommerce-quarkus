package com.sanedge.review.handler;

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
import com.sanedge.review.domain.requests.CreateReviewRequest;
import com.sanedge.review.domain.requests.UpdateReviewRequest;
import com.sanedge.review.domain.response.ReviewResponse;
import com.sanedge.review.domain.response.ReviewResponseDeleteAt;
import com.sanedge.review.service.ReviewCommandService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.review.ReviewCommon.FindByIdReviewRequest;

@ExtendWith(MockitoExtension.class)
class ReviewCommandGrpcHandlerTest {

        @Mock
        private ReviewCommandService reviewCommandService;

        private ReviewCommandGrpcHandler handler;

        @BeforeEach
        void setUp() throws Exception {
                handler = new ReviewCommandGrpcHandler();
                java.lang.reflect.Field field = ReviewCommandGrpcHandler.class.getDeclaredField("reviewCommandService");
                field.setAccessible(true);
                field.set(handler, reviewCommandService);
        }

        @Nested
        @DisplayName("Create Review Handler Tests")
        class CreateReviewHandlerTests {

                @Test
                @DisplayName("Should create review successfully via gRPC handler")
                void shouldCreateReviewSuccessfully() {

                        pb.review.ReviewCommand.CreateReviewRequest request = pb.review.ReviewCommand.CreateReviewRequest
                                        .newBuilder()
                                        .setUserId(1)
                                        .setProductId(100)
                                        .setName("John Doe")
                                        .setRating(5)
                                        .setComment("Great product!")
                                        .build();

                        ReviewResponse responseData = ReviewResponse.builder()
                                        .id(1)
                                        .userId(1)
                                        .productId(100)
                                        .name("John Doe")
                                        .rating(5)
                                        .comment("Great product!")
                                        .build();

                        ApiResponse<ReviewResponse> serviceResponse = ApiResponse
                                        .success("Review created successfully!", responseData);

                        when(reviewCommandService.create(any(CreateReviewRequest.class)))
                                        .thenReturn(Uni.createFrom().item(serviceResponse));

                        pb.review.ReviewCommon.ApiResponseReview response = handler.create(request)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getMessage()).isEqualTo("Review created successfully!");
                        assertThat(response.getData()).isNotNull();
                        assertThat(response.getData().getId()).isEqualTo(1);
                        assertThat(response.getData().getUserId()).isEqualTo(1);
                        assertThat(response.getData().getProductId()).isEqualTo(100);
                }

                @Test
                @DisplayName("Should handle failure when creating review via gRPC handler")
                void shouldHandleFailureWhenCreatingReview() {

                        pb.review.ReviewCommand.CreateReviewRequest request = pb.review.ReviewCommand.CreateReviewRequest
                                        .newBuilder()
                                        .setUserId(1)
                                        .setProductId(100)
                                        .setName("John Doe")
                                        .setRating(5)
                                        .setComment("Great product!")
                                        .build();

                        RuntimeException exception = new RuntimeException("Database error");
                        when(reviewCommandService.create(any(CreateReviewRequest.class)))
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
        @DisplayName("Update Review Handler Tests")
        class UpdateReviewHandlerTests {

                @Test
                @DisplayName("Should update review successfully via gRPC handler")
                void shouldUpdateReviewSuccessfully() {

                        pb.review.ReviewCommand.UpdateReviewRequest request = pb.review.ReviewCommand.UpdateReviewRequest
                                        .newBuilder()
                                        .setReviewId(1)
                                        .setName("Jane Doe")
                                        .setRating(4)
                                        .setComment("Updated comment")
                                        .build();

                        ReviewResponse responseData = ReviewResponse.builder()
                                        .id(1)
                                        .userId(1)
                                        .productId(100)
                                        .name("Jane Doe")
                                        .rating(4)
                                        .comment("Updated comment")
                                        .build();

                        ApiResponse<ReviewResponse> serviceResponse = ApiResponse
                                        .success("Review updated successfully!", responseData);

                        when(reviewCommandService.update(any(UpdateReviewRequest.class)))
                                        .thenReturn(Uni.createFrom().item(serviceResponse));

                        pb.review.ReviewCommon.ApiResponseReview response = handler.update(request)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData().getName()).isEqualTo("Jane Doe");
                        assertThat(response.getData().getRating()).isEqualTo(4);
                }

                @Test
                @DisplayName("Should return NOT_FOUND when review not found")
                void shouldReturnNotFoundWhenReviewNotFound() {

                        pb.review.ReviewCommand.UpdateReviewRequest request = pb.review.ReviewCommand.UpdateReviewRequest
                                        .newBuilder()
                                        .setReviewId(999)
                                        .setName("Jane Doe")
                                        .setRating(4)
                                        .setComment("Updated comment")
                                        .build();

                        ResourceNotFoundException exception = new ResourceNotFoundException("Review not found");
                        when(reviewCommandService.update(any(UpdateReviewRequest.class)))
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
        @DisplayName("Trash Review Handler Tests")
        class TrashReviewHandlerTests {

                @Test
                @DisplayName("Should trash review successfully via gRPC handler")
                void shouldTrashReviewSuccessfully() {

                        FindByIdReviewRequest request = FindByIdReviewRequest.newBuilder()
                                        .setId(1)
                                        .build();

                        ReviewResponseDeleteAt responseData = ReviewResponseDeleteAt.builder()
                                        .id(1)
                                        .userId(1)
                                        .productId(100)
                                        .name("John Doe")
                                        .rating(5)
                                        .comment("Great product!")
                                        .deletedAt("2024-01-01 00:00:00")
                                        .build();

                        ApiResponse<ReviewResponseDeleteAt> serviceResponse = ApiResponse
                                        .success("Review trashed successfully!", responseData);

                        when(reviewCommandService.trash(1))
                                        .thenReturn(Uni.createFrom().item(serviceResponse));

                        pb.review.ReviewCommon.ApiResponseReviewDeleteAt response = handler.trashedReview(request)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData()).isNotNull();
                        assertThat(response.getData().getId()).isEqualTo(1);
                }

                @Test
                @DisplayName("Should return NOT_FOUND when trashing non-existent review")
                void shouldReturnNotFoundWhenTrashingNonExistentReview() {

                        FindByIdReviewRequest request = FindByIdReviewRequest.newBuilder()
                                        .setId(999)
                                        .build();

                        ResourceNotFoundException exception = new ResourceNotFoundException("Review not found");
                        when(reviewCommandService.trash(999))
                                        .thenReturn(Uni.createFrom().failure(exception));

                        try {
                                handler.trashedReview(request).await().indefinitely();
                                assertThat(false).isTrue();
                        } catch (StatusRuntimeException e) {
                                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                        }
                }
        }

        @Nested
        @DisplayName("Restore Review Handler Tests")
        class RestoreReviewHandlerTests {

                @Test
                @DisplayName("Should restore review successfully via gRPC handler")
                void shouldRestoreReviewSuccessfully() {

                        FindByIdReviewRequest request = FindByIdReviewRequest.newBuilder()
                                        .setId(1)
                                        .build();

                        ReviewResponseDeleteAt responseData = ReviewResponseDeleteAt.builder()
                                        .id(1)
                                        .userId(1)
                                        .productId(100)
                                        .name("John Doe")
                                        .rating(5)
                                        .comment("Great product!")
                                        .build();

                        ApiResponse<ReviewResponseDeleteAt> serviceResponse = ApiResponse
                                        .success("Review restored successfully!", responseData);

                        when(reviewCommandService.restore(1))
                                        .thenReturn(Uni.createFrom().item(serviceResponse));

                        pb.review.ReviewCommon.ApiResponseReviewDeleteAt response = handler.restoreReview(request)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData()).isNotNull();
                }
        }

        @Nested
        @DisplayName("Delete Review Handler Tests")
        class DeleteReviewHandlerTests {

                @Test
                @DisplayName("Should delete review permanently via gRPC handler")
                void shouldDeleteReviewPermanently() {

                        FindByIdReviewRequest request = FindByIdReviewRequest.newBuilder()
                                        .setId(1)
                                        .build();

                        ApiResponse<Void> serviceResponse = ApiResponse.success("Review permanently deleted!");

                        when(reviewCommandService.delete(1))
                                        .thenReturn(Uni.createFrom().item(serviceResponse));

                        pb.review.ReviewCommon.ApiResponseReviewDelete response = handler.deleteReviewPermanent(request)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getMessage()).isEqualTo("Review permanently deleted!");
                }
        }

        @Nested
        @DisplayName("Restore All & Delete All Handler Tests")
        class RestoreAllDeleteAllHandlerTests {

                @Test
                @DisplayName("Should restore all reviews via gRPC handler")
                void shouldRestoreAllReviews() {

                        com.google.protobuf.Empty request = com.google.protobuf.Empty.getDefaultInstance();

                        ApiResponse<Void> serviceResponse = ApiResponse.success("All reviews restored successfully!");

                        when(reviewCommandService.restoreAll())
                                        .thenReturn(Uni.createFrom().item(serviceResponse));

                        pb.review.ReviewCommon.ApiResponseReviewAll response = handler.restoreAllReview(request)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getMessage()).isEqualTo("All reviews restored successfully!");
                }

                @Test
                @DisplayName("Should delete all reviews permanently via gRPC handler")
                void shouldDeleteAllReviewsPermanently() {

                        com.google.protobuf.Empty request = com.google.protobuf.Empty.getDefaultInstance();

                        ApiResponse<Void> serviceResponse = ApiResponse.success("All reviews permanently deleted!");

                        when(reviewCommandService.deleteAll())
                                        .thenReturn(Uni.createFrom().item(serviceResponse));

                        pb.review.ReviewCommon.ApiResponseReviewAll response = handler.deleteAllReviewPermanent(request)
                                        .await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getMessage()).isEqualTo("All reviews permanently deleted!");
                }
        }
}
