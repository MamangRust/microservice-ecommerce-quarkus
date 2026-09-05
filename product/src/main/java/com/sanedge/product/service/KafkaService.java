package com.sanedge.product.service;

import java.util.HashMap;
import java.util.Map;

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

    @Inject Vertx vertx;
    @Inject TracingMetrics tracingMetrics;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092") String bootstrapServers;
    @ConfigProperty(name = "kafka.acks", defaultValue = "all") String acks;
    @ConfigProperty(name = "kafka.enable.idempotence", defaultValue = "true") boolean enableIdempotence;
    @ConfigProperty(name = "kafka.retries", defaultValue = "10") int kafkaRetries;
    @ConfigProperty(name = "kafka.retry.backoff.ms", defaultValue = "500") long retryBackoffMs;
    @ConfigProperty(name = "kafka.max.in.flight.requests.per.connection", defaultValue = "5") int maxInFlightRequests;
    @ConfigProperty(name = "kafka.delivery.timeout.ms", defaultValue = "120000") int deliveryTimeoutMs;
    @ConfigProperty(name = "kafka.request.timeout.ms", defaultValue = "30000") int requestTimeoutMs;
    @ConfigProperty(name = "kafka.compression.type", defaultValue = "lz4") String compressionType;
    @ConfigProperty(name = "kafka.linger.ms", defaultValue = "5") int lingerMs;
    @ConfigProperty(name = "kafka.batch.size", defaultValue = "16384") int batchSize;
    @ConfigProperty(name = "kafka.max.block.ms", defaultValue = "60000") int maxBlockMs;

    private volatile KafkaProducer<String, String> producer;

    @PostConstruct void init() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", acks); config.put("enable.idempotence", String.valueOf(enableIdempotence));
        config.put("retries", String.valueOf(kafkaRetries)); config.put("retry.backoff.ms", String.valueOf(retryBackoffMs));
        config.put("max.in.flight.requests.per.connection", String.valueOf(maxInFlightRequests));
        config.put("delivery.timeout.ms", String.valueOf(deliveryTimeoutMs)); config.put("request.timeout.ms", String.valueOf(requestTimeoutMs));
        config.put("compression.type", compressionType); config.put("linger.ms", String.valueOf(lingerMs));
        config.put("batch.size", String.valueOf(batchSize)); config.put("max.block.ms", String.valueOf(maxBlockMs));
        producer = KafkaProducer.create(vertx, config);
    }

    @PreDestroy void destroy() {
        KafkaProducer<String, String> c = producer; producer = null;
        if (c != null) c.close().onSuccess(v -> log.info("Kafka producer closed.")).onFailure(e -> log.warn("Error closing: {}", e.getMessage()));
    }

    public Uni<Void> sendExistingEvent(String topic, String key, JsonObject eventPayload) {
        if (eventPayload == null) return Uni.createFrom().failure(new IllegalArgumentException("payload null"));
        KafkaProducer<String, String> current = producer;
        if (current == null) return Uni.createFrom().failure(new IllegalStateException("producer not init"));
        KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, eventPayload.encode());
        return Uni.createFrom().emitter(emitter -> current.send(record)
                .onSuccess(m -> { if (tracingMetrics != null) tracingMetrics.recordKafkaPublish("success"); emitter.complete(null); })
                .onFailure(e -> { if (tracingMetrics != null) tracingMetrics.recordKafkaPublish("failure"); emitter.fail(e); }));
    }
}
