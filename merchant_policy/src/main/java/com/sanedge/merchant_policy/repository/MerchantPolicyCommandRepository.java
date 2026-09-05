package com.sanedge.merchant_policy.repository;

import com.sanedge.merchant_policy.entity.MerchantPolicy;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@ApplicationScoped
public class MerchantPolicyCommandRepository implements PanacheRepository<MerchantPolicy> {

    @WithTransaction
    public Uni<MerchantPolicy> trash(Long merchantPolicyId) {
        return findById(merchantPolicyId)
                .chain(policy -> {
                    if (policy != null && policy.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        policy.setDeletedAt(Timestamp.valueOf(date));
                        return persist(policy).map(v -> policy);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantPolicy> restore(Long merchantPolicyId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", merchantPolicyId).firstResult()
                .chain(policy -> {
                    if (policy != null) {
                        policy.setDeletedAt(null);
                        return persist(policy).map(v -> policy);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantPolicy> deletePermanent(Long merchantPolicyId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", merchantPolicyId).firstResult()
                .chain(policy -> {
                    if (policy != null) {
                        return delete(policy).map(v -> policy);
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