package com.sanedge.common.test;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class PostgreSqlResource implements QuarkusTestResourceLifecycleManager {

    private GenericContainer<?> postgres;

    @Override
    public Map<String, String> start() {
        postgres = new GenericContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withEnv("POSTGRES_USER", "test")
                .withEnv("POSTGRES_PASSWORD", "test")
                .withEnv("POSTGRES_DB", "test")
                .withExposedPorts(5432)
                .waitingFor(Wait.forListeningPort());

        postgres.start();

        int port = postgres.getMappedPort(5432);
        String host = postgres.getHost();

        // Point BOTH the reactive and JDBC URLs at the test container. Modules that
        // include quarkus-jdbc-postgresql (e.g. transaction) run Hibernate DDL through
        // the Agroal JDBC pool; without the JDBC URL it would fall back to localhost:5432
        // and fail with password authentication errors against the dev database.
        return Map.of(
                "quarkus.datasource.username", "test",
                "quarkus.datasource.password", "test",
                "quarkus.datasource.reactive.url", "postgresql://" + host + ":" + port + "/test",
                "quarkus.datasource.jdbc.url", "jdbc:postgresql://" + host + ":" + port + "/test"
        );
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
