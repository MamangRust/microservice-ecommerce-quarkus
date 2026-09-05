package com.sanedge.banner.repository;

import com.sanedge.banner.entity.Banner;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@ApplicationScoped
public class BannerCommandRepository implements PanacheRepository<Banner> {
    @WithTransaction
    public Uni<Banner> trash(Long bannerId) {
        return findById(bannerId)
                .chain(banner -> {
                    if (banner != null && banner.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        banner.setDeletedAt(Timestamp.valueOf(date));
                        return persist(banner).map(v -> banner);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Banner> restore(Long bannerId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", bannerId).firstResult()
                .chain(banner -> {
                    if (banner != null) {
                        banner.setDeletedAt(null);
                        return persist(banner).map(v -> banner);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Banner> deletePermanent(Long bannerId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", bannerId).firstResult()
                .chain(banner -> {
                    if (banner != null) {
                        return delete(banner).map(v -> banner);
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
