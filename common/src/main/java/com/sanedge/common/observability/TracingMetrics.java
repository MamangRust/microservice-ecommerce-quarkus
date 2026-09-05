package com.sanedge.common.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TracingMetrics {
    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter requestCounter;
    private final DoubleHistogram requestDurationHistogram;
    private final LongCounter kafkaPublishCounter;
    private final LongCounter kafkaRetryCounter;
    private final LongCounter outboxPublishedCounter;
    private final LongCounter dlqCounter;
    private final LongCounter cacheOutcomeCounter;
    private final LongCounter stockCompensationCounter;
    private final LongCounter emailSentCounter;
    private final LongCounter emailFailedCounter;
    private final LongCounter emailRetriedCounter;
    private final LongCounter emailDuplicateCounter;
    private final LongCounter emailInvalidCounter;
    private final DoubleHistogram emailProcessingDuration;
    private final TextMapPropagator propagator;

    /** Buckets chosen so p50/p95/p99 are meaningful from the Prometheus histogram. */
    private static final List<Double> DURATION_BUCKETS = List.of(
            0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0);

    private static final AttributeKey<String> METHOD_KEY = AttributeKey.stringKey("method");
    private static final AttributeKey<String> STATUS_KEY = AttributeKey.stringKey("status");
    private static final AttributeKey<String> OUTBOX_KEY = AttributeKey.stringKey("outbox");
    private static final AttributeKey<String> RESULT_KEY = AttributeKey.stringKey("result");

    // No-arg constructor required for CDI proxying
    protected TracingMetrics() {
        this.tracer = null;
        this.meter = null;
        this.requestCounter = null;
        this.requestDurationHistogram = null;
        this.kafkaPublishCounter = null;
        this.kafkaRetryCounter = null;
        this.outboxPublishedCounter = null;
        this.dlqCounter = null;
        this.cacheOutcomeCounter = null;
        this.stockCompensationCounter = null;
        this.emailSentCounter = null;
        this.emailFailedCounter = null;
        this.emailRetriedCounter = null;
        this.emailDuplicateCounter = null;
        this.emailInvalidCounter = null;
        this.emailProcessingDuration = null;
        this.propagator = null;
    }

    @Inject
    public TracingMetrics(OpenTelemetry openTelemetry) {
        String instrumentationName = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.application.name", String.class)
                .orElse("payment-service");

        this.tracer = openTelemetry.getTracer(instrumentationName);
        this.meter = openTelemetry.getMeter(instrumentationName);
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();

        this.requestCounter = meter.counterBuilder("requests_total")
                .setDescription("Total number of requests")
                .build();

        this.requestDurationHistogram = meter.histogramBuilder("request_duration_seconds")
                .setDescription("Request duration in seconds")
                .setUnit("s")
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS)
                .build();

        this.kafkaPublishCounter = meter.counterBuilder("kafka_publish_total")
                .setDescription("Total Kafka messages published by result")
                .build();

        this.kafkaRetryCounter = meter.counterBuilder("kafka_retry_total")
                .setDescription("Total Kafka publish retries")
                .build();

        this.outboxPublishedCounter = meter.counterBuilder("outbox_events_published_total")
                .setDescription("Total outbox events published")
                .build();

        this.dlqCounter = meter.counterBuilder("dlq_events_total")
                .setDescription("Total events routed to the dead letter queue")
                .build();

        this.cacheOutcomeCounter = meter.counterBuilder("cache_outcome_total")
                .setDescription("Cache outcome counts (hit, miss, fallback)")
                .build();

        this.stockCompensationCounter = meter.counterBuilder("stock_compensation_total")
                .setDescription("Total stock compensation events by result and item count")
                .build();

        this.emailSentCounter = meter.counterBuilder("email_sent_total")
                .setDescription("Total emails successfully sent via SMTP")
                .build();

        this.emailFailedCounter = meter.counterBuilder("email_failed_total")
                .setDescription("Total email send attempts that failed")
                .build();

        this.emailRetriedCounter = meter.counterBuilder("email_retried_total")
                .setDescription("Total emails routed to the retry topic")
                .build();

        this.emailDuplicateCounter = meter.counterBuilder("email_duplicate_total")
                .setDescription("Total duplicate email events skipped")
                .build();

        this.emailInvalidCounter = meter.counterBuilder("email_invalid_event_total")
                .setDescription("Total events rejected for an invalid envelope")
                .build();

        // Phase 6: email-specific processing duration (POS/Payment already expose
        // email_processing_duration_seconds — this closes the Ecommerce gap).
        this.emailProcessingDuration = meter.histogramBuilder("email_processing_duration_seconds")
                .setDescription("Email record processing duration in seconds")
                .setUnit("s")
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS)
                .build();
    }

    public Tracer getTracer() {
        return tracer;
    }

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(@Nonnull Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        @Nullable
        public String get(@Nullable Map<String, String> carrier, @Nonnull String key) {
            return carrier != null ? carrier.get(key) : null;
        }
    };

    public void injectContext(@Nonnull Context context, @Nonnull Map<String, String> carrier) {
        propagator.inject(Objects.requireNonNull(context), Objects.requireNonNull(carrier), Map::put);
    }

    @Nonnull
    public Context extractContext(@Nonnull Map<String, String> carrier) {
        return Objects.requireNonNull(propagator.extract(
                Objects.requireNonNull(Context.current()),
                Objects.requireNonNull(carrier),
                Objects.requireNonNull(MAP_GETTER)));
    }

    public TracingContext startSpan(String operationName) {
        return startSpan(operationName, Attributes.empty());
    }

    public TracingContext startSpan(String operationName, Attributes attributes) {
        Instant startTime = Instant.now();

        Span span = tracer.spanBuilder(Objects.requireNonNull(operationName))
                .setSpanKind(SpanKind.INTERNAL)
                .setAllAttributes(Objects.requireNonNull(attributes))
                .startSpan();

        Context context = Context.current().with(span);

        return new TracingContext(context, startTime);
    }

    public <T> Uni<T> traceAndMeasure(String operationName, String method, Supplier<Uni<T>> supplier) {
        return traceAndMeasure(operationName, method, Attributes.empty(), supplier);
    }

    public <T> Uni<T> traceAndMeasure(String operationName, String method, Attributes attributes,
            Supplier<Uni<T>> supplier) {
        TracingContext tracingContext = startSpan(operationName, attributes);

        try (Scope scope = tracingContext.getContext().makeCurrent()) {
            Uni<T> result = supplier.get();

            return result
                    .onItemOrFailure().invoke((res, err) -> {
                        if (err != null) {
                            completeSpanError(tracingContext, method, "Operation failed: " + err.getMessage());
                        } else {
                            completeSpanSuccess(tracingContext, method, "Operation completed successfully");
                        }
                    });
        } catch (Exception e) {
            completeSpanError(tracingContext, method, "Operation failed: " + e.getMessage());
            throw e;
        }
    }

    public <T> T traceAndMeasureSync(String operationName, String method, Supplier<T> supplier) {
        return traceAndMeasureSync(operationName, method, Attributes.empty(), supplier);
    }

    public <T> T traceAndMeasureSync(String operationName, String method, Attributes attributes, Supplier<T> supplier) {
        TracingContext tracingContext = startSpan(operationName, attributes);

        try (Scope scope = tracingContext.getContext().makeCurrent()) {
            T result = supplier.get();
            completeSpanSuccess(tracingContext, method, "Operation completed successfully");
            return result;
        } catch (Exception e) {
            completeSpanError(tracingContext, method, "Operation failed: " + e.getMessage());
            throw e;
        }
    }

    /** Records a Kafka publish result ("success" | "failure"). */
    public void recordKafkaPublish(String status) {
        if (kafkaPublishCounter == null) {
            return;
        }
        kafkaPublishCounter.add(1, Attributes.of(STATUS_KEY, status));
    }

    /** Records a Kafka publish retry. */
    public void recordKafkaRetry() {
        if (kafkaRetryCounter == null) {
            return;
        }
        kafkaRetryCounter.add(1);
    }

    /** Records a successfully published outbox event (per outbox table name). */
    public void recordOutboxPublished(String outbox) {
        if (outboxPublishedCounter == null) {
            return;
        }
        outboxPublishedCounter.add(1, Attributes.of(OUTBOX_KEY, outbox));
    }

    /** Records an event routed to the dead letter queue. */
    public void recordDlqSent() {
        if (dlqCounter == null) {
            return;
        }
        dlqCounter.add(1);
    }

    /** Records a successful SMTP delivery. */
    public void recordEmailSent() {
        if (emailSentCounter == null) {
            return;
        }
        emailSentCounter.add(1);
    }

    /** Records a failed SMTP send attempt. */
    public void recordEmailFailed() {
        if (emailFailedCounter == null) {
            return;
        }
        emailFailedCounter.add(1);
    }

    /** Records an event routed to the retry topic. */
    public void recordEmailRetried() {
        if (emailRetriedCounter == null) {
            return;
        }
        emailRetriedCounter.add(1);
    }

    /** Records a duplicate event skipped by idempotency. */
    public void recordEmailDuplicate() {
        if (emailDuplicateCounter == null) {
            return;
        }
        emailDuplicateCounter.add(1);
    }

    /** Records an event rejected for an invalid envelope. */
    public void recordEmailInvalid() {
        if (emailInvalidCounter == null) {
            return;
        }
        emailInvalidCounter.add(1);
    }

    /** Records email record processing duration in seconds. */
    public void recordEmailProcessingDuration(double seconds) {
        if (emailProcessingDuration == null) {
            return;
        }
        emailProcessingDuration.record(seconds);
    }

    /** Records a cache outcome ("hit" | "miss" | "fallback"). */
    public void recordCache(String result) {
        if (cacheOutcomeCounter == null) {
            return;
        }
        cacheOutcomeCounter.add(1, Attributes.of(RESULT_KEY, result));
    }

    /** Records a stock compensation event ("success" | "failure"). */
    public void recordStockCompensation(String result, int items) {
        if (stockCompensationCounter == null) {
            return;
        }
        stockCompensationCounter.add(1, Attributes.of(
                RESULT_KEY, result,
                AttributeKey.longKey("items"), (long) items));
    }

    /**
     * Registers an observable gauge exposing Kafka consumer lag per partition.
     * The supplier returns a map of "topic-partition" to lag; runs on each
     * scrape and degrades gracefully on errors.
     */
    public void registerKafkaLagGauge(String group, java.util.function.Supplier<java.util.Map<String, Long>> supplier) {
        if (meter == null) {
            return;
        }
        meter.gaugeBuilder("kafka_consumer_lag")
                .setDescription("Kafka consumer lag per partition")
                .buildWithCallback(measurement -> {
                    try {
                        java.util.Map<String, Long> lag = supplier.get();
                        if (lag == null) {
                            return;
                        }
                        lag.forEach((partition, value) -> measurement.record(
                                value == null ? 0.0 : value.doubleValue(),
                                Attributes.of(
                                        AttributeKey.stringKey("group"), group,
                                        AttributeKey.stringKey("partition"), partition)));
                    } catch (RuntimeException e) {
                        // Skip this scrape; metrics collection stays resilient.
                    }
                });
    }

    /**
     * Registers an observable gauge exposing the current pending outbox backlog.
     * The supplier runs on each scrape; failures degrade to 0 to keep metrics
     * collection resilient.
     */
    public void registerOutboxBacklogGauge(String outbox, java.util.function.Supplier<Long> supplier) {
        if (meter == null) {
            return;
        }
        meter.gaugeBuilder("outbox_backlog")
                .setDescription("Current pending outbox events")
                .buildWithCallback(measurement -> {
                    try {
                        Long value = supplier.get();
                        measurement.record(value == null ? 0.0 : value.doubleValue(),
                                Attributes.of(OUTBOX_KEY, outbox));
                    } catch (RuntimeException e) {
                        measurement.record(0.0, Attributes.of(OUTBOX_KEY, outbox));
                    }
                });
    }

    public void completeSpanSuccess(TracingContext tracingContext, String method, String message) {
        completeSpan(tracingContext, method, true, message);
    }

    public void completeSpanError(TracingContext tracingContext, String method, String errorMessage) {
        completeSpan(tracingContext, method, false, errorMessage);
    }

    private void completeSpan(TracingContext tracingContext, String method, boolean isSuccess, String message) {
        String status = isSuccess ? "SUCCESS" : "ERROR";
        double duration = Duration.between(tracingContext.getStartTime(), Instant.now()).toMillis() / 1000.0;

        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        span.addEvent("Operation completed", Attributes.builder()
                .put("status", status)
                .put("duration_secs", duration)
                .put("message", message)
                .build());

        if (isSuccess) {
            span.setStatus(StatusCode.OK);
        } else {
            span.setStatus(StatusCode.ERROR, message);
        }

        Attributes metricAttributes = Attributes.builder()
                .put(METHOD_KEY, method)
                .put(STATUS_KEY, status)
                .build();

        requestCounter.add(1, metricAttributes);
        requestDurationHistogram.record(duration, metricAttributes);

        span.end();
    }

    public static class TracingContext {
        private final Context context;
        private final Instant startTime;

        public TracingContext(Context context, Instant startTime) {
            this.context = context;
            this.startTime = startTime;
        }

        public Context getContext() {
            return context;
        }

        public Instant getStartTime() {
            return startTime;
        }
    }
}