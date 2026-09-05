package com.sanedge.common.test;

import java.util.Map;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts a real Kafka broker (Testcontainers, KRaft mode) and injects
 * {@code kafka.bootstrap.servers} so integration tests exercise the actual
 * producer/consumer/outbox code paths against a real broker.
 */
public class KafkaResource implements QuarkusTestResourceLifecycleManager {

    // 7.6.0 is used (instead of a newer tag) because it is already cached on the
    // local Docker daemon; pulling a brand-new tag stalls on slow connections.
    static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.6.0");

    private KafkaContainer kafka;

    @Override
    public Map<String, String> start() {
        kafka = new KafkaContainer(KAFKA_IMAGE);
        kafka.start();
        return Map.of("kafka.bootstrap.servers", kafka.getBootstrapServers());
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
    }
}
