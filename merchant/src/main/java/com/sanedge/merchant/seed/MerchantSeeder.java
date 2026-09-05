package com.sanedge.merchant.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the ecommerce_merchant schema: merchants and sub-entities.
 * Order = 20.
 */
public class MerchantSeeder implements Seeder {

    private static final String[][] MERCHANTS = {
            {"Tech Store", "Best electronics store", "Jakarta", "tech@example.com", "081234567890", "APPROVED"},
            {"Fashion Hub", "Trendy fashion outlet", "Bandung", "fashion@example.com", "081234567891", "APPROVED"},
            {"Home Decor", "Quality home furniture", "Surabaya", "home@example.com", "081234567892", "APPROVED"},
            {"Beauty World", "Premium beauty products", "Yogyakarta", "beauty@example.com", "081234567893", "APPROVED"},
            {"Sports Zone", "Sports equipment specialist", "Semarang", "sports@example.com", "081234567894", "APPROVED"}
    };

    @Override
    public String domain() {
        return "merchant";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        String fetchUsersSql = "SELECT \"id\" FROM ecommerce_identity.users ORDER BY \"id\" LIMIT 5";

        return pool.preparedQuery(fetchUsersSql).execute()
                .onItem().transform(rows -> {
                    List<Integer> userIds = new ArrayList<>();
                    for (var row : rows) {
                        userIds.add(row.getInteger("id"));
                    }
                    return userIds;
                })
                .chain(userIds -> seedMerchantsSequential(pool, userIds, log))
                .chain(merchantIds -> seedMerchantDetails(pool, merchantIds, log))
                .chain(merchantIds -> seedAwards(pool, merchantIds, log))
                .chain(merchantIds -> seedPolicies(pool, merchantIds, log))
                .chain(merchantIds -> seedBusinessInfo(pool, merchantIds, log))
                .replaceWithVoid();
    }

    private Uni<List<Integer>> seedMerchantsSequential(io.vertx.mutiny.sqlclient.Pool pool,
                                                        List<Integer> userIds, Logger log) {
        String insertSql = """
                INSERT INTO "merchants" ("user_id", "name", "description", "address", "contact_email", "contact_phone", "status")
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ("id") DO NOTHING
                """;

        Uni<List<Integer>> chain = Uni.createFrom().item(new ArrayList<Integer>());

        for (int i = 0; i < Math.min(userIds.size(), MERCHANTS.length); i++) {
            int userId = userIds.get(i);
            String[] m = MERCHANTS[i];
            chain = chain.chain(ids -> {
                Tuple params = Tuple.from(List.of(userId, m[0], m[1], m[2], m[3], m[4], m[5]));
                return pool.preparedQuery(insertSql)
                        .execute(params)
                        .onItem().transform(r -> {
                            ids.add(userId);
                            return ids;
                        });
            });
        }

        return chain.invoke(ids -> log.infof("Merchants seeded: %d merchants", ids.size()));
    }

    private Uni<List<Integer>> seedMerchantDetails(io.vertx.mutiny.sqlclient.Pool pool,
                                                     List<Integer> merchantIds, Logger log) {
        String sql = """
                INSERT INTO "merchant_details" ("merchant_id", "display_name", "short_description")
                VALUES ($1, $2, $3)
                ON CONFLICT ("id") DO NOTHING
                """;

        Uni<List<Integer>> chain = Uni.createFrom().item(new ArrayList<Integer>());

        for (int merchantId : merchantIds) {
            chain = chain.chain(ids -> pool.preparedQuery(sql)
                    .execute(Tuple.of(merchantId, "Store #" + merchantId, "Description for merchant " + merchantId))
                    .onItem().transform(r -> {
                        ids.add(merchantId);
                        return ids;
                    }));
        }

        return chain.invoke(ids -> log.infof("Merchant details seeded: %d", ids.size()));
    }

    private Uni<List<Integer>> seedAwards(io.vertx.mutiny.sqlclient.Pool pool,
                                           List<Integer> merchantIds, Logger log) {
        String sql = """
                INSERT INTO "merchant_certifications_and_awards" ("merchant_id", "title", "description")
                VALUES ($1, $2, $3)
                ON CONFLICT ("id") DO NOTHING
                """;

        Uni<List<Integer>> chain = Uni.createFrom().item(new ArrayList<Integer>());

        for (int merchantId : merchantIds) {
            chain = chain.chain(ids -> pool.preparedQuery(sql)
                    .execute(Tuple.of(merchantId, "Best Seller " + merchantId, "Award for outstanding sales"))
                    .onItem().transform(r -> {
                        ids.add(merchantId);
                        return ids;
                    }));
        }

        return chain.invoke(ids -> log.infof("Merchant awards seeded: %d", ids.size()));
    }

    private Uni<List<Integer>> seedPolicies(io.vertx.mutiny.sqlclient.Pool pool,
                                             List<Integer> merchantIds, Logger log) {
        String sql = """
                INSERT INTO "merchant_policies" ("merchant_id", "policy_type", "title", "description")
                VALUES ($1, $2, $3, $4)
                ON CONFLICT ("id") DO NOTHING
                """;

        Uni<List<Integer>> chain = Uni.createFrom().item(new ArrayList<Integer>());

        for (int merchantId : merchantIds) {
            chain = chain.chain(ids -> pool.preparedQuery(sql)
                    .execute(Tuple.of(merchantId, "Return", "Return Policy", "30-day return policy for all products"))
                    .onItem().transform(r -> {
                        ids.add(merchantId);
                        return ids;
                    }));
        }

        return chain.invoke(ids -> log.infof("Merchant policies seeded: %d", ids.size()));
    }

    private Uni<List<Integer>> seedBusinessInfo(io.vertx.mutiny.sqlclient.Pool pool,
                                                 List<Integer> merchantIds, Logger log) {
        String sql = """
                INSERT INTO "merchant_business_information" ("merchant_id", "business_type", "tax_id", "established_year")
                VALUES ($1, $2, $3, $4)
                ON CONFLICT ("id") DO NOTHING
                """;

        Uni<List<Integer>> chain = Uni.createFrom().item(new ArrayList<Integer>());

        for (int idx = 0; idx < merchantIds.size(); idx++) {
            int merchantId = merchantIds.get(idx);
            int year = 2020 + idx;
            String taxId = "1234567890" + idx;
            chain = chain.chain(ids -> pool.preparedQuery(sql)
                    .execute(Tuple.of(merchantId, "PT", taxId, year))
                    .onItem().transform(r -> {
                        ids.add(merchantId);
                        return ids;
                    }));
        }

        return chain.invoke(ids -> log.infof("Merchant business info seeded: %d", ids.size()));
    }
}
