package com.sanedge.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import com.sanedge.product.domain.requests.FindAllProductByMerchantRequest;
import com.sanedge.product.entity.Product;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class ProductRepositoryTest {

    @Inject
    ProductQueryRepository productQueryRepo;
    @Inject
    ProductCommandRepository productCommandRepo;

    private Uni<Product> createAndPersistProduct(String name, Integer merchantId) {
        Product p = new Product();
        p.setMerchantId(merchantId);
        p.setCategoryId(1);
        p.setName(name);
        p.setDescription("Test description for " + name);
        p.setPrice(100000);
        p.setCountInStock(10);
        p.setBrand("TestBrand");
        p.setWeight(500);
        p.setRating(4.5f);
        p.setSlugProduct(name.toLowerCase().replace(" ", "-"));
        p.setImageProduct("http://example.com/" + name + ".jpg");
        p.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        p.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return productQueryRepo.persist(p).replaceWith(p);
    }

    @Test
    @WithSession
    Uni<Void> testCreateAndFindById() {
        return createAndPersistProduct("Phone", 1)
                .invoke(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.id).isNotNull();
                    assertThat(saved.getName()).isEqualTo("Phone");
                })
                .chain(saved -> productQueryRepo.findById(saved.id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Phone");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashProduct() {
        return createAndPersistProduct("ToTrash", 1)
                .chain(saved -> productCommandRepo.trashed(saved.id))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashReturnsNullIfAlreadyTrashed() {
        return createAndPersistProduct("TrashTwice", 1)
                .chain(saved -> productCommandRepo.trashed(saved.id)
                        .chain(ignored -> productCommandRepo.trashed(saved.id)))
                .invoke(second -> assertThat(second).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreProduct() {
        return createAndPersistProduct("ToRestore", 1)
                .chain(saved -> productCommandRepo.trashed(saved.id)
                        .chain(ignored -> productCommandRepo.restore(saved.id)))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanent() {
        return createAndPersistProduct("ToDelete", 1)
                .chain(saved -> productCommandRepo.trashed(saved.id)
                        .chain(ignored -> productCommandRepo.deletePermanent(saved.id))
                        .chain(deleted -> {
                            assertThat(deleted).isNotNull();
                            return productQueryRepo.findById(saved.id);
                        }))
                .invoke(check -> assertThat(check).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindProductsByMerchant() {
        return Uni.join().all(
                createAndPersistProduct("Phone1", 1),
                createAndPersistProduct("Phone2", 2))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllProductByMerchantRequest req = new FindAllProductByMerchantRequest();
                    req.setMerchantId(1);
                    req.setCategoryId(0);
                    req.setMinPrice(0);
                    req.setMaxPrice(999999999);
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("");
                    return productQueryRepo.findByMerchant(req);
                })
                .invoke(result -> assertThat(result.getData()).isNotEmpty())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreAllDeleted() {
        return productCommandRepo.restoreAllDeleted()
                .invoke(result -> assertThat(result).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteAllDeleted() {
        return productCommandRepo.deleteAllDeleted()
                .invoke(result -> assertThat(result).isNotNull())
                .replaceWithVoid();
    }
}
