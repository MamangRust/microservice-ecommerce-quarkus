package com.sanedge.merchant_detail.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.sanedge.merchant_detail.entity.MerchantDetail;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class MerchantDetailRepositoryTest {

    @Inject
    MerchantDetailQueryRepository merchantDetailQueryRepo;

    @Inject
    MerchantDetailCommandRepository merchantDetailCommandRepo;

    private Uni<MerchantDetail> createAndPersistDetail(Integer merchantId, String displayName) {
        MerchantDetail detail = new MerchantDetail();
        detail.setMerchantId(merchantId);
        detail.setDisplayName(displayName);
        detail.setCoverImageUrl("http://example.com/cover.jpg");
        detail.setLogoUrl("http://example.com/logo.jpg");
        detail.setShortDescription("Short desc for " + displayName);
        detail.setWebsiteUrl("http://" + displayName.toLowerCase().replace(" ", "") + ".com");
        return merchantDetailQueryRepo.persist(detail).replaceWith(detail);
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return createAndPersistDetail(1, "Toko Sejahtera")
                .invoke(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.id).isNotNull();
                    assertThat(saved.getDisplayName()).isEqualTo("Toko Sejahtera");
                })
                .chain(saved -> merchantDetailQueryRepo.findById(saved.id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getWebsiteUrl()).contains("tokosejahtera");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashMerchantDetail() {
        return createAndPersistDetail(1, "Toko Trash")
                .invoke(saved -> assertThat(saved.getDeletedAt()).isNull())
                .chain(saved -> merchantDetailCommandRepo.trashed(saved.id))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashMerchantDetailReturnsNullIfAlreadyTrashed() {
        return createAndPersistDetail(1, "Toko Trash 2")
                .chain(saved -> merchantDetailCommandRepo.trashed(saved.id)
                        .chain(ignored -> merchantDetailCommandRepo.trashed(saved.id)))
                .invoke(trashedAgain -> assertThat(trashedAgain).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashMerchantDetailReturnsNullIfNotFound() {
        return merchantDetailCommandRepo.trashed(99999L)
                .invoke(trashed -> assertThat(trashed).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreMerchantDetail() {
        return createAndPersistDetail(1, "Toko Restore")
                .chain(saved -> merchantDetailCommandRepo.trashed(saved.id)
                        .chain(ignored -> merchantDetailCommandRepo.restore(saved.id)))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreMerchantDetailReturnsNullIfNotTrashed() {
        return createAndPersistDetail(1, "Toko Restore 2")
                .chain(saved -> merchantDetailCommandRepo.restore(saved.id))
                .invoke(restored -> assertThat(restored).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return createAndPersistDetail(1, "Toko Delete")
                .chain(saved -> merchantDetailCommandRepo.trashed(saved.id)
                        .chain(ignored -> merchantDetailCommandRepo.deletePermanent(saved.id))
                        .chain(deleted -> {
                            assertThat(deleted).isNotNull();
                            return merchantDetailQueryRepo.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return createAndPersistDetail(1, "Toko Delete 2")
                .chain(saved -> merchantDetailCommandRepo.deletePermanent(saved.id)
                        .chain(deleted -> {
                            assertThat(deleted).isNull();
                            return merchantDetailQueryRepo.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistDetail(1, "Bulk Restore 1"),
                        createAndPersistDetail(1, "Bulk Restore 2"),
                        createAndPersistDetail(2, "Bulk Restore 3"))
                .asTuple()
                .chain(tuple -> {
                    Long id1 = tuple.getItem1().id;
                    Long id2 = tuple.getItem2().id;
                    Long id3 = tuple.getItem3().id;

                    return Uni.join().all(
                            merchantDetailCommandRepo.trashed(id1),
                            merchantDetailCommandRepo.trashed(id2))
                            .andCollectFailures()
                            .replaceWithVoid()
                            .chain(ignored -> merchantDetailCommandRepo.restoreAllDeleted())
                            .invoke(result -> assertThat(result).isTrue())
                            .chain(ignored -> Panache.getSession().invoke(s -> s.clear()))
                            .chain(ignored -> merchantDetailQueryRepo.findById(id1))
                            .invoke(item -> assertThat(item.getDeletedAt()).isNull())
                            .chain(ignored -> merchantDetailQueryRepo.findById(id2))
                            .invoke(item -> assertThat(item.getDeletedAt()).isNull())
                            .chain(ignored -> merchantDetailQueryRepo.findById(id3))
                            .invoke(item -> assertThat(item.getDeletedAt()).isNull());
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistDetail(1, "Bulk Del 1"),
                        createAndPersistDetail(1, "Bulk Del 2"),
                        createAndPersistDetail(2, "Bulk Del 3"))
                .asTuple()
                .chain(tuple -> {
                    Long id1 = tuple.getItem1().id;
                    Long id2 = tuple.getItem2().id;
                    Long id3 = tuple.getItem3().id;

                    return Uni.join().all(
                            merchantDetailCommandRepo.trashed(id1),
                            merchantDetailCommandRepo.trashed(id2))
                            .andCollectFailures()
                            .replaceWithVoid()
                            .chain(ignored -> merchantDetailCommandRepo.deleteAllDeleted())
                            .invoke(result -> assertThat(result).isTrue())
                            .chain(ignored -> Panache.getSession().invoke(s -> s.clear()))
                            .chain(ignored -> merchantDetailQueryRepo.findById(id1))
                            .invoke(item -> assertThat(item).isNull())
                            .chain(ignored -> merchantDetailQueryRepo.findById(id2))
                            .invoke(item -> assertThat(item).isNull())
                            .chain(ignored -> merchantDetailQueryRepo.findById(id3))
                            .invoke(item -> assertThat(item).isNotNull());
                })
                .replaceWithVoid();
    }
}