package com.sanedge.transaction.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Seeds the ecommerce_transaction schema: transactions.
 * Order = 40.
 */
public class TransactionSeeder implements Seeder {

    private static final String[] PAYMENT_METHODS = {"Bank Transfer", "Credit Card", "E-Wallet", "COD"};

    @Override
    public String domain() {
        return "transaction";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        // Fetch order IDs and their prices
        String fetchOrdersSql = "SELECT \"id\", \"total_price\", \"merchant_id\" FROM ecommerce_order.orders ORDER BY \"id\"";

        return pool.preparedQuery(fetchOrdersSql).execute()
                .chain(rows -> {
                    java.util.List<Object[]> orders = new java.util.ArrayList<>();
                    for (var row : rows) {
                        orders.add(new Object[]{
                                row.getInteger("id"),
                                row.getInteger("total_price"),
                                row.getInteger("merchant_id")
                        });
                    }

                    String sql = """
                            INSERT INTO "transactions" ("order_id", "merchant_id", "payment_method", "amount", "payment_status", "created_at", "updated_at")
                            VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
                            ON CONFLICT ("id") DO NOTHING
                            """;

                    Uni<Void> chain = Uni.createFrom().voidItem();
                    int idx = 0;
                    for (Object[] order : orders) {
                        String method = PAYMENT_METHODS[idx % PAYMENT_METHODS.length];
                        String status = idx % 5 == 0 ? "FAILED" : "SUCCESS";
                        final int i = idx;
                        chain = chain.chain(() -> pool.preparedQuery(sql)
                                .execute(Tuple.of(order[0], order[2], method, order[1], status))
                                .onItem().ignore().andContinueWithNull());
                        idx++;
                    }

                    return chain;
                }).invoke(() -> log.info("Transactions seeded"));
    }
}
