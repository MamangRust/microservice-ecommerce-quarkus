package com.sanedge.merchant_award.repository;

import com.sanedge.merchant_award.entity.MerchantCertificationAndAward;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@ApplicationScoped
public class MerchantAwardCommandRepository implements PanacheRepository<MerchantCertificationAndAward> {

    @WithTransaction
    public Uni<MerchantCertificationAndAward> trashed(Long merchantAwardId) {
        return findById(merchantAwardId)
                .chain(award -> {
                    if (award != null && award.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        award.setDeletedAt(Timestamp.valueOf(date));
                        return persist(award).map(v -> award);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantCertificationAndAward> restore(Long merchantAwardId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", merchantAwardId).firstResult()
                .chain(award -> {
                    if (award != null) {
                        award.setDeletedAt(null);
                        return persist(award).map(v -> award);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantCertificationAndAward> deletePermanent(Long merchantAwardId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", merchantAwardId).firstResult()
                .chain(award -> {
                    if (award != null) {
                        return delete(award).map(v -> award);
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