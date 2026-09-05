package com.sanedge.statswriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.clickhouse.ClickHouseClient;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatsWriterConsumer {
    private static final Logger log = LoggerFactory.getLogger(StatsWriterConsumer.class);

    @Inject Vertx vertx;
    @Inject ClickHouseClient clickhouse;
    @Inject StatsWriterMetrics metrics;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092") String bootstrapServers;
    @ConfigProperty(name = "kafka.consumer.group.id", defaultValue = "ecommerce-stats-writer") String groupId;
    @ConfigProperty(name = "stats-writer.batch.max-size", defaultValue = "1000") int maxBatchSize;

    private KafkaConsumer<String, String> consumer;
    private final ConcurrentLinkedQueue<String> orderBatch = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> orderItemBatch = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> transactionBatch = new ConcurrentLinkedQueue<>();
    private final AtomicLong eventsConsumed = new AtomicLong(0);

    @PostConstruct void init() {
        Map<String, String> config = Map.of(
                "bootstrap.servers", bootstrapServers, "group.id", groupId,
                "auto.offset.reset", "earliest", "enable.auto.commit", "true",
                "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                "value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = KafkaConsumer.create(vertx, config);
        consumer.subscribe(Set.of(
                "stats.ecommerce.transaction.event",
                "stats.ecommerce.order.event",
                "stats.ecommerce.order_item.event"))
                .onSuccess(v -> log.info("Subscribed to stats topics"))
                .onFailure(e -> log.error("Subscribe failed: {}", e.getMessage()));
        // Use handler() not recordHandler() — Vert.x 4.x API
        consumer.handler(record -> {
            try { processEvent(record.value()); }
            catch (Exception e) { log.error("Process failed: {}", e.getMessage()); }
        });
        log.info("StatsWriterConsumer initialized. brokers={} group={}", bootstrapServers, groupId);
    }

    @PreDestroy void destroy() { if (consumer != null) consumer.close(); }

    private void processEvent(String message) {
        JsonObject json = new JsonObject(message);
        JsonObject payload = json.getJsonObject("payload", json);
        String eventType = json.getString("event_type", "");
        String occurredAt = ClickHouseClient.normalizeDateTime(json.getString("occurred_at"));
        String eventId = json.getString("event_id", java.util.UUID.randomUUID().toString());
        eventsConsumed.incrementAndGet();
        metrics.recordEventConsumed();

        if (eventType.contains("order_item")) {
            orderItemBatch.add(String.join("\t", eventId, occurredAt != null ? occurredAt : "",
                    String.valueOf(payload.getLong("order_item_id", 0L)), String.valueOf(payload.getLong("order_id", 0L)),
                    String.valueOf(payload.getLong("merchant_id", 0L)), String.valueOf(payload.getLong("category_id", 0L)),
                    String.valueOf(payload.getLong("product_id", 0L)), String.valueOf(payload.getInteger("quantity", 0)),
                    String.valueOf(payload.getDouble("unit_price", 0.0)), String.valueOf(payload.getDouble("subtotal", 0.0)),
                    String.valueOf(System.currentTimeMillis())));
        } else if (eventType.contains("order")) {
            orderBatch.add(String.join("\t", eventId, occurredAt != null ? occurredAt : "",
                    String.valueOf(payload.getLong("order_id", 0L)), String.valueOf(payload.getLong("merchant_id", 0L)),
                    payload.getString("status", ""), String.valueOf(payload.getDouble("total_amount", 0.0)),
                    String.valueOf(System.currentTimeMillis())));
        } else if (eventType.contains("transaction")) {
            transactionBatch.add(String.join("\t", eventId, occurredAt != null ? occurredAt : "",
                    String.valueOf(payload.getLong("transaction_id", 0L)), String.valueOf(payload.getLong("order_id", 0L)),
                    String.valueOf(payload.getLong("merchant_id", 0L)), payload.getString("payment_method", ""),
                    payload.getString("status", ""), String.valueOf(payload.getDouble("amount", 0.0)),
                    String.valueOf(System.currentTimeMillis())));
        }

        if (orderBatch.size() + orderItemBatch.size() + transactionBatch.size() >= maxBatchSize) {
            flush().subscribe().with(v -> {}, e -> log.error("Auto-flush failed: {}", e.getMessage()));
        }
    }

    public Uni<Void> flush() {
        metrics.setBatchPending(orderBatch.size() + orderItemBatch.size() + transactionBatch.size());
        List<Uni<Void>> flushes = new ArrayList<>();
        if (!orderBatch.isEmpty()) flushes.add(flushTable("order_daily", orderBatch));
        if (!orderItemBatch.isEmpty()) flushes.add(flushTable("order_item_daily", orderItemBatch));
        if (!transactionBatch.isEmpty()) flushes.add(flushTable("transaction_daily", transactionBatch));
        if (flushes.isEmpty()) return Uni.createFrom().voidItem();
        return Uni.combine().all().unis(flushes).discardItems();
    }

    private Uni<Void> flushTable(String table, ConcurrentLinkedQueue<String> batch) {
        List<String> rows = new ArrayList<>();
        String row;
        while ((row = batch.poll()) != null) rows.add(row);
        if (rows.isEmpty()) return Uni.createFrom().voidItem();
        String sql = String.format("INSERT INTO ecommerce_stats.%s FORMAT TabSeparated\n%s", table, String.join("\n", rows));
        metrics.setBatchPending(orderBatch.size() + orderItemBatch.size() + transactionBatch.size());
        return clickhouse.execute(sql)
                .invoke(() -> metrics.recordRowsFlushed(table, rows.size()))
                .onFailure().invoke(err -> {
                    log.error("Flush {} failed: {}", table, err.getMessage());
                    metrics.recordFlushError(table);
                });
    }

    public long getEventsConsumed() { return eventsConsumed.get(); }
}
