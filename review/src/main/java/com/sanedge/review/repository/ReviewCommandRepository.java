package com.sanedge.review.repository;

import com.sanedge.review.entity.Review;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReviewCommandRepository implements PanacheRepository<Review> {

    @WithTransaction
    public Uni<Review> trash(Long reviewId) {
        return findById(reviewId)
                .chain(review -> {
                    if (review != null && review.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        review.setDeletedAt(Timestamp.valueOf(date));
                        return persist(review).map(v -> review);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Review> restore(Long reviewId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", reviewId).firstResult()
                .chain(review -> {
                    if (review != null) {
                        review.setDeletedAt(null);
                        return persist(review).map(v -> review);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Review> deletePermanent(Long reviewId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", reviewId).firstResult()
                .chain(review -> {
                    if (review != null) {
                        return delete(review).map(v -> review);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> restoreAllDeleted() {
        return update("deletedAt = NULL WHERE deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteAllDeleted() {
        return delete("deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }
}
