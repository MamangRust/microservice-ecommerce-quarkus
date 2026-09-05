package com.sanedge.cart.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the ecommerce_order schema: carts.
 * Order = 30.
 */
public class CartSeeder implements Seeder {

    @Override
    public String domain() {
        return "cart";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        String fetchUsersSql = "SELECT \"id\" FROM ecommerce_identity.users ORDER BY \"id\" LIMIT 3";
        String fetchProductsSql = "SELECT \"id\", \"name\", \"price\", \"image_product\" FROM ecommerce_catalog.products ORDER BY \"id\" LIMIT 5";

        return pool.preparedQuery(fetchUsersSql).execute()
                .onItem().transform(rows -> {
                    List<Integer> userIds = new ArrayList<>();
                    for (var row : rows) {
                        userIds.add(row.getInteger("id"));
                    }
                    return userIds;
                })
                .chain(userIds -> pool.preparedQuery(fetchProductsSql).execute()
                        .onItem().transform(rows -> {
                            List<Object[]> products = new ArrayList<>();
                            for (var row : rows) {
                                products.add(new Object[]{
                                        row.getInteger("id"),
                                        row.getString("name"),
                                        row.getInteger("price"),
                                        row.getString("image_product") != null ? row.getString("image_product") : "default.png"
                                });
                            }
                            return products;
                        })
                        .chain(products -> seedCartsSequential(pool, userIds, products, log)))
                .replaceWithVoid();
    }

    private Uni<Void> seedCartsSequential(io.vertx.mutiny.sqlclient.Pool pool,
                                           List<Integer> userIds,
                                           List<Object[]> products,
                                           Logger log) {
        // 7 params → use Tuple.from(List)
        String sql = """
                INSERT INTO "carts" ("user_id", "product_id", "name", "price", "image", "quantity", "weight")
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ("id") DO NOTHING
                """;

        Uni<Void> chain = Uni.createFrom().voidItem();
        int cartIdx = 0;
        for (int userId : userIds) {
            for (int pIdx = 0; pIdx < Math.min(2, products.size()); pIdx++) {
                Object[] product = products.get((cartIdx + pIdx) % products.size());
                final int qty = 1 + (cartIdx % 3);
                final int currentUserId = userId;
                chain = chain.chain(() -> {
                    Tuple params = Tuple.from(List.of(currentUserId, product[0], product[1], product[2], product[3], qty, 500));
                    return pool.preparedQuery(sql)
                            .execute(params)
                            .onItem().ignore().andContinueWithNull();
                });
                cartIdx++;
            }
        }

        return chain.invoke(() -> log.info("Carts seeded"));
    }
}
