package com.sanedge.category.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.category.domain.requests.FindAllCategoryRequest;
import com.sanedge.category.entity.Category;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class CategoryRepositoryTest {

    @Inject
    CategoryQueryRepository categoryQueryRepo;

    @Inject
    CategoryCommandRepository categoryCommandRepo;

    private Uni<Long> persist(String name, String description, String slug) {
        Category cat = new Category();
        cat.setName(name);
        cat.setDescription(description);
        cat.setSlugCategory(slug);
        return categoryQueryRepo.persist(cat).map(Category::getId);
    }

    private Uni<Long> persist(String name) {
        return persist(name, "description of " + name, "slug-" + name.toLowerCase().replace(' ', '-'));
    }

    private Uni<Void> clean() {
        return categoryQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllCategoryRequest findAllReq(int page, int size, String search) {
        FindAllCategoryRequest r = new FindAllCategoryRequest();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persist("Electronics"))
                .chain(id -> categoryQueryRepo.findById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Electronics");
                    assertThat(found.getSlugCategory()).isEqualTo("slug-electronics");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindCategoryById() {
        return clean()
                .chain(() -> persist("Books"))
                .chain(id -> categoryQueryRepo.findCategoryById(id))
                .invoke(cat -> {
                    assertThat(cat).isNotNull();
                    assertThat(cat.getName()).isEqualTo("Books");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindCategoryByIdReturnsNullIfNotFound() {
        return clean()
                .chain(() -> categoryQueryRepo.findCategoryById(999999L))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindCategoryByIdReturnsNullIfTrashed() {
        return clean()
                .chain(() -> persist("Clothing"))
                .chain(id -> categoryCommandRepo.trash(id)
                        .chain(() -> categoryQueryRepo.findCategoryById(id)))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> categoryQueryRepo.findById(999999L))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashCategory() {
        return clean()
                .chain(() -> persist("Sports"))
                .chain(id -> categoryCommandRepo.trash(id))
                .invoke(cat -> {
                    assertThat(cat).isNotNull();
                    assertThat(cat.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashCategoryReturnsNullIfAlreadyTrashed() {
        return clean()
                .chain(() -> persist("Toys"))
                .chain(id -> categoryCommandRepo.trash(id)
                        .chain(() -> categoryCommandRepo.trash(id)))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashCategoryReturnsNullIfNotFound() {
        return clean()
                .chain(() -> categoryCommandRepo.trash(99999L))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreCategory() {
        return clean()
                .chain(() -> persist("Food"))
                .chain(id -> categoryCommandRepo.trash(id)
                        .chain(() -> categoryCommandRepo.restore(id)))
                .invoke(cat -> {
                    assertThat(cat).isNotNull();
                    assertThat(cat.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreCategoryReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persist("Beverages"))
                .chain(id -> categoryCommandRepo.restore(id))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreCategoryReturnsNullIfNotFound() {
        return clean()
                .chain(() -> categoryCommandRepo.restore(99999L))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist("Furniture"))
                .chain(id -> categoryCommandRepo.trash(id)
                        .chain(() -> categoryCommandRepo.deletePermanent(id))
                        .chain(deleted -> categoryQueryRepo.findById(id)))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return clean()
                .chain(() -> persist("Garden"))
                .chain(id -> categoryCommandRepo.deletePermanent(id))
                .invoke(cat -> assertThat(cat).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist("Music").chain(id -> categoryCommandRepo.trash(id).replaceWithVoid()))
                .chain(() -> persist("Movies").chain(id -> categoryCommandRepo.trash(id).replaceWithVoid()))
                .chain(() -> categoryCommandRepo.restoreAllDeleted())
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> categoryQueryRepo.findActiveCategories(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persist("Art").chain(id -> categoryCommandRepo.trash(id).replaceWithVoid()))
                .chain(() -> persist("Crafts").chain(id -> categoryCommandRepo.trash(id).replaceWithVoid()))
                .chain(() -> persist("Jewelry")) // tidak di‑trash
                .chain(() -> categoryCommandRepo.deleteAllDeleted())
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> categoryQueryRepo.findActiveCategories(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(1)) // hanya Jewelry yang tersisa
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindActiveCategories() {
        return clean()
                .chain(() -> persist("A"))
                .chain(() -> persist("B"))
                .chain(() -> categoryQueryRepo.findActiveCategories(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedCategories() {
        return clean()
                .chain(() -> persist("C"))
                .chain(() -> persist("D").chain(id -> categoryCommandRepo.trash(id).replaceWithVoid()))
                .chain(() -> categoryQueryRepo.findTrashedCategories(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindCategoriesWithSearchKeyword() {
        return clean()
                .chain(() -> persist("Smartphone"))
                .chain(() -> persist("Smartwatch"))
                .chain(() -> persist("Tablet"))
                .chain(() -> categoryQueryRepo.findCategories(findAllReq(1, 10, "Smart")))
                .invoke(page -> {
                    assertThat(page.getTotalRecords()).isEqualTo(2);
                    assertThat(page.getData()).extracting(Category::getName)
                            .containsExactlyInAnyOrder("Smartphone", "Smartwatch");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindCategoriesWithPagination() {
        return clean()
                .chain(() -> persist("Cat1"))
                .chain(() -> persist("Cat2"))
                .chain(() -> persist("Cat3"))
                .chain(() -> persist("Cat4"))
                .chain(() -> persist("Cat5"))
                .chain(() -> categoryQueryRepo.findCategories(findAllReq(1, 2, "")))
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(() -> categoryQueryRepo.findCategories(findAllReq(2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .replaceWithVoid();
    }
}