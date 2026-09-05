package com.sanedge.banner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.sql.Time;

import org.junit.jupiter.api.Test;

import com.sanedge.banner.domain.requests.FindAllBannerRequest;
import com.sanedge.banner.entity.Banner;

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
class BannerRepositoryTest {

    @Inject
    BannerQueryRepository bannerQueryRepo;

    @Inject
    BannerCommandRepository bannerCommandRepo;

    private Uni<Banner> createAndPersistBanner(String name, boolean isActive) {
        Banner banner = new Banner();
        banner.setName(name);
        banner.setStartDate(Date.valueOf("2025-01-01"));
        banner.setEndDate(Date.valueOf("2025-12-31"));
        banner.setStartTime(Time.valueOf("00:00:00"));
        banner.setEndTime(Time.valueOf("23:59:59"));
        banner.setIsActive(isActive);
        return bannerQueryRepo.persist(banner);
    }

    private Uni<Banner> createAndPersistBannerWithSchedule(String name, boolean isActive,
            String startDate, String endDate, String startTime, String endTime) {
        Banner banner = new Banner();
        banner.setName(name);
        banner.setStartDate(Date.valueOf(startDate));
        banner.setEndDate(Date.valueOf(endDate));
        banner.setStartTime(Time.valueOf(startTime));
        banner.setEndTime(Time.valueOf(endTime));
        banner.setIsActive(isActive);
        return bannerQueryRepo.persist(banner);
    }

    @Test
    @WithSession
    Uni<Void> testCreateAndFindById() {
        return createAndPersistBannerWithSchedule("Summer Sale", true,
                "2025-06-01", "2025-08-31", "08:00:00", "22:00:00")
                .invoke(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.id).isNotNull();
                    assertThat(saved.getName()).isEqualTo("Summer Sale");
                    assertThat(saved.getIsActive()).isTrue();
                    assertThat(saved.getStartDate()).isEqualTo(Date.valueOf("2025-06-01"));
                    assertThat(saved.getEndDate()).isEqualTo(Date.valueOf("2025-08-31"));
                    assertThat(saved.getStartTime()).isEqualTo(Time.valueOf("08:00:00"));
                    assertThat(saved.getEndTime()).isEqualTo(Time.valueOf("22:00:00"));
                })
                .chain(saved -> bannerQueryRepo.findById(saved.id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Summer Sale");
                    assertThat(found.getStartDate()).isEqualTo(Date.valueOf("2025-06-01"));
                    assertThat(found.getEndDate()).isEqualTo(Date.valueOf("2025-08-31"));
                    assertThat(found.getStartTime()).isEqualTo(Time.valueOf("08:00:00"));
                    assertThat(found.getEndTime()).isEqualTo(Time.valueOf("22:00:00"));
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByName() {
        return createAndPersistBanner("Flash Sale", true)
                .chain(ignored -> bannerQueryRepo.findByName("Flash Sale"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Flash Sale");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByNameCaseInsensitive() {
        return createAndPersistBanner("Mega Sale", true)
                .chain(ignored -> bannerQueryRepo.findByName("MEGA SALE"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Mega Sale");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByNameReturnsNullWhenNotFound() {
        return bannerQueryRepo.findByName("Nonexistent Banner")
                .invoke(notFound -> assertThat(notFound).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByNameExcludesTrashed() {
        return createAndPersistBanner("Trashed Banner", true)
                .chain(saved -> bannerCommandRepo.trash(saved.id)
                        .chain(ignored -> bannerQueryRepo.findByName("Trashed Banner")))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return bannerQueryRepo.findById(Long.MAX_VALUE)
                .invoke(notFound -> assertThat(notFound).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashBanner() {
        return createAndPersistBanner("Trash Me", true)
                .invoke(saved -> assertThat(saved.getDeletedAt()).isNull())
                .chain(saved -> bannerCommandRepo.trash(saved.id))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashBannerReturnsNullIfAlreadyTrashed() {
        return createAndPersistBanner("Trash Me Twice", true)
                .chain(saved -> bannerCommandRepo.trash(saved.id)
                        .chain(ignored -> bannerCommandRepo.trash(saved.id)))
                .invoke(trashedAgain -> assertThat(trashedAgain).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashBannerReturnsNullIfNotFound() {
        return bannerCommandRepo.trash(99999L)
                .invoke(trashed -> assertThat(trashed).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreBanner() {
        return createAndPersistBanner("Restore Me", true)
                .chain(saved -> bannerCommandRepo.trash(saved.id)
                        .chain(ignored -> bannerCommandRepo.restore(saved.id)))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreBannerReturnsNullIfNotTrashed() {
        return createAndPersistBanner("Restore Me Not", true)
                .chain(saved -> bannerCommandRepo.restore(saved.id))
                .invoke(restored -> assertThat(restored).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreBannerReturnsNullIfNotFound() {
        return bannerCommandRepo.restore(99999L)
                .invoke(restored -> assertThat(restored).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanent() {
        return createAndPersistBanner("Delete Permanent", true)
                .chain(saved -> bannerCommandRepo.trash(saved.id)
                        .chain(ignored -> bannerCommandRepo.deletePermanent(saved.id))
                        .chain(deleted -> {
                            assertThat(deleted).isNotNull();
                            return bannerQueryRepo.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return createAndPersistBanner("Delete Permanent Fail", true)
                .chain(saved -> bannerCommandRepo.deletePermanent(saved.id)
                        .chain(deleted -> {
                            assertThat(deleted).isNull();
                            return bannerQueryRepo.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanentReturnsNullIfNotFound() {
        return bannerCommandRepo.deletePermanent(99999L)
                .invoke(deleted -> assertThat(deleted).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistBanner("Bulk Restore 1", true),
                        createAndPersistBanner("Bulk Restore 2", true),
                        createAndPersistBanner("Bulk Restore 3", true))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        bannerCommandRepo.trash(tuple.getItem1().id),
                        bannerCommandRepo.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> bannerCommandRepo.restoreAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    bannerQueryRepo.findById(tuple.getItem1().id),
                                    bannerQueryRepo.findById(tuple.getItem2().id),
                                    bannerQueryRepo.findById(tuple.getItem3().id))
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
    Uni<Void> testRestoreAllDeletedReturnsFalseWhenNoTrashed() {
        return createAndPersistBanner("No Trashed Here", true)
                .chain(ignored -> bannerCommandRepo.restoreAllDeleted())
                .invoke(result -> assertThat(result).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistBanner("Bulk Delete 1", true),
                        createAndPersistBanner("Bulk Delete 2", true),
                        createAndPersistBanner("Bulk Delete 3", true))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        bannerCommandRepo.trash(tuple.getItem1().id),
                        bannerCommandRepo.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> bannerCommandRepo.deleteAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    bannerQueryRepo.findById(tuple.getItem1().id),
                                    bannerQueryRepo.findById(tuple.getItem2().id),
                                    bannerQueryRepo.findById(tuple.getItem3().id))
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

    @Test
    @WithSession
    Uni<Void> testDeleteAllDeletedReturnsFalseWhenNoTrashed() {
        return createAndPersistBanner("No Trashed To Delete", true)
                .chain(ignored -> bannerCommandRepo.deleteAllDeleted())
                .invoke(result -> assertThat(result).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindBannersExcludesTrashed() {
        return Uni.combine().all()
                .unis(createAndPersistBanner("Active Banner", true),
                        createAndPersistBanner("Trashed Banner", true))
                .asTuple()
                .chain(tuple -> bannerCommandRepo.trash(tuple.getItem2().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    return bannerQueryRepo.findBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Active Banner");
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindActiveBanners() {
        return Uni.combine().all()
                .unis(createAndPersistBanner("Active One", true),
                        createAndPersistBanner("Inactive One", false),
                        createAndPersistBanner("Trashed Active", true))
                .asTuple()
                .chain(tuple -> bannerCommandRepo.trash(tuple.getItem3().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    return bannerQueryRepo.findActiveBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Active One");
                    assertThat(result.getData().get(0).getIsActive()).isTrue();
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindTrashedBanners() {
        return Uni.combine().all()
                .unis(createAndPersistBanner("Trashed Only", true),
                        createAndPersistBanner("Not Trashed", true))
                .asTuple()
                .chain(tuple -> bannerCommandRepo.trash(tuple.getItem1().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    return bannerQueryRepo.findTrashedBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Trashed Only");
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindBannersWithSearchKeyword() {
        return Uni.join().all(
                createAndPersistBanner("Summer Promotion", true),
                createAndPersistBanner("Summer Clearance", true),
                createAndPersistBanner("Winter Sale", true))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("Summer");
                    return bannerQueryRepo.findBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindActiveBannersWithSearchKeyword() {
        return Uni.join().all(
                createAndPersistBanner("Active Flash", true),
                createAndPersistBanner("Active Deal", true),
                createAndPersistBanner("Inactive Flash", false))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("Flash");
                    return bannerQueryRepo.findActiveBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Active Flash");
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindTrashedBannersWithSearchKeyword() {
        return Uni.combine().all()
                .unis(createAndPersistBanner("Trashed Promo", true),
                        createAndPersistBanner("Trashed Discount", true),
                        createAndPersistBanner("Active Promo", true))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        bannerCommandRepo.trash(tuple.getItem1().id),
                        bannerCommandRepo.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("Promo");
                    return bannerQueryRepo.findTrashedBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Trashed Promo");
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindBannersWithPagination() {
        return Uni.join().all(
                createAndPersistBanner("Page Banner 1", true),
                createAndPersistBanner("Page Banner 2", true),
                createAndPersistBanner("Page Banner 3", true),
                createAndPersistBanner("Page Banner 4", true),
                createAndPersistBanner("Page Banner 5", true))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllBannerRequest reqPage1 = new FindAllBannerRequest();
                    reqPage1.setPage(1);
                    reqPage1.setPageSize(2);
                    return bannerQueryRepo.findBanners(reqPage1);
                })
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(page1 -> {
                    FindAllBannerRequest reqPage2 = new FindAllBannerRequest();
                    reqPage2.setPage(2);
                    reqPage2.setPageSize(2);
                    return bannerQueryRepo.findBanners(reqPage2)
                            .invoke(page2 -> {
                                assertThat(page2.getData()).hasSize(2);
                                assertThat(page2.getData().get(0).getName()).isNotIn(
                                        page1.getData().get(0).getName(),
                                        page1.getData().get(1).getName());
                            });
                })
                .chain(ignored -> {
                    FindAllBannerRequest reqPage3 = new FindAllBannerRequest();
                    reqPage3.setPage(3);
                    reqPage3.setPageSize(2);
                    return bannerQueryRepo.findBanners(reqPage3);
                })
                .invoke(page3 -> assertThat(page3.getData()).hasSize(1))
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindBannersWithZeroPageDefaultsToFirstPage() {
        return Uni.join().all(
                createAndPersistBanner("Default Page 1", true),
                createAndPersistBanner("Default Page 2", true),
                createAndPersistBanner("Default Page 3", true))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(0);
                    req.setPageSize(10);
                    return bannerQueryRepo.findBanners(req);
                })
                .invoke(result -> assertThat(result.getData()).hasSize(3))
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindBannersWithZeroPageSizeDefaultsToTen() {
        Uni<Banner> createBanners = Uni.createFrom().voidItem()
                .chain(ignored -> createAndPersistBanner("Default Size 1", true))
                .chain(ignored -> createAndPersistBanner("Default Size 2", true))
                .chain(ignored -> createAndPersistBanner("Default Size 3", true))
                .chain(ignored -> createAndPersistBanner("Default Size 4", true))
                .chain(ignored -> createAndPersistBanner("Default Size 5", true))
                .chain(ignored -> createAndPersistBanner("Default Size 6", true))
                .chain(ignored -> createAndPersistBanner("Default Size 7", true))
                .chain(ignored -> createAndPersistBanner("Default Size 8", true))
                .chain(ignored -> createAndPersistBanner("Default Size 9", true))
                .chain(ignored -> createAndPersistBanner("Default Size 10", true))
                .chain(ignored -> createAndPersistBanner("Default Size 11", true))
                .chain(ignored -> createAndPersistBanner("Default Size 12", true))
                .chain(ignored -> createAndPersistBanner("Default Size 13", true))
                .chain(ignored -> createAndPersistBanner("Default Size 14", true))
                .chain(ignored -> createAndPersistBanner("Default Size 15", true));

        return createBanners
                .chain(ignored -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(0);
                    return bannerQueryRepo.findBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(10);
                    assertThat(result.getTotalRecords()).isEqualTo(15);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindBannersWithEmptySearchReturnsAll() {
        return Uni.join().all(
                createAndPersistBanner("Banner A", true),
                createAndPersistBanner("Banner B", true))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return bannerQueryRepo.findBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindBannersWithNullSearchReturnsAll() {
        return Uni.join().all(
                createAndPersistBanner("Banner C", true),
                createAndPersistBanner("Banner D", true))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch(null);
                    return bannerQueryRepo.findBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindBannersSearchReturnsEmptyWhenNoMatch() {
        return createAndPersistBanner("Summer Sale", true)
                .chain(ignored -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("Winter");
                    return bannerQueryRepo.findBanners(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).isEmpty();
                    assertThat(result.getTotalRecords()).isZero();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashRestoreAndFindAgain() {
        return createAndPersistBanner("Lifecycle Banner", true)
                .chain(saved -> bannerQueryRepo.findByName("Lifecycle Banner")
                        .invoke(found1 -> assertThat(found1).isNotNull())
                        .replaceWith(saved))
                .chain(saved -> bannerCommandRepo.trash(saved.id)
                        .replaceWith(saved))
                .chain(saved -> bannerQueryRepo.findByName("Lifecycle Banner")
                        .invoke(found2 -> assertThat(found2).isNull())
                        .replaceWith(saved))
                .chain(saved -> {
                    FindAllBannerRequest req = new FindAllBannerRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    return bannerQueryRepo.findTrashedBanners(req)
                            .invoke(trashedResult -> assertThat(trashedResult.getData()).hasSize(1))
                            .replaceWith(saved);
                })
                .chain(saved -> bannerCommandRepo.restore(saved.id)
                        .replaceWith(saved))
                .chain(saved -> bannerQueryRepo.findByName("Lifecycle Banner"))
                .invoke(found3 -> {
                    assertThat(found3).isNotNull();
                    assertThat(found3.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashAndPermanentDeleteLifecycle() {
        FindAllBannerRequest req = new FindAllBannerRequest();
        req.setPage(1);
        req.setPageSize(10);

        return createAndPersistBanner("Full Lifecycle", true)
                .chain(saved -> bannerCommandRepo.trash(saved.id)
                        .invoke(trashed -> assertThat(trashed).isNotNull())
                        .replaceWith(saved))
                .chain(saved -> bannerQueryRepo.findTrashedBanners(req)
                        .invoke(trashedResult -> assertThat(trashedResult.getData()).hasSize(1))
                        .replaceWith(saved))
                .chain(saved -> bannerCommandRepo.deletePermanent(saved.id)
                        .invoke(deleted -> assertThat(deleted).isNotNull())
                        .replaceWith(saved))
                .chain(saved -> bannerQueryRepo.findTrashedBanners(req)
                        .invoke(afterDelete -> assertThat(afterDelete.getData()).isEmpty())
                        .replaceWith(saved))
                .chain(saved -> bannerQueryRepo.findById(saved.id))
                .invoke(checkDb -> assertThat(checkDb).isNull())
                .replaceWithVoid();
    }
}
