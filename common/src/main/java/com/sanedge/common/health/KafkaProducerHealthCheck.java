package com.sanedge.common.health;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dependency-aware readiness check for Kafka broker connectivity.
 *
 * <p>Only registered in modules that opt in via
 * {@code kafka.health.enabled=true} (auth, merchant, transaction,
 * email-service). Modules that do not use Kafka are never affected.</p>
 *
 * <p>Uses a Kafka {@link AdminClient} to resolve the cluster id with a short
 * timeout instead of relying on producer lazily connecting on first send.</p>
 */
@Readiness
@ApplicationScoped
@IfBuildProperty(name = "kafka.health.enabled", stringValue = "true")
public class KafkaProducerHealthCheck implements HealthCheck {

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    @ConfigProperty(name = "kafka.health.timeout.ms", defaultValue = "3000")
    long timeoutMs;

    @Override
    public HealthCheckResponse call() {
        long effectiveTimeout = Math.max(500, timeoutMs);
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Kafka expects request.timeout.ms as a 32-bit Integer, while the
        // MicroProfile config value is represented as a long.
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                (int) Math.min(Integer.MAX_VALUE, effectiveTimeout));
        try (AdminClient admin = AdminClient.create(config)) {
            String clusterId = admin.describeCluster().clusterId()
                    .get(effectiveTimeout, TimeUnit.MILLISECONDS);
            return HealthCheckResponse.named("kafka-broker-connectivity")
                    .up()
                    .withData("brokers", bootstrapServers)
                    .withData("cluster_id", clusterId == null ? "unknown" : clusterId)
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("kafka-broker-connectivity")
                    .down()
                    .withData("brokers", bootstrapServers)
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
