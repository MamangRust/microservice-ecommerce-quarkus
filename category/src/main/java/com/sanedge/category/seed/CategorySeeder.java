package com.sanedge.category.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Seeds the ecommerce_catalog schema: 10 categories.
 * Order = 20.
 */
public class CategorySeeder implements Seeder {

    private static final List<String[]> CATEGORIES = List.of(
            new String[]{"Electronics", "Electronic devices and gadgets", "electronics"},
            new String[]{"Fashion", "Clothing, shoes, and accessories", "fashion"},
            new String[]{"Home & Living", "Furniture and home decor", "home-living"},
            new String[]{"Beauty", "Skincare, makeup, and beauty products", "beauty"},
            new String[]{"Sports", "Sports equipment and outdoor gear", "sports"},
            new String[]{"Books", "Books, magazines, and educational materials", "books"},
            new String[]{"Food & Beverage", "Food, snacks, and beverages", "food-beverage"},
            new String[]{"Automotive", "Automotive parts and accessories", "automotive"},
            new String[]{"Health", "Health supplements and wellness products", "health"},
            new String[]{"Toys", "Toys, games, and entertainment", "toys"}
    );

    @Override
    public String domain() {
        return "category";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        String sql = """
                INSERT INTO "categories" ("name", "description", "slug_category")
                VALUES ($1, $2, $3)
                ON CONFLICT ("id") DO NOTHING
                """;

        Uni<Void> chain = Uni.createFrom().voidItem();
        int id = 1;
        for (String[] cat : CATEGORIES) {
            final int catId = id++;
            chain = chain.chain(() -> pool.preparedQuery(sql)
                    .execute(Tuple.of(cat[0], cat[1], cat[2]))
                    .onItem().ignore().andContinueWithNull());
        }

        return chain.invoke(() -> log.infof("Categories seeded: %d categories", CATEGORIES.size()));
    }
}
