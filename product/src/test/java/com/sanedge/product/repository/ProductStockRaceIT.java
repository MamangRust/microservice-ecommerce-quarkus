package com.sanedge.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.product.entity.Product;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.inject.Inject;

/**
 * Resilience integration test for the atomic stock guard in
 * {@link ProductCommandRepository#adjustStock}: with a real PostgreSQL, many
 * concurrent decrements must never drive {@code count_in_stock} below zero.
 *
 * <p>The concurrent statements use the reactive PostgreSQL pool directly. This
 * gives every operation its own pool connection and avoids sharing a Hibernate
 * Reactive session between concurrent operations on one Vert.x context, while
 * exercising the same atomic SQL predicate used by {@code adjustStock}.</p>
 */
@QuarkusTest
@QuarkusTestResource(value = PostgreSqlResource.class, restrictToAnnotatedClass = true)
@TestProfile(ProductStockRaceIT.RaceProfile.class)
@RunOnVertxContext
class ProductStockRaceIT {

    public static class RaceProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.datasource.reactive.max-size", "20");
        }
    }

    @Inject
    PgPool pgPool;

    @Inject
    ProductQueryRepository productQueryRepo;

    private Uni<Product> persistProduct(int stock, String name) {
        Product p = new Product();
        p.setMerchantId(1);
        p.setCategoryId(1);
        p.setName(name);
        p.setDescription("Stock race IT product");
        p.setPrice(100000);
        p.setCountInStock(stock);
        p.setBrand("RaceBrand");
        p.setWeight(100);
        p.setRating(4.5f);
        p.setSlugProduct(name);
        p.setImageProduct("http://example.com/" + name + ".jpg");
        p.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        p.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return Panache.withTransaction(() -> productQueryRepo.persist(p).replaceWith(p));
    }

    private Uni<RowSet<Row>> decrement(Long id) {
        return pgPool.preparedQuery("UPDATE products SET count_in_stock = count_in_stock - 1 "
                + "WHERE id = $1 AND deleted_at IS NULL AND count_in_stock - 1 >= 0")
                .execute(Tuple.of(id));
    }

    private Uni<Integer> currentStock(Long id) {
        return pgPool.preparedQuery("SELECT count_in_stock FROM products WHERE id = $1")
                .execute(Tuple.of(id))
                .map(rows -> rows.iterator().hasNext() ? rows.iterator().next().getInteger("count_in_stock") : null);
    }

    @Test
    void concurrentDecrementsNeverOversell(UniAsserter asserter) {
        Long[] id = new Long[1];
        Integer[] successes = new Integer[1];

        asserter.execute(() -> persistProduct(2, "race-product-a").invoke(p -> id[0] = p.id));
        asserter.execute(() -> {
            List<Uni<RowSet<Row>>> calls = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                calls.add(decrement(id[0]));
            }
            return Uni.combine().all().unis(calls).combinedWith(results -> {
                int ok = 0;
                for (Object result : results) {
                    if (((RowSet<?>) result).rowCount() == 1) {
                        ok++;
                    }
                }
                successes[0] = ok;
                return ok;
            });
        });
        asserter.assertThat(() -> Uni.createFrom().item(successes[0]),
                s -> assertThat(s).isEqualTo(2));
        asserter.assertThat(() -> currentStock(id[0]),
                stock -> assertThat(stock).isEqualTo(0));
    }

    @Test
    void rejectedDecrementDoesNotChangeStock(UniAsserter asserter) {
        Long[] id = new Long[1];

        asserter.execute(() -> persistProduct(1, "race-product-b").invoke(p -> id[0] = p.id));
        asserter.execute(() -> decrement(id[0]));
        asserter.assertThat(() -> decrement(id[0]),
                rejected -> assertThat(rejected.rowCount()).isZero());
        asserter.assertThat(() -> currentStock(id[0]),
                stock -> assertThat(stock).isEqualTo(0));
    }
}
