package com.sanedge.review.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the ecommerce_content schema: reviews and review_details.
 * Order = 50.
 */
public class ReviewSeeder implements Seeder {

    private static final String[][] REVIEWS = {
            {"1", "1", "Admin", "Great laptop, fast performance!", "5"},
            {"2", "1", "Staff", "Good value for money", "4"},
            {"3", "2", "User", "Nice mouse, comfortable to use", "4"},
            {"1", "3", "Admin", "Useful hub, all ports work", "5"},
            {"2", "4", "Staff", "Excellent phone, great camera", "5"},
            {"3", "5", "User", "Basic but comfortable shirt", "3"},
            {"1", "7", "Admin", "Perfect running shoes", "5"},
            {"2", "8", "Staff", "Good yoga mat, non-slip", "4"},
            {"3", "9", "User", "Sturdy table, easy assembly", "4"},
            {"1", "11", "Admin", "Great face cream, skin feels soft", "5"}
    };

    @Override
    public String domain() {
        return "review";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        // Fetch user IDs
        String fetchUsersSql = "SELECT \"id\" FROM ecommerce_identity.users ORDER BY \"id\" LIMIT 3";
        String fetchProductsSql = "SELECT \"id\" FROM ecommerce_catalog.products ORDER BY \"id\" LIMIT 10";

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
                            List<Integer> productIds = new ArrayList<>();
                            for (var row : rows) {
                                productIds.add(row.getInteger("id"));
                            }
                            return productIds;
                        })
                        .chain(productIds -> seedReviewsSequential(pool, userIds, productIds, log)))
                .replaceWithVoid();
    }

    private Uni<List<Integer>> seedReviewsSequential(io.vertx.mutiny.sqlclient.Pool pool,
                                                      List<Integer> userIds,
                                                      List<Integer> productIds,
                                                      Logger log) {
        String insertReviewSql = """
                INSERT INTO "reviews" ("user_id", "product_id", "name", "comment", "rating")
                VALUES ($1, $2, $3, $4, $5)
                ON CONFLICT ("id") DO NOTHING
                RETURNING "id"
                """;

        Uni<List<Integer>> chain = Uni.createFrom().item(new ArrayList<Integer>());

        for (String[] r : REVIEWS) {
            int userId = userIds.get((Integer.parseInt(r[0]) - 1) % userIds.size());
            int productId = productIds.get((Integer.parseInt(r[1]) - 1) % productIds.size());

            chain = chain.chain(ids -> pool.preparedQuery(insertReviewSql)
                    .execute(Tuple.of(userId, productId, r[2], r[3], Integer.parseInt(r[4])))
                    .onItem().transform(rows -> {
                        if (rows.rowCount() > 0) {
                            ids.add(rows.iterator().next().getInteger("id"));
                        }
                        return ids;
                    }));
        }

        // Add review details for first 5 reviews
        return chain.chain(ids -> {
            String insertDetailSql = """
                    INSERT INTO "review_details" ("review_id", "type", "url", "caption")
                    VALUES ($1, $2, $3, $4)
                    ON CONFLICT ("id") DO NOTHING
                    """;

            Uni<List<Integer>> detailChain = Uni.createFrom().item(ids);
            for (int i = 0; i < Math.min(5, ids.size()); i++) {
                final int reviewId = ids.get(i);
                final String caption = "Photo " + (i + 1);
                detailChain = detailChain.chain(currentIds -> pool.preparedQuery(insertDetailSql)
                        .execute(Tuple.of(reviewId, "photo", "https://example.com/review/" + reviewId + ".jpg", caption))
                        .onItem().transform(r -> currentIds));
            }
            return detailChain;
        }).invoke(ids -> log.infof("Reviews seeded: %d reviews", REVIEWS.length));
    }
}
