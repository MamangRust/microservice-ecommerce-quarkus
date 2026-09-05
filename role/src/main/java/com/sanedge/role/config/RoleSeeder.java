package com.sanedge.role.config;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.role.entity.Role;
import com.sanedge.role.repository.RoleRepository;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Keeps the base roles present so flows that resolve roles by name (auth
 * register, user creation with explicit roles, admin logins) never fail with
 * "Role not found".
 * <p>
 * Uses {@link Scheduled} (not a raw Vert.x timer) because scheduled methods run
 * on a duplicated Vert.x context, which reactive Panache/Redis calls require.
 * The name caches are deleted explicitly so a stale entry (e.g. pointing at a
 * recycled id from an older database state) cannot poison lookups.
 */
@ApplicationScoped
public class RoleSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSeeder.class);

    private static final List<String> BASE_ROLES = List.of("ROLE_USER", "ROLE_STAFF", "ROLE_ADMIN");

    @Inject
    RoleRepository roleRepository;

    @Inject
    RedisService redisService;

    // Returning Uni makes the scheduler execute this on the event loop (with a
    // duplicated context), which reactive Panache/Redis calls require.
    @Scheduled(delay = 5, delayUnit = TimeUnit.SECONDS, every = "24h")
    Uni<Void> seed() {
        return seedRoles()
                .invoke(() -> LOGGER.info("Base roles ensured and role name caches flushed: {}", BASE_ROLES))
                .onFailure().invoke(err -> LOGGER.error("Failed to seed base roles", err));
    }

    private Uni<Void> seedRoles() {
        // Sequential: parallel @WithTransaction calls on the shared reactive
        // session trip Hibernate's transaction-state stack.
        java.util.List<Uni<Void>> steps = new java.util.ArrayList<>();
        for (String name : BASE_ROLES) {
            steps.add(roleRepository.ensureBaseRole(name).replaceWithVoid());
        }
        // Slot-routed direct deletes are reliable in cluster mode (keys() by
        // pattern only scans the node receiving the command).
        for (String name : BASE_ROLES) {
            steps.add(redisService.deleteReactive("role:name:" + name));
        }
        steps.add(redisService.deleteByPatternReactive("role:*"));
        steps.add(redisService.deleteByPatternReactive("roles:user:*"));
        return chainSequential(steps);
    }

    private static Uni<Void> chainSequential(java.util.List<Uni<Void>> steps) {
        Uni<Void> result = Uni.createFrom().voidItem();
        for (Uni<Void> step : steps) {
            result = result.chain(v -> step);
        }
        return result;
    }
}
