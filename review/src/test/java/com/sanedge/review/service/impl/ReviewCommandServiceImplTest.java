package com.sanedge.review.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.review.domain.requests.CreateReviewRequest;
import com.sanedge.review.domain.requests.UpdateReviewRequest;
import com.sanedge.review.domain.response.ReviewResponse;
import com.sanedge.review.domain.response.ReviewResponseDeleteAt;
import com.sanedge.review.entity.Review;
import com.sanedge.review.repository.ReviewCommandRepository;
import com.sanedge.review.repository.ReviewQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceImplTest {

    @Mock
    private ReviewQueryRepository reviewQueryRepository;

    @Mock
    private ReviewCommandRepository reviewCommandRepository;

    @Mock
    private Validator validator;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private ReviewCommandServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewCommandServiceImpl(
                reviewQueryRepository,
                reviewCommandRepository,
                validator,
                redisService,
                tracingMetrics);
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                        .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
    }

    private void setReviewId(Review review, Long id) {
        try {
            Field f = review.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(review, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Review createReviewEntity(Long id, int userId, int productId, String name, String comment, int rating) {
        Review review = new Review();
        setReviewId(review, id);
        review.setUserId(userId);
        review.setProductId(productId);
        review.setName(name);
        review.setComment(comment);
        review.setRating(rating);
        review.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        review.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return review;
    }

    @Nested
    @DisplayName("Create Review Tests")
    class CreateReviewTests {

        @Test
        @DisplayName("Should create review successfully")
        void shouldCreateReviewSuccessfully() {
            CreateReviewRequest request = new CreateReviewRequest();
            request.setUserId(1);
            request.setProductId(100);
            request.setName("John Doe");
            request.setRating(5);
            request.setComment("Great product!");

            Review savedReview = createReviewEntity(1L, 1, 100, "John Doe", "Great product!", 5);

            when(validator.validate(request)).thenReturn(new HashSet<>());
            when(reviewCommandRepository.persist(any(Review.class)))
                    .thenReturn(Uni.createFrom().item(savedReview));
            when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<ReviewResponse> response = reviewService.create(request).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Review created successfully!");
            assertThat(response.data()).isNotNull();
            assertThat(response.data().getUserId()).isEqualTo(1);
            assertThat(response.data().getProductId()).isEqualTo(100);
            assertThat(response.data().getRating()).isEqualTo(5);
            verify(reviewCommandRepository).persist(any(Review.class));
        }

        @Test
        @DisplayName("Should fail create review when validation fails")
        void shouldFailCreateReviewWhenValidationFails() {
            CreateReviewRequest request = new CreateReviewRequest();
            request.setUserId(1);
            request.setProductId(100);
            request.setName("John Doe");
            request.setRating(0);
            request.setComment("");

            Set<ConstraintViolation<CreateReviewRequest>> violations = new HashSet<>();
            ConstraintViolation<CreateReviewRequest> v = org.mockito.Mockito.mock(ConstraintViolation.class);
            org.mockito.Mockito.when(v.getPropertyPath())
                    .thenReturn(org.mockito.Mockito.mock(jakarta.validation.Path.class));
            org.mockito.Mockito.when(v.getMessage()).thenReturn("must not be blank");
            violations.add(v);
            when(validator.validate(request)).thenReturn(violations);

            assertThatThrownBy(() -> reviewService.create(request).await().indefinitely())
                    .isInstanceOf(jakarta.validation.ConstraintViolationException.class)
                    .hasMessageContaining("Validation failed");
        }
    }

    @Nested
    @DisplayName("Update Review Tests")
    class UpdateReviewTests {

        @Test
        @DisplayName("Should update review successfully")
        void shouldUpdateReviewSuccessfully() {
            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(1);
            request.setName("Jane Doe");
            request.setRating(4);
            request.setComment("Updated comment");

            Review existingReview = createReviewEntity(1L, 1, 100, "John Doe", "Great product!", 5);
            Review updatedReview = createReviewEntity(1L, 1, 100, "Jane Doe", "Updated comment", 4);

            when(validator.validate(request)).thenReturn(new HashSet<>());
            when(reviewQueryRepository.findReviewById(1L))
                    .thenReturn(Uni.createFrom().item(Optional.of(existingReview)));
            when(reviewCommandRepository.persist(any(Review.class)))
                    .thenReturn(Uni.createFrom().item(updatedReview));
            when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<ReviewResponse> response = reviewService.update(request).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Review updated successfully!");
            assertThat(response.data().getName()).isEqualTo("Jane Doe");
            assertThat(response.data().getRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should fail update when review not found")
        void shouldFailUpdateWhenReviewNotFound() {
            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(999);
            request.setName("Jane Doe");
            request.setRating(4);
            request.setComment("Updated comment");

            when(validator.validate(request)).thenReturn(new HashSet<>());
            when(reviewQueryRepository.findReviewById(999L))
                    .thenReturn(Uni.createFrom().item(Optional.empty()));

            assertThatThrownBy(() -> reviewService.update(request).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Review not found");
        }
    }

    @Nested
    @DisplayName("Trash Review Tests")
    class TrashReviewTests {

        @Test
        @DisplayName("Should trash review successfully")
        void shouldTrashReviewSuccessfully() {
            Review trashedReview = createReviewEntity(1L, 1, 100, "John Doe", "Great product!", 5);
            trashedReview.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

            when(reviewCommandRepository.trash(1L)).thenReturn(Uni.createFrom().item(trashedReview));
            when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<ReviewResponseDeleteAt> response = reviewService.trash(1).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Review trashed successfully!");
            assertThat(response.data()).isNotNull();
        }

        @Test
        @DisplayName("Should fail trash when review not found")
        void shouldFailTrashWhenReviewNotFound() {
            when(reviewCommandRepository.trash(999L)).thenReturn(Uni.createFrom().nullItem());

            assertThatThrownBy(() -> reviewService.trash(999).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Review not found");
        }
    }

    @Nested
    @DisplayName("Restore Review Tests")
    class RestoreReviewTests {

        @Test
        @DisplayName("Should restore review successfully")
        void shouldRestoreReviewSuccessfully() {
            Review restoredReview = createReviewEntity(1L, 1, 100, "John Doe", "Great product!", 5);

            when(reviewCommandRepository.restore(1L)).thenReturn(Uni.createFrom().item(restoredReview));
            when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<ReviewResponseDeleteAt> response = reviewService.restore(1).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Review restored successfully!");
            assertThat(response.data()).isNotNull();
        }

        @Test
        @DisplayName("Should fail restore when review not found or not trashed")
        void shouldFailRestoreWhenReviewNotFoundOrNotTrashed() {
            when(reviewCommandRepository.restore(999L)).thenReturn(Uni.createFrom().nullItem());

            assertThatThrownBy(() -> reviewService.restore(999).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Review not found");
        }
    }

    @Nested
    @DisplayName("Delete Review Tests")
    class DeleteReviewTests {

        @Test
        @DisplayName("Should delete review permanently successfully")
        void shouldDeleteReviewPermanentlySuccessfully() {
            Review deletedReview = createReviewEntity(1L, 1, 100, "John Doe", "Great product!", 5);

            when(reviewCommandRepository.deletePermanent(1L)).thenReturn(Uni.createFrom().item(deletedReview));
            when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<Void> response = reviewService.delete(1).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Review permanently deleted!");
        }

        @Test
        @DisplayName("Should fail delete when review not found or not trashed")
        void shouldFailDeleteWhenReviewNotFoundOrNotTrashed() {
            when(reviewCommandRepository.deletePermanent(999L)).thenReturn(Uni.createFrom().nullItem());

            assertThatThrownBy(() -> reviewService.delete(999).await().indefinitely())
                    .isInstanceOf(com.sanedge.common.exception.InvalidRequestException.class)
                    .hasMessageContaining("must be trashed");
        }
    }

    @Nested
    @DisplayName("Restore All & Delete All Tests")
    class RestoreAllDeleteAllTests {

        @Test
        @DisplayName("Should restore all trashed reviews successfully")
        void shouldRestoreAllTrashedReviewsSuccessfully() {
            when(reviewCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));

            ApiResponse<Void> response = reviewService.restoreAll().await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("All reviews restored successfully!");
        }

        @Test
        @DisplayName("Should fail restore all when no trashed reviews found")
        void shouldFailRestoreAllWhenNoTrashedReviewsFound() {
            when(reviewCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));

            assertThatThrownBy(() -> reviewService.restoreAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed reviews found");
        }

        @Test
        @DisplayName("Should delete all trashed reviews permanently successfully")
        void shouldDeleteAllTrashedReviewsPermanentlySuccessfully() {
            when(reviewCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));

            ApiResponse<Void> response = reviewService.deleteAll().await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("All reviews permanently deleted!");
        }

        @Test
        @DisplayName("Should fail delete all when no trashed reviews found")
        void shouldFailDeleteAllWhenNoTrashedReviewsFound() {
            when(reviewCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));

            assertThatThrownBy(() -> reviewService.deleteAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed reviews found");
        }
    }

    /**
     * Finds the Supplier argument in the invocation regardless of whether it was
     * passed positionally in the 3-arg overload (arg index 2) or 4-arg overload
     * (arg index 3), then invokes it and returns the resulting Uni. This lets
     * a single Answer<?> body serve both traceAndMeasure overloads.
     */
    private Answer<Uni<?>> invokeSupplier() {
        return invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? (Uni<?>) supplier.get() : null;
        };
    }
}