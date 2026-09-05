package com.sanedge.product.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds the ecommerce_catalog schema: 20 products.
 * Order = 30.
 */
public class ProductSeeder implements Seeder {

    private static final String[][] PRODUCTS = {
            {"Laptop Pro 15", "electronics", "15000000", "50", "TechBrand", "2000", "laptop-pro-15"},
            {"Wireless Mouse", "electronics", "250000", "200", "LogiMax", "200", "wireless-mouse"},
            {"USB-C Hub", "electronics", "450000", "150", "TechBrand", "300", "usb-c-hub"},
            {"Smartphone X", "electronics", "8000000", "75", "PhoneCo", "180", "smartphone-x"},
            {"T-Shirt Basic", "fashion", "150000", "300", "FashionCo", "200", "t-shirt-basic"},
            {"Denim Jacket", "fashion", "500000", "80", "FashionCo", "800", "denim-jacket"},
            {"Running Shoes", "sports", "1200000", "120", "SportMax", "600", "running-shoes"},
            {"Yoga Mat", "sports", "350000", "200", "FitLife", "1500", "yoga-mat"},
            {"Coffee Table", "home-living", "2500000", "30", "HomeStyle", "15000", "coffee-table"},
            {"Desk Lamp", "home-living", "450000", "100", "LightMax", "800", "desk-lamp"},
            {"Face Cream", "beauty", "250000", "250", "GlowUp", "100", "face-cream"},
            {"Lipstick Set", "beauty", "350000", "180", "GlowUp", "50", "lipstick-set"},
            {"Programming Book", "books", "180000", "100", "TechPress", "500", "programming-book"},
            {"Recipe Book", "books", "120000", "80", "FoodPress", "400", "recipe-book"},
            {"Snack Box", "food-beverage", "85000", "500", "TastyCo", "300", "snack-box"},
            {"Coffee Beans", "food-beverage", "150000", "300", "BrewMax", "500", "coffee-beans"},
            {"Car Phone Mount", "automotive", "120000", "150", "AutoGear", "200", "car-phone-mount"},
            {"Vitamin C", "health", "95000", "400", "VitaLife", "100", "vitamin-c"},
            {"Building Blocks", "toys", "250000", "120", "PlayMax", "800", "building-blocks"},
            {"Remote Control Car", "toys", "450000", "60", "PlayMax", "500", "rc-car"}
    };

    @Override
    public String domain() {
        return "product";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        String fetchMerchantSql = "SELECT \"id\" FROM ecommerce_merchant.merchants ORDER BY \"id\" LIMIT 1";

        return pool.preparedQuery(fetchMerchantSql).execute()
                .onItem().transform(rows -> {
                    if (rows.rowCount() > 0) {
                        return rows.iterator().next().getInteger("id");
                    }
                    return 1;
                })
                .chain(merchantId -> {
                    String fetchCategorySql = "SELECT \"id\", \"slug_category\" FROM ecommerce_catalog.categories ORDER BY \"id\"";
                    return pool.preparedQuery(fetchCategorySql).execute()
                            .onItem().transform(rows -> {
                                Map<String, Integer> categoryMap = new HashMap<>();
                                for (var row : rows) {
                                    categoryMap.put(row.getString("slug_category"), row.getInteger("id"));
                                }
                                return categoryMap;
                            })
                            .chain(categoryMap -> seedProductsSequential(pool, merchantId, categoryMap, log));
                })
                .replaceWithVoid();
    }

    private Uni<Void> seedProductsSequential(io.vertx.mutiny.sqlclient.Pool pool,
                                              int merchantId,
                                              Map<String, Integer> categoryMap,
                                              Logger log) {
        // Use positional params (?) with Tuple.from(List) since we have 9 params (Tuple.of max 6)
        String insertSql = """
                INSERT INTO "products" ("merchant_id", "category_id", "name", "description", "price", "count_in_stock", "brand", "weight", "slug_product")
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ("id") DO NOTHING
                """;

        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String[] p : PRODUCTS) {
            Integer categoryId = categoryMap.get(p[1]);
            if (categoryId == null) categoryId = 1;
            int catId = categoryId;
            chain = chain.chain(() -> {
                Tuple params = Tuple.from(List.of(
                        merchantId, catId, p[0], "Description for " + p[0],
                        Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                        p[4], Integer.parseInt(p[5]), p[6]
                ));
                return pool.preparedQuery(insertSql)
                        .execute(params)
                        .onItem().ignore().andContinueWithNull();
            });
        }

        return chain.invoke(() -> log.infof("Products seeded: %d products", PRODUCTS.length));
    }
}
