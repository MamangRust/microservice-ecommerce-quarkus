package com.sanedge.user.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the ecommerce_identity schema: roles, users, and user_roles.
 * Order = 10 (first to run).
 * All inserts use ON CONFLICT for idempotency.
 */
public class IdentitySeeder implements Seeder {

    private static final List<String> ROLES = List.of(
            "Super Admin", "Admin", "Merchant Admin", "Merchant Operator",
            "Finance", "Compliance", "Auditor", "Support", "Viewer", "User"
    );

    @Override
    public String domain() {
        return "identity";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        var pool = ctx.pool();

        return seedRoles(pool, log)
                .chain(() -> seedUsers(pool, ctx.passwordHasher(), log))
                .chain(userIds -> seedUserRoles(pool, userIds, log))
                .replaceWithVoid();
    }

    private Uni<Void> seedRoles(io.vertx.mutiny.sqlclient.Pool pool, Logger log) {
        String sql = """
                INSERT INTO "roles" ("role_name") VALUES ($1)
                ON CONFLICT ("role_name") DO NOTHING
                """;

        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String role : ROLES) {
            chain = chain.chain(() -> pool.preparedQuery(sql)
                    .execute(Tuple.of(role))
                    .onItem().ignore().andContinueWithNull());
        }

        return chain.invoke(() -> log.infof("Roles seeded: %d roles", ROLES.size()));
    }

    private Uni<List<Integer>> seedUsers(io.vertx.mutiny.sqlclient.Pool pool,
                                          java.util.function.Function<String, String> passwordHasher,
                                          Logger log) {
        String insertSql = """
                INSERT INTO "users" ("firstname", "lastname", "username", "email", "password")
                VALUES ($1, $2, $3, $4, $5)
                ON CONFLICT ("username") DO UPDATE SET "email" = EXCLUDED."email"
                """;

        String[][] users = {
                {"Admin", "System", "admin", "admin@example.com"},
                {"Staff", "Operator", "staff", "staff@example.com"},
                {"John", "Doe", "user", "user@example.com"}
        };

        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String[] u : users) {
            String hashed = passwordHasher.apply("password");
            chain = chain.chain(() -> pool.preparedQuery(insertSql)
                    .execute(Tuple.of(u[0], u[1], u[2], u[3], hashed))
                    .onItem().ignore().andContinueWithNull());
        }

        // Fetch user IDs
        return chain.chain(() -> fetchUserIds(pool, List.of("admin", "staff", "user")))
                .invoke(userIds -> log.infof("Users seeded: %d users, IDs: %s", users.length, userIds));
    }

    private Uni<List<Integer>> fetchUserIds(io.vertx.mutiny.sqlclient.Pool pool, List<String> usernames) {
        String sql = "SELECT \"id\" FROM \"users\" WHERE \"username\" = $1";
        Uni<List<Integer>> chain = Uni.createFrom().item(new ArrayList<>());

        for (String username : usernames) {
            chain = chain.chain(ids -> pool.preparedQuery(sql).execute(Tuple.of(username))
                    .onItem().transform(rows -> {
                        if (rows.rowCount() > 0) {
                            ids.add(rows.iterator().next().getInteger("id"));
                        }
                        return ids;
                    }));
        }

        return chain;
    }

    private Uni<Void> seedUserRoles(io.vertx.mutiny.sqlclient.Pool pool,
                                     List<Integer> userIds, Logger log) {
        if (userIds.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        String sql = """
                INSERT INTO "user_roles" ("user_id", "role_id")
                SELECT $1, "id" FROM "roles" WHERE "role_name" = $2
                ON CONFLICT ("user_id", "role_id") DO NOTHING
                """;

        String[] roleNames = {"Admin", "Merchant Admin", "User"};
        Uni<Void> chain = Uni.createFrom().voidItem();

        for (int i = 0; i < Math.min(userIds.size(), roleNames.length); i++) {
            final int userId = userIds.get(i);
            final String roleName = roleNames[i];
            chain = chain.chain(() -> pool.preparedQuery(sql)
                    .execute(Tuple.of(userId, roleName))
                    .onItem().ignore().andContinueWithNull());
        }

        return chain.invoke(() -> log.infof("User roles assigned: %d users", Math.min(userIds.size(), roleNames.length)));
    }
}
