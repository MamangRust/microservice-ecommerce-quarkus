package com.sanedge.statswriter;

import java.util.concurrent.atomic.AtomicLong;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OTel Meter metrics for the stats-writer pipeline.
 *
 * <p>Counters:
 * <ul>
 *   <li>{@code stats_writer_events_consumed_total} — events received from Kafka</li>
 *   <li>{@code stats_writer_flush_errors_total} — failed ClickHouse flushes</li>
 * </ul>
 *
 * <p>Gauges (observable, reported per scrape):
 * <ul>
 *   <li>{@code stats_writer_kafka_lag} — estimated Kafka consumer lag</li>
 *   <li>{@code stats_writer_batch_pending} — rows buffered before flush</li>
 * </ul>
 */
@ApplicationScoped
public class StatsWriterMetrics {

    private final LongCounter eventsConsumed;
    private final LongCounter flushErrors;
    private final LongCounter rowsFlushed;
    private final AtomicLong kafkaLag = new AtomicLong();
    private final AtomicLong batchPending = new AtomicLong();

    private static final AttributeKey<String> TABLE_KEY = AttributeKey.stringKey("table");
    private static final AttributeKey<String> STATUS_KEY = AttributeKey.stringKey("status");

    @Inject
    public StatsWriterMetrics(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter("stats-writer");

        this.eventsConsumed = meter.counterBuilder("stats_writer_events_consumed_total")
                .setDescription("Total events received from Kafka")
                .build();

        this.flushErrors = meter.counterBuilder("stats_writer_flush_errors_total")
                .setDescription("Total failed ClickHouse flush attempts")
                .build();

        this.rowsFlushed = meter.counterBuilder("stats_writer_rows_flushed_total")
                .setDescription("Total rows successfully flushed to ClickHouse")
                .build();

        // Observable gauges — reported on every Prometheus scrape
        meter.gaugeBuilder("stats_writer_kafka_lag")
                .setDescription("Estimated Kafka consumer lag (messages)")
                .ofLongs()
                .buildWithCallback(obs -> obs.record(kafkaLag.get()));

        meter.gaugeBuilder("stats_writer_batch_pending")
                .setDescription("Rows currently buffered awaiting flush")
                .ofLongs()
                .buildWithCallback(obs -> obs.record(batchPending.get()));
    }

    /** Increment the events-consumed counter by 1. */
    public void recordEventConsumed() {
        eventsConsumed.add(1);
    }

    /** Record a failed flush attempt for the given table. */
    public void recordFlushError(String table) {
        flushErrors.add(1, Attributes.of(TABLE_KEY, table));
    }

    /** Record a successful flush of N rows to the given table. */
    public void recordRowsFlushed(String table, long count) {
        rowsFlushed.add(count, Attributes.of(TABLE_KEY, table));
    }

    /** Update the Kafka lag gauge (called from async lag probe). */
    public void setKafkaLag(long lag) {
        kafkaLag.set(lag);
    }

    /** Update the batch pending gauge. */
    public void setBatchPending(long pending) {
        batchPending.set(pending);
    }

    /** Get current batch pending count (for health checks). */
    public long getBatchPending() {
        return batchPending.get();
    }

    /** Get current Kafka lag (for health checks). */
    public long getKafkaLag() {
        return kafkaLag.get();
    }
}
