package com.sanedge.slider.repository;

import com.sanedge.slider.entity.Slider;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@ApplicationScoped
public class SliderCommandRepository implements PanacheRepository<Slider> {

    @WithTransaction
    public Uni<Slider> trashed(Long sliderId) {
        return findById(sliderId)
                .chain(slider -> {
                    if (slider != null && slider.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        slider.setDeletedAt(Timestamp.valueOf(date));
                        return persist(slider).map(v -> slider);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Slider> restore(Long sliderId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", sliderId).firstResult()
                .chain(slider -> {
                    if (slider != null) {
                        slider.setDeletedAt(null);
                        return persist(slider).map(v -> slider);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Slider> deletePermanent(Long sliderId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", sliderId).firstResult()
                .chain(slider -> {
                    if (slider != null) {
                        return delete(slider).map(v -> slider);
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
