package com.sanedge.review_detail.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import com.sanedge.review_detail.entity.ReviewDetail;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class ReviewDetailRepositoryTest {

    @Inject
    ReviewDetailRepository reviewDetailRepository;

    private Uni<ReviewDetail> createAndPersistDetail(Long reviewId, String type, String url) {
        ReviewDetail detail = new ReviewDetail();
        detail.setType(type);
        detail.setUrl(url);
        detail.setCaption("Test caption for " + type);
        detail.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        detail.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return reviewDetailRepository.persist(detail).replaceWith(detail);
    }

    @Test
    @WithSession
    Uni<Void> testTrashReviewDetail() {
        return createAndPersistDetail(1L, "IMAGE", "http://example.com/img.jpg")
                .invoke(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.id).isNotNull();
                    assertThat(saved.getDeletedAt()).isNull();
                })
                .chain(saved -> reviewDetailRepository.trash(saved.id))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashReviewDetailReturnsNullIfAlreadyTrashed() {
        return createAndPersistDetail(1L, "VIDEO", "http://example.com/vid.mp4")
                .chain(saved -> reviewDetailRepository.trash(saved.id)
                        .chain(ignored -> reviewDetailRepository.trash(saved.id)))
                .invoke(trashedAgain -> assertThat(trashedAgain).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashReviewDetailReturnsNullIfNotFound() {
        return reviewDetailRepository.trash(99999L)
                .invoke(trashed -> assertThat(trashed).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreReviewDetail() {
        return createAndPersistDetail(1L, "IMAGE", "http://example.com/img2.jpg")
                .chain(saved -> reviewDetailRepository.trash(saved.id)
                        .chain(ignored -> reviewDetailRepository.restore(saved.id)))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreReviewDetailReturnsNullIfNotTrashed() {
        return createAndPersistDetail(1L, "IMAGE", "http://example.com/img3.jpg")
                .chain(saved -> reviewDetailRepository.restore(saved.id))
                .invoke(restored -> assertThat(restored).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanent() {
        return createAndPersistDetail(1L, "IMAGE", "http://example.com/img4.jpg")
                .chain(saved -> reviewDetailRepository.trash(saved.id)
                        .chain(ignored -> reviewDetailRepository.deletePermanent(saved.id))
                        .chain(deleted -> {
                            assertThat(deleted).isNotNull();
                            return reviewDetailRepository.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return createAndPersistDetail(1L, "IMAGE", "http://example.com/img5.jpg")
                .chain(saved -> reviewDetailRepository.deletePermanent(saved.id)
                        .chain(deleted -> {
                            assertThat(deleted).isNull();
                            return reviewDetailRepository.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistDetail(1L, "IMAGE", "url1.jpg"),
                        createAndPersistDetail(1L, "VIDEO", "url2.mp4"),
                        createAndPersistDetail(2L, "IMAGE", "url3.jpg"))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        reviewDetailRepository.trash(tuple.getItem1().id),
                        reviewDetailRepository.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> reviewDetailRepository.restoreAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    reviewDetailRepository.findById(tuple.getItem1().id),
                                    reviewDetailRepository.findById(tuple.getItem2().id),
                                    reviewDetailRepository.findById(tuple.getItem3().id))
                                    .andCollectFailures()
                                    .replaceWith(tuple);
                        }))
                .invoke(tuple -> {
                    assertThat(tuple.getItem1().getDeletedAt()).isNull();
                    assertThat(tuple.getItem2().getDeletedAt()).isNull();
                    assertThat(tuple.getItem3().getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistDetail(1L, "IMAGE", "url4.jpg"),
                        createAndPersistDetail(1L, "VIDEO", "url5.mp4"),
                        createAndPersistDetail(2L, "IMAGE", "url6.jpg"))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        reviewDetailRepository.trash(tuple.getItem1().id),
                        reviewDetailRepository.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> reviewDetailRepository.deleteAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    reviewDetailRepository.findById(tuple.getItem1().id),
                                    reviewDetailRepository.findById(tuple.getItem2().id),
                                    reviewDetailRepository.findById(tuple.getItem3().id))
                                    .andCollectFailures()
                                    .replaceWith(tuple);
                        }))
                .invoke(tuple -> {
                    assertThat(tuple.getItem1()).isNull();
                    assertThat(tuple.getItem2()).isNull();
                    assertThat(tuple.getItem3()).isNotNull();
                })
                .replaceWithVoid();
    }
}
