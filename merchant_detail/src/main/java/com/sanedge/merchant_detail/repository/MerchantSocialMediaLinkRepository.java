package com.sanedge.merchant_detail.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import com.sanedge.merchant_detail.entity.MerchantSocialMediaLink;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantSocialMediaLinkRepository implements PanacheRepository<MerchantSocialMediaLink> {

    @WithTransaction
    public Uni<MerchantSocialMediaLink> trashed(Long merchantSocialId) {
        return findById(merchantSocialId)
                .chain(socialLink -> {
                    if (socialLink != null && socialLink.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        socialLink.setDeletedAt(Timestamp.valueOf(date));
                        return persist(socialLink).map(v -> socialLink);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantSocialMediaLink> restore(Long merchantSocialId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", merchantSocialId).firstResult()
                .chain(socialLink -> {
                    if (socialLink != null) {
                        socialLink.setDeletedAt(null);
                        return persist(socialLink).map(v -> socialLink);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantSocialMediaLink> deletePermanent(Long merchantSocialId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", merchantSocialId).firstResult()
                .chain(socialLink -> {
                    if (socialLink != null) {
                        return delete(socialLink).map(v -> socialLink);
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

    public Uni<Optional<MerchantSocialMediaLink>> findByMerchantDetailIdAndPlatform(Integer merchantDetailId,
            String platform) {
        return find("merchantDetailId = ?1 AND platform = ?2 AND deletedAt IS NULL", merchantDetailId, platform)
                .firstResult()
                .map(Optional::ofNullable);
    }
}
