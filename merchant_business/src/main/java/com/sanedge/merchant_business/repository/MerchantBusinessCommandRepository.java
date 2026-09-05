package com.sanedge.merchant_business.repository;

import com.sanedge.merchant_business.entity.MerchantBusinessInformation;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@ApplicationScoped
public class MerchantBusinessCommandRepository implements PanacheRepository<MerchantBusinessInformation> {

    @WithTransaction
    public Uni<MerchantBusinessInformation> trashed(Long merchantBusinessId) {
        return findById(merchantBusinessId)
                .chain(info -> {
                    if (info != null && info.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        info.setDeletedAt(Timestamp.valueOf(date));
                        return persist(info).map(v -> info);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantBusinessInformation> restore(Long merchantBusinessId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", merchantBusinessId).firstResult()
                .chain(info -> {
                    if (info != null) {
                        info.setDeletedAt(null);
                        return persist(info).map(v -> info);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantBusinessInformation> deletePermanent(Long merchantBusinessId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", merchantBusinessId).firstResult()
                .chain(info -> {
                    if (info != null) {
                        return delete(info).map(v -> info);
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
