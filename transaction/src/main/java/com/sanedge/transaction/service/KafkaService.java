package com.sanedge.transaction.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sanedge.common.observability.TracingMetrics;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KafkaService {
    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);

    @Inject
    Vertx vertx;

    @Inject
    com.sanedge.common.chaos.ChaosManager chaosManager;

    @Inject
    TracingMetrics tracingMetrics;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    @ConfigProperty(name = "kafka.acks", defaultValue = "all")
    String acks;

    @ConfigProperty(name = "kafka.enable.idempotence", defaultValue = "true")
    boolean enableIdempotence;

    @ConfigProperty(name = "kafka.retries", defaultValue = "10")
    int kafkaRetries;

    @ConfigProperty(name = "kafka.retry.backoff.ms", defaultValue = "500")
    long retryBackoffMs;

    @ConfigProperty(name = "kafka.max.in.flight.requests.per.connection", defaultValue = "5")
    int maxInFlightRequests;

    @ConfigProperty(name = "kafka.delivery.timeout.ms", defaultValue = "120000")
    int deliveryTimeoutMs;

    @ConfigProperty(name = "kafka.request.timeout.ms", defaultValue = "30000")
    int requestTimeoutMs;

    @ConfigProperty(name = "kafka.compression.type", defaultValue = "lz4")
    String compressionType;

    @ConfigProperty(name = "kafka.linger.ms", defaultValue = "5")
    int lingerMs;

    @ConfigProperty(name = "kafka.batch.size", defaultValue = "16384")
    int batchSize;

    @ConfigProperty(name = "kafka.max.block.ms", defaultValue = "60000")
    int maxBlockMs;

    private volatile KafkaProducer<String, String> producer;

    @PostConstruct
    void init() {
        validateProducerConfiguration();
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", acks);
        config.put("enable.idempotence", String.valueOf(enableIdempotence));
        config.put("retries", String.valueOf(kafkaRetries));
        config.put("retry.backoff.ms", String.valueOf(retryBackoffMs));
        config.put("max.in.flight.requests.per.connection", String.valueOf(maxInFlightRequests));
        config.put("delivery.timeout.ms", String.valueOf(deliveryTimeoutMs));
        config.put("request.timeout.ms", String.valueOf(requestTimeoutMs));
        // Throughput tuning: compression + batching reduce network bytes and
        // round trips for the email event traffic.
        config.put("compression.type", compressionType);
        config.put("linger.ms", String.valueOf(lingerMs));
        config.put("batch.size", String.valueOf(batchSize));
        // Upper bound for how long a send() may block waiting for metadata/partitions.
        config.put("max.block.ms", String.valueOf(maxBlockMs));
        producer = KafkaProducer.create(vertx, config);
        log.info("Kafka producer initialized. brokers={}, acks={}, idempotence={}", bootstrapServers, acks,
                enableIdempotence);
    }

    private void validateProducerConfiguration() {
        if (enableIdempotence && !"all".equalsIgnoreCase(acks)) {
            throw new IllegalStateException("Kafka idempotence requires acks=all");
        }
        if (enableIdempotence && (maxInFlightRequests < 1 || maxInFlightRequests > 5)) {
            throw new IllegalStateException("Kafka max.in.flight.requests.per.connection must be between 1 and 5 when idempotence is enabled");
        }
        if (kafkaRetries < 0 || retryBackoffMs < 0 || requestTimeoutMs <= 0 || deliveryTimeoutMs <= 0) {
            throw new IllegalStateException("Kafka retry and timeout settings must be positive");
        }
        if (lingerMs < 0 || batchSize <= 0) {
            throw new IllegalStateException("Kafka linger.ms must be >= 0 and batch.size must be > 0");
        }
    }

    @PreDestroy
    void destroy() {
        KafkaProducer<String, String> current = producer;
        producer = null;
        if (current != null) {
            current.close()
                    .onSuccess(v -> log.info("Kafka producer closed."))
                    .onFailure(err -> log.warn("Error closing Kafka producer: {}", err.getMessage()));
        }
    }

    public Uni<Void> sendMessage(String topic, String key, JsonObject payload) {
        if (payload == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Kafka payload cannot be null"));
        }
        JsonObject eventPayload = payload.copy()
                .put("event_id", UUID.randomUUID().toString())
                .put("schema_version", 1)
                .put("event_type", topic)
                .put("occurred_at", Instant.now().toString());
        return sendWithRetry(topic, key, eventPayload, Math.max(0, kafkaRetries), 0);
    }

    /**
     * Retries with exponential backoff, recording exactly one retry per actual
     * re-attempt ({@code kafka_retry_total} is precise: the final failure is
     * not counted as a retry).
     */
    private Uni<Void> sendWithRetry(String topic, String key, JsonObject payload, int remaining, int attempt) {
        Uni<Void> single = sendOnce(topic, key, payload);
        if (remaining <= 0) {
            return single;
        }
        return single
                .onFailure().invoke(error -> recordRetry())
                .onFailure().recoverWithUni(error -> Uni.createFrom().nullItem()
                        .onItem().delayIt().by(retryDelay(attempt))
                        .onItem().transformToUni(ignored -> sendWithRetry(topic, key, payload, remaining - 1,
                                attempt + 1)));
    }

    private Duration retryDelay(int attempt) {
        long base = Math.max(1, retryBackoffMs);
        long capped = Math.min(5000L, base << Math.min(12, Math.max(0, attempt)));
        return Duration.ofMillis(Math.max(1, capped));
    }

    public Uni<Void> sendExistingEvent(String topic, String key, JsonObject eventPayload) {
        if (eventPayload == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Kafka payload cannot be null"));
        }
        return sendOnce(topic, key, eventPayload);
    }

    public Vertx vertx() {
        return vertx;
    }

    private Uni<Void> sendOnce(String topic, String key, JsonObject payload) {
        KafkaProducer<String, String> current = producer;
        if (current == null) {
            return Uni.createFrom().failure(new IllegalStateException("Kafka producer not initialized"));
        }
        com.sanedge.common.chaos.ChaosPolicy policy = chaosManager.evaluate("kafka", topic);
        if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
            log.info("Injecting Kafka chaos policy={} topic={}", policy.getName(), topic);
            if (policy.getLatencyMs() > 0) {
                return Uni.createFrom().emitter(emitter -> vertx.setTimer(policy.getLatencyMs(), id -> {
                    if (policy.isDropMessage()) {
                        log.info("Kafka chaos dropped message topic={}", topic);
                        emitter.fail(new RuntimeException("Simulated Kafka drop"));
                    } else if (policy.isRejectMessage()) {
                        emitter.fail(new RuntimeException(policy.getErrorMessage() != null
                                ? policy.getErrorMessage() : "Simulated Kafka rejection"));
                    } else {
                        sendRecord(current, topic, key, payload).subscribe().with(emitter::complete, emitter::fail);
                    }
                }));
            }
            if (policy.isDropMessage()) {
                log.info("Kafka chaos dropped message topic={}", topic);
                return Uni.createFrom().failure(new RuntimeException("Simulated Kafka drop"));
            }
            if (policy.isRejectMessage()) {
                return Uni.createFrom().failure(new RuntimeException(policy.getErrorMessage() != null
                        ? policy.getErrorMessage() : "Simulated Kafka rejection"));
            }
        }
        return sendRecord(current, topic, key, payload);
    }

    private Uni<Void> sendRecord(KafkaProducer<String, String> current, String topic, String key,
            JsonObject payload) {
        KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, payload.encode());
        return Uni.createFrom().emitter(emitter -> current.send(record)
                .onSuccess(metadata -> {
                    if (tracingMetrics != null) {
                        tracingMetrics.recordKafkaPublish("success");
                    }
                    log.debug("Kafka sent topic={} partition={} offset={} event_id={}", topic,
                            metadata.getPartition(), metadata.getOffset(), payload.getString("event_id"));
                    emitter.complete(null);
                })
                .onFailure(err -> {
                    if (tracingMetrics != null) {
                        tracingMetrics.recordKafkaPublish("failure");
                    }
                    log.warn("Kafka send failed topic={} event_id={}: {}", topic, payload.getString("event_id"),
                            err.getMessage());
                    emitter.fail(err);
                }));
    }

    private void recordRetry() {
        if (tracingMetrics != null) {
            tracingMetrics.recordKafkaRetry();
        }
    }
}