package com.sanedge.order.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the ecommerce_order schema: orders, order_items, and shipping_addresses.
 * Order = 30.
 * Uses specific created_at dates to make stats deterministic.
 */
public class OrderSeeder implements Seeder {

    private static final String[][] ORDERS = {
            {"1", "1", "15000000", "2024-01-15 10:30:00"},
            {"1", "1", "700000", "2024-01-20 14:00:00"},
            {"1", "2", "500000", "2024-02-05 09:00:00"},
            {"2", "1", "8000000", "2024-02-10 11:30:00"},
            {"2", "3", "2500000", "2024-03-01 16:00:00"},
            {"3", "1", "450000", "2024-03-15 13:00:00"},
            {"3", "2", "1200000", "2024-04-01 10:00:00"},
            {"1", "4", "350000", "2024-04-20 15:30:00"},
            {"2", "5", "250000", "2024-05-01 08:00:00"},
            {"3", "1", "15000000", "2024-05-15 12:00:00"}
    };

    @Override
    public String domain() {
        return "order";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        return seedOrders(pool, log)
                .chain(orderIds -> seedOrderItems(pool, orderIds, log))
                .chain(orderIds -> seedShippingAddresses(pool, orderIds, log))
                .replaceWithVoid();
    }

    private Uni<List<Integer>> seedOrders(io.vertx.mutiny.sqlclient.Pool pool, Logger log) {
        String sql = """
                INSERT INTO "orders" ("user_id", "merchant_id", "total_price", "created_at", "updated_at")
                VALUES ($1, $2, $3, $4::timestamp, $4::timestamp)
                ON CONFLICT ("id") DO NOTHING
                """;

        List<Integer> ids = new ArrayList<>();

        Uni<List<Integer>> chain = Uni.createFrom().item(ids);

        for (String[] o : ORDERS) {
            chain = chain.chain(currentIds -> pool.preparedQuery(sql)
                    .execute(Tuple.of(
                            Integer.parseInt(o[0]), Integer.parseInt(o[1]),
                            Integer.parseInt(o[2]), o[3]
                    ))
                    .onItem().transform(r -> {
                        currentIds.add(0);
                        return (List<Integer>) currentIds;
                    }));
        }

        return chain.chain(currentIds -> {
            String fetchSql = "SELECT \"id\" FROM \"orders\" ORDER BY \"id\"";
            return pool.preparedQuery(fetchSql).execute()
                    .onItem().transform(rows -> {
                        List<Integer> realIds = new ArrayList<>();
                        for (var row : rows) {
                            realIds.add(row.getInteger("id"));
                        }
                        return (List<Integer>) realIds;
                    });
        }).invoke(realIds -> log.infof("Orders seeded: %d orders", realIds.size()));
    }

    private Uni<List<Integer>> seedOrderItems(io.vertx.mutiny.sqlclient.Pool pool,
                                               List<Integer> orderIds, Logger log) {
        String fetchProductsSql = "SELECT \"id\", \"price\" FROM ecommerce_catalog.products ORDER BY \"id\" LIMIT 10";
        String insertSql = """
                INSERT INTO "order_items" ("order_id", "product_id", "quantity", "price", "created_at")
                VALUES ($1, $2, $3, $4, $5::timestamp)
                ON CONFLICT ("id") DO NOTHING
                """;

        return pool.preparedQuery(fetchProductsSql).execute()
                .chain(productRows -> {
                    List<int[]> products = new ArrayList<>();
                    for (var row : productRows) {
                        products.add(new int[]{row.getInteger("id"), row.getInteger("price")});
                    }

                    List<Integer> ids = new ArrayList<>();
                    Uni<List<Integer>> chain = Uni.createFrom().item(ids);
                    int orderIdx = 0;
                    for (int orderId : orderIds) {
                        if (orderId == 0) continue;
                        int productIdx = orderIdx % products.size();
                        int[] product = products.get(productIdx);
                        int quantity = 1 + (orderIdx % 3);
                        String createdAt = ORDERS[Math.min(orderIdx, ORDERS.length - 1)][3];

                        chain = chain.chain(currentIds -> pool.preparedQuery(insertSql)
                                .execute(Tuple.of(orderId, product[0], quantity, product[1] * quantity, createdAt))
                                .onItem().transform(r -> {
                                    currentIds.add(orderId);
                                    return (List<Integer>) currentIds;
                                }));
                        orderIdx++;
                    }
                    return chain;
                }).invoke(ids -> log.infof("Order items seeded: %d items", ids.size()));
    }

    private Uni<List<Integer>> seedShippingAddresses(io.vertx.mutiny.sqlclient.Pool pool,
                                                      List<Integer> orderIds, Logger log) {
        String sql = """
                INSERT INTO "shipping_addresses" ("order_id", "alamat", "provinsi", "negara", "kota", "courier", "shipping_method", "shipping_cost")
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ("id") DO NOTHING
                """;

        String[] provinces = {"DKI Jakarta", "Jawa Barat", "Jawa Timur", "Bali", "Sumatera Utara"};
        String[] cities = {"Jakarta Pusat", "Bandung", "Surabaya", "Denpasar", "Medan"};

        List<Integer> ids = new ArrayList<>();
        Uni<List<Integer>> chain = Uni.createFrom().item(ids);

        int idx = 0;
        for (int orderId : orderIds) {
            if (orderId == 0) continue;
            final int i = idx % provinces.length;
            final int currentOrderId = orderId;
            final int cost = 15000 + (i * 5000);
            chain = chain.chain(currentIds -> {
                Tuple params = Tuple.from(List.of(currentOrderId, "Jl. Sudirman No. 1", provinces[i], "Indonesia", cities[i], "JNE", "Regular", cost));
                return pool.preparedQuery(sql)
                        .execute(params)
                        .onItem().transform(r -> {
                            currentIds.add(currentOrderId);
                            return (List<Integer>) currentIds;
                        });
            });
            idx++;
        }

        return chain.invoke(currentIds -> log.infof("Shipping addresses seeded: %d", currentIds.size()));
    }
}
