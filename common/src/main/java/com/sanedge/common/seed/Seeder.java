package com.sanedge.common.seed;

import io.smallrye.mutiny.Uni;

/**
 * SPI interface for per-domain seeders.
 * <p>
 * Each domain module provides an implementation registered via
 * {@code META-INF/services/com.sanedge.common.seed.Seeder}.
 * The seeder orchestrator loads all implementations via {@link java.util.ServiceLoader},
 * sorts them by {@link #order()}, and runs them sequentially.
 */
public interface Seeder {

    /**
     * Domain name used for filtering via SEED_DOMAINS env var.
     * Must match the value used in the orchestrator filter.
     */
    String domain();

    /**
     * Execution order. Lower values run first.
     * <ul>
     *   <li>10 = identity (roles, users, user_roles)</li>
     *   <li>20 = category, merchant</li>
     *   <li>30 = product, order, cart</li>
     *   <li>40 = transaction</li>
     *   <li>50 = review, banner (content)</li>
     * </ul>
     */
    default int order() {
        return 100;
    }

    /**
     * Execute the seed operation. Must be idempotent — running multiple times
     * should not produce duplicates (use ON CONFLICT in SQL).
     *
     * @param ctx the seed context containing the reactive pool and utilities
     * @return Uni that completes when seeding is done
     */
    Uni<Void> seed(SeedContext ctx);
}
