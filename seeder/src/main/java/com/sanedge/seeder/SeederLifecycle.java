package com.sanedge.seeder;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import com.sanedge.common.utils.PasswordUtil;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeder orchestrator lifecycle.
 * <p>
 * On startup:
 * 1. Loads all {@link Seeder} implementations via {@link ServiceLoader}
 * 2. Filters by SEED_DOMAINS env var (comma-separated, or "all"/empty for all)
 * 3. Sorts by {@link Seeder#order()}
 * 4. Runs each seeder sequentially
 * 5. Exits with code 0 (success) or 1 (failure)
 */
@ApplicationScoped
public class SeederLifecycle {

    private static final Logger LOGGER = Logger.getLogger(SeederLifecycle.class);

    @Inject
    Pool pool;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("=== Seeder Orchestrator starting ===");

        // Load all seeders via ServiceLoader
        List<Seeder> allSeeders = ServiceLoader.load(Seeder.class).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparingInt(Seeder::order))
                .toList();

        LOGGER.infof("Loaded %d seeders via ServiceLoader", allSeeders.size());

        // Filter by SEED_DOMAINS env var
        String seedDomainsEnv = System.getenv("SEED_DOMAINS");
        Set<String> allowedDomains = parseSeedDomains(seedDomainsEnv);

        List<Seeder> seeders = allSeeders.stream()
                .filter(s -> allowedDomains.isEmpty() || allowedDomains.contains(s.domain()))
                .toList();

        if (seeders.isEmpty()) {
            LOGGER.warn("No seeders matched SEED_DOMAINS filter. Nothing to seed.");
            Quarkus.asyncExit(0);
            return;
        }

        LOGGER.infof("Will run %d seeders: %s",
                seeders.size(),
                seeders.stream().map(s -> s.domain() + " (" + s.order() + ")").collect(Collectors.joining(", ")));

        // Create SeedContext
        PasswordUtil passwordUtil = new PasswordUtil();
        SeedContext ctx = new SeedContext(
                pool,
                LOGGER,
                passwordUtil::hashPassword
        );

        // Run seeders sequentially
        runSeeders(seeders, ctx, 0);
    }

    private void runSeeders(List<Seeder> seeders, SeedContext ctx, int index) {
        if (index >= seeders.size()) {
            LOGGER.info("=== All seeders completed successfully ===");
            Quarkus.asyncExit(0);
            return;
        }

        Seeder seeder = seeders.get(index);
        LOGGER.infof("Running seeder [%d/%d]: %s (order=%d)",
                index + 1, seeders.size(), seeder.domain(), seeder.order());

        seeder.seed(ctx)
                .onItem().invoke(() -> {
                    LOGGER.infof("Seeder [%d/%d] %s completed successfully", index + 1, seeders.size(), seeder.domain());
                    runSeeders(seeders, ctx, index + 1);
                })
                .onFailure().invoke(failure -> {
                    LOGGER.errorf(failure, "Seeder [%d/%d] %s FAILED", index + 1, seeders.size(), seeder.domain());
                    Quarkus.asyncExit(1);
                })
                .subscribe().with(
                        item -> { /* handled by onItem */ },
                        failure -> { /* handled by onFailure */ }
                );
    }

    /**
     * Parse SEED_DOMAINS env var.
     * Accepts: "identity,merchant" or "all" or empty (all).
     */
    private Set<String> parseSeedDomains(String envValue) {
        if (envValue == null || envValue.isBlank() || "all".equalsIgnoreCase(envValue.trim())) {
            return Set.of(); // empty = all seeders
        }
        return Arrays.stream(envValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
