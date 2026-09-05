package com.sanedge.common.seed;

import io.vertx.mutiny.sqlclient.Pool;
import org.jboss.logging.Logger;

import java.util.function.Function;

/**
 * Context passed to each {@link Seeder#seed(SeedContext)} invocation.
 *
 * @param pool           Vert.x reactive SQL pool (shared, injected by Quarkus)
 * @param log            Logger instance
 * @param passwordHasher Function to hash passwords (PBKDF2 via PasswordUtil)
 */
public record SeedContext(
        Pool pool,
        Logger log,
        Function<String, String> passwordHasher
) {
}
