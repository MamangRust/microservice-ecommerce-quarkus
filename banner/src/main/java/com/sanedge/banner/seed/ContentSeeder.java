package com.sanedge.banner.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

/**
 * Seeds the ecommerce_content schema: banners and sliders.
 * Order = 50.
 */
public class ContentSeeder implements Seeder {

    @Override
    public String domain() {
        return "content";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        return seedSliders(pool, log)
                .chain(() -> seedBanners(pool, log))
                .replaceWithVoid();
    }

    private Uni<Void> seedSliders(io.vertx.mutiny.sqlclient.Pool pool, Logger log) {
        String sql = """
                INSERT INTO "sliders" ("name", "image")
                VALUES ($1, $2)
                ON CONFLICT ("id") DO NOTHING
                """;

        String[][] sliders = {
                {"Summer Sale", "slider-summer.png"},
                {"New Arrivals", "slider-new.png"},
                {"Free Shipping", "slider-shipping.png"},
                {"Flash Deal", "slider-flash.png"},
                {"Clearance", "slider-clearance.png"}
        };

        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String[] s : sliders) {
            chain = chain.chain(() -> pool.preparedQuery(sql)
                    .execute(Tuple.of(s[0], s[1]))
                    .onItem().ignore().andContinueWithNull());
        }

        return chain.invoke(() -> log.infof("Sliders seeded: %d sliders", sliders.length));
    }

    private Uni<Void> seedBanners(io.vertx.mutiny.sqlclient.Pool pool, Logger log) {
        String sql = """
                INSERT INTO "banners" ("name", "start_date", "end_date", "start_time", "end_time", "is_active")
                VALUES ($1, $2::date, $3::date, $4::time, $5::time, $6)
                ON CONFLICT ("id") DO NOTHING
                """;

        String[][] banners = {
                {"Electronics Week", "2024-01-01", "2024-01-31", "00:00:00", "23:59:59", "true"},
                {"Fashion Sale", "2024-02-01", "2024-02-28", "00:00:00", "23:59:59", "true"},
                {"Home Living Promo", "2024-03-01", "2024-03-31", "00:00:00", "23:59:59", "true"},
                {"Beauty Festival", "2024-04-01", "2024-04-30", "00:00:00", "23:59:59", "true"},
                {"Sports Month", "2024-05-01", "2024-05-31", "00:00:00", "23:59:59", "false"}
        };

        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String[] b : banners) {
            chain = chain.chain(() -> pool.preparedQuery(sql)
                    .execute(Tuple.of(b[0], b[1], b[2], b[3], b[4], Boolean.parseBoolean(b[5])))
                    .onItem().ignore().andContinueWithNull());
        }

        return chain.invoke(() -> log.infof("Banners seeded: %d banners", banners.length));
    }
}
