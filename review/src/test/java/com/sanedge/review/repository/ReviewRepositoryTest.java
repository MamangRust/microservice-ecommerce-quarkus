package com.sanedge.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import com.sanedge.review.domain.requests.FindAllReview;
import com.sanedge.review.entity.Review;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class ReviewRepositoryTest {

    @Inject
    ReviewQueryRepository reviewQueryRepo;

    @Inject
    ReviewCommandRepository reviewCommandRepo;

    private Uni<Review> createAndPersistReview(String name, String comment, Integer productId) {
        Review review = new Review();
        review.setUserId(1);
        review.setProductId(productId);
        review.setName(name);
        review.setComment(comment);
        review.setRating(5);
        review.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        review.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return reviewQueryRepo.persist(review).replaceWith(review);
    }

    private FindAllReview buildFindAllRequest(int page, int pageSize, String search) {
        FindAllReview req = new FindAllReview();
        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(search);
        return req;
    }

    @Test
    @WithSession
    Uni<Void> testCreateAndFindById() {
        return createAndPersistReview("John Doe", "Great product!", 1)
                .invoke(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.id).isNotNull();
                    assertThat(saved.getName()).isEqualTo("John Doe");
                })
                .chain(saved -> reviewQueryRepo.findReviewById(saved.id))
                .invoke(opt -> {
                    assertThat(opt).isPresent();
                    assertThat(opt.get().getComment()).isEqualTo("Great product!");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindReviewByIdReturnsEmptyWhenNotFound() {
        return reviewQueryRepo.findReviewById(99999L)
                .invoke(opt -> assertThat(opt).isEmpty())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashReview() {
        return createAndPersistReview("Jane Doe", "Nice item", 1)
                .invoke(saved -> assertThat(saved.getDeletedAt()).isNull())
                .chain(saved -> reviewCommandRepo.trash(saved.id))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashReviewReturnsNullIfAlreadyTrashed() {
        return createAndPersistReview("Alice", "Okay", 1)
                .chain(saved -> reviewCommandRepo.trash(saved.id)
                        .chain(ignored -> reviewCommandRepo.trash(saved.id)))
                .invoke(trashedAgain -> assertThat(trashedAgain).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreReview() {
        return createAndPersistReview("Bob", "Good", 1)
                .chain(saved -> reviewCommandRepo.trash(saved.id)
                        .chain(ignored -> reviewCommandRepo.restore(saved.id)))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanent() {
        return createAndPersistReview("Charlie", "Bad", 1)
                .chain(saved -> reviewCommandRepo.trash(saved.id)
                        .chain(ignored -> reviewCommandRepo.deletePermanent(saved.id))
                        .chain(deleted -> {
                            assertThat(deleted).isNotNull();
                            return reviewQueryRepo.findReviewById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isEmpty())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistReview("R1", "C1", 1),
                        createAndPersistReview("R2", "C2", 1))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        reviewCommandRepo.trash(tuple.getItem1().id),
                        reviewCommandRepo.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> reviewCommandRepo.restoreAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    reviewQueryRepo.findReviewById(tuple.getItem1().id),
                                    reviewQueryRepo.findReviewById(tuple.getItem2().id))
                                    .andCollectFailures()
                                    .replaceWith(tuple);
                        }))
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistReview("D1", "C1", 1),
                        createAndPersistReview("D2", "C2", 1))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        reviewCommandRepo.trash(tuple.getItem1().id),
                        reviewCommandRepo.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> reviewCommandRepo.deleteAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    reviewQueryRepo.findReviewById(tuple.getItem1().id),
                                    reviewQueryRepo.findReviewById(tuple.getItem2().id))
                                    .andCollectFailures()
                                    .replaceWith(tuple);
                        }))
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindReviewsExcludesTrashed() {
        return Uni.combine().all()
                .unis(createAndPersistReview("Active Review", "Good", 1),
                        createAndPersistReview("Trashed Review", "Bad", 1))
                .asTuple()
                .chain(tuple -> reviewCommandRepo.trash(tuple.getItem2().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllReview req = buildFindAllRequest(1, 10, null);
                    return reviewQueryRepo.findReviews(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Active Review");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindTrashedReviews() {
        return Uni.combine().all()
                .unis(createAndPersistReview("Trashed Only", "Bad", 1),
                        createAndPersistReview("Not Trashed", "Good", 1))
                .asTuple()
                .chain(tuple -> reviewCommandRepo.trash(tuple.getItem1().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllReview req = buildFindAllRequest(1, 10, null);
                    return reviewQueryRepo.findTrashedReviews(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Trashed Only");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindReviewsWithSearchKeyword() {
        return Uni.join().all(
                createAndPersistReview("Excellent product", "Loved it", 1),
                createAndPersistReview("Excellent service", "Fast delivery", 1),
                createAndPersistReview("Average item", "Okay", 1))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllReview req = buildFindAllRequest(1, 10, "Excellent");
                    return reviewQueryRepo.findReviews(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindReviewsWithPagination() {
        return Uni.join().all(
                createAndPersistReview("Page1", "C1", 1),
                createAndPersistReview("Page2", "C2", 1),
                createAndPersistReview("Page3", "C3", 1),
                createAndPersistReview("Page4", "C4", 1),
                createAndPersistReview("Page5", "C5", 1))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllReview reqPage1 = buildFindAllRequest(1, 2, null);
                    return reviewQueryRepo.findReviews(reqPage1);
                })
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(page1 -> {
                    FindAllReview reqPage2 = buildFindAllRequest(2, 2, null);
                    return reviewQueryRepo.findReviews(reqPage2)
                            .invoke(page2 -> {
                                assertThat(page2.getData()).hasSize(2);
                                assertThat(page2.getData().get(0).getName()).isNotIn(
                                        page1.getData().get(0).getName(),
                                        page1.getData().get(1).getName());
                            });
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindReviewsWithNullSearchReturnsAll() {
        return Uni.join().all(
                createAndPersistReview("Item A", "Comment A", 1),
                createAndPersistReview("Item B", "Comment B", 1))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllReview req = buildFindAllRequest(1, 10, null);
                    return reviewQueryRepo.findReviews(req);
                })
                .invoke(result -> assertThat(result.getData()).hasSize(2))
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindReviewsSearchReturnsEmptyWhenNoMatch() {
        return createAndPersistReview("Summer Sale", "Good", 1)
                .chain(ignored -> {
                    FindAllReview req = buildFindAllRequest(1, 10, "Winter");
                    return reviewQueryRepo.findReviews(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).isEmpty();
                    assertThat(result.getTotalRecords()).isZero();
                })
                .replaceWithVoid();
    }
}
