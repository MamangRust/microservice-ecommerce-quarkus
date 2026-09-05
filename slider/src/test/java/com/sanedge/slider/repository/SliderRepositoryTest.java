package com.sanedge.slider.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.slider.domain.requests.FindAllSliderRequest;
import com.sanedge.slider.entity.Slider;

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
class SliderRepositoryTest {

    @Inject
    SliderQueryRepository sliderQueryRepo;

    @Inject
    SliderCommandRepository sliderCommandRepo;

    private Uni<Slider> createAndPersistSlider(String name) {
        Slider slider = new Slider();
        slider.setName(name);
        return sliderQueryRepo.persist(slider).replaceWith(slider);
    }

    @Test
    @WithSession
    Uni<Void> testCreateAndFindById() {
        return createAndPersistSlider("Main Banner")
                .invoke(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.id).isNotNull();
                    assertThat(saved.getName()).isEqualTo("Main Banner");
                })
                .chain(saved -> sliderQueryRepo.findById(saved.id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Main Banner");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashSlider() {
        return createAndPersistSlider("Trash Me")
                .invoke(saved -> assertThat(saved.getDeletedAt()).isNull())
                .chain(saved -> sliderCommandRepo.trashed(saved.id))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashSliderReturnsNullIfAlreadyTrashed() {
        return createAndPersistSlider("Trash Me Twice")
                .chain(saved -> sliderCommandRepo.trashed(saved.id)
                        .chain(ignored -> sliderCommandRepo.trashed(saved.id)))
                .invoke(trashedAgain -> assertThat(trashedAgain).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashSliderReturnsNullIfNotFound() {
        return sliderCommandRepo.trashed(99999L)
                .invoke(trashed -> assertThat(trashed).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreSlider() {
        return createAndPersistSlider("Restore Me")
                .chain(saved -> sliderCommandRepo.trashed(saved.id)
                        .chain(ignored -> sliderCommandRepo.restore(saved.id)))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreSliderReturnsNullIfNotTrashed() {
        return createAndPersistSlider("Restore Me Not")
                .chain(saved -> sliderCommandRepo.restore(saved.id))
                .invoke(restored -> assertThat(restored).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanent() {
        return createAndPersistSlider("Delete Permanent")
                .chain(saved -> sliderCommandRepo.trashed(saved.id)
                        .chain(ignored -> sliderCommandRepo.deletePermanent(saved.id))
                        .chain(deleted -> {
                            assertThat(deleted).isNotNull();
                            return sliderQueryRepo.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return createAndPersistSlider("Delete Permanent Fail")
                .chain(saved -> sliderCommandRepo.deletePermanent(saved.id)
                        .chain(deleted -> {
                            assertThat(deleted).isNull();
                            return sliderQueryRepo.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistSlider("Bulk Restore 1"),
                        createAndPersistSlider("Bulk Restore 2"),
                        createAndPersistSlider("Bulk Restore 3"))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        sliderCommandRepo.trashed(tuple.getItem1().id),
                        sliderCommandRepo.trashed(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> sliderCommandRepo.restoreAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    sliderQueryRepo.findById(tuple.getItem1().id),
                                    sliderQueryRepo.findById(tuple.getItem2().id),
                                    sliderQueryRepo.findById(tuple.getItem3().id))
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
                .unis(createAndPersistSlider("Bulk Delete 1"),
                        createAndPersistSlider("Bulk Delete 2"),
                        createAndPersistSlider("Bulk Delete 3"))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        sliderCommandRepo.trashed(tuple.getItem1().id),
                        sliderCommandRepo.trashed(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> sliderCommandRepo.deleteAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    sliderQueryRepo.findById(tuple.getItem1().id),
                                    sliderQueryRepo.findById(tuple.getItem2().id),
                                    sliderQueryRepo.findById(tuple.getItem3().id))
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
    Uni<Void> testFindSlidersExcludesTrashed() {
        return Uni.combine().all()
                .unis(createAndPersistSlider("Active Slider"),
                        createAndPersistSlider("Trashed Slider"))
                .asTuple()
                .chain(tuple -> sliderCommandRepo.trashed(tuple.getItem2().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllSliderRequest req = new FindAllSliderRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    return sliderQueryRepo.findSliders(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Active Slider");
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindTrashedSliders() {
        return Uni.combine().all()
                .unis(createAndPersistSlider("Trashed Only"),
                        createAndPersistSlider("Not Trashed"))
                .asTuple()
                .chain(tuple -> sliderCommandRepo.trashed(tuple.getItem1().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllSliderRequest req = new FindAllSliderRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    return sliderQueryRepo.findTrashedSliders(req);
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
    Uni<Void> testFindSlidersWithSearchKeyword() {
        return Uni.join().all(
                createAndPersistSlider("Summer Promo"),
                createAndPersistSlider("Summer Clearance"),
                createAndPersistSlider("Winter Sale"))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllSliderRequest req = new FindAllSliderRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("Summer");
                    return sliderQueryRepo.findSliders(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindTrashedSlidersWithSearchKeyword() {
        return Uni.combine().all()
                .unis(createAndPersistSlider("Trashed Promo"),
                        createAndPersistSlider("Trashed Discount"),
                        createAndPersistSlider("Active Promo"))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        sliderCommandRepo.trashed(tuple.getItem1().id),
                        sliderCommandRepo.trashed(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllSliderRequest req = new FindAllSliderRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("Promo");
                    return sliderQueryRepo.findTrashedSliders(req);
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
    Uni<Void> testFindSlidersWithPagination() {
        return Uni.join().all(
                createAndPersistSlider("Page Slider 1"),
                createAndPersistSlider("Page Slider 2"),
                createAndPersistSlider("Page Slider 3"),
                createAndPersistSlider("Page Slider 4"),
                createAndPersistSlider("Page Slider 5"))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllSliderRequest reqPage1 = new FindAllSliderRequest();
                    reqPage1.setPage(1);
                    reqPage1.setPageSize(2);
                    return sliderQueryRepo.findSliders(reqPage1);
                })
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(page1 -> {
                    FindAllSliderRequest reqPage2 = new FindAllSliderRequest();
                    reqPage2.setPage(2);
                    reqPage2.setPageSize(2);
                    return sliderQueryRepo.findSliders(reqPage2)
                            .invoke(page2 -> {
                                assertThat(page2.getData()).hasSize(2);
                                assertThat(page2.getData().get(0).getName()).isNotIn(
                                        page1.getData().get(0).getName(),
                                        page1.getData().get(1).getName());
                            });
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindSlidersWithEmptySearchReturnsAll() {
        return Uni.join().all(
                createAndPersistSlider("Slider A"),
                createAndPersistSlider("Slider B"))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllSliderRequest req = new FindAllSliderRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return sliderQueryRepo.findSliders(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindSlidersWithNullSearchReturnsAll() {
        return Uni.join().all(
                createAndPersistSlider("Slider C"),
                createAndPersistSlider("Slider D"))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllSliderRequest req = new FindAllSliderRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch(null);
                    return sliderQueryRepo.findSliders(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindSlidersSearchReturnsEmptyWhenNoMatch() {
        return createAndPersistSlider("Summer Sale")
                .chain(ignored -> {
                    FindAllSliderRequest req = new FindAllSliderRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("Winter");
                    return sliderQueryRepo.findSliders(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).isEmpty();
                    assertThat(result.getTotalRecords()).isZero();
                })
                .replaceWithVoid();
    }
}
