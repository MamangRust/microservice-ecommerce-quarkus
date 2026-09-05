package com.sanedge.email;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    public static final String RETRY_TOPIC = "email-service-topic-email-retry";
    public static final String DEAD_LETTER_TOPIC = "email-service-topic-email-dlq";
    private static final int EVENT_SCHEMA_VERSION = 1;
    private static final List<String> INTERNAL_FIELDS = Arrays.asList(
            "_srcTopic", "_srcPartition", "_srcOffset", "_attempt", "_retryAt", "_reason");
    private static final String STATE_PROCESSING = "PROCESSING";
    private static final String STATE_SENT = "SENT";

    /** Result of an atomic idempotency claim (Phase 3). */
    enum ClaimResult {
        /** This caller owns the lease and must send the email. */
        CLAIMED,
        /** Event already reached a terminal SENT state — skip. */
        DUPLICATE,
        /** Another processor holds an active lease — do not send, retry later. */
        BUSY
    }

    @Inject
    Vertx vertx;

    @Inject
    ReactiveMailer mailer;

    @Inject
    RedisService redisService;

    @Inject
    TracingMetrics tracingMetrics;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    @ConfigProperty(name = "kafka.email.group.id", defaultValue = "email-service-group")
    String consumerGroup;

    @ConfigProperty(name = "kafka.email.retry.max-attempts", defaultValue = "3")
    int maxMailAttempts;

    @ConfigProperty(name = "kafka.email.retry.backoff.ms", defaultValue = "500")
    long retryBackoffMs;

    @ConfigProperty(name = "kafka.email.lag.poll.ms", defaultValue = "15000")
    long lagPollMs;

    @ConfigProperty(name = "kafka.email.idempotency.lease-seconds", defaultValue = "60")
    long leaseSeconds;

    @ConfigProperty(name = "kafka.email.idempotency.ttl-seconds", defaultValue = "86400")
    long idempotencyTtlSeconds;

    private KafkaConsumer<String, JsonObject> consumer;
    KafkaProducer<String, String> producer;
    private final AtomicBoolean processing = new AtomicBoolean();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedDeque<KafkaConsumerRecord<String, JsonObject>> pendingRecords = new ConcurrentLinkedDeque<>();
    private final Map<TopicPartition, Long> latestCommittedPosition = new ConcurrentHashMap<>();
    private final Map<String, Long> partitionLag = new ConcurrentHashMap<>();
    private volatile long lagTimerId = -1;

    void onStart(@Observes StartupEvent ev) {
        log.info("Starting Email Service...");

        Map<String, String> kafkaConfig = KafkaSecurityConfig.consumer(bootstrapServers, consumerGroup);

        consumer = KafkaConsumer.create(vertx, kafkaConfig);
        producer = KafkaProducer.create(vertx, producerConfig());

        List<String> topics = Arrays.asList(
                "email-service-topic-auth-register",
                "email-service-topic-auth-forgot-password",
                "email-service-topic-auth-verify-code-success",
                "email-service-topic-merchant-create",
                "email-service-topic-merchant-update-status",
                "email-service-topic-merchant-document-create",
                "email-service-topic-merchant-document-update-status",
                "email-service-topic-transaction-create");

        consumer.handler(this::handleRecord);
        consumer.subscribe(new HashSet<>(topics))
                .onSuccess(v -> log.info("Email Service subscribed to {} topics with manual commits", topics.size()))
                .onFailure(err -> log.error("Failed to subscribe Email Service", err));

        if (tracingMetrics != null) {
            tracingMetrics.registerKafkaLagGauge(consumerGroup, () -> new HashMap<>(partitionLag));
        }
        lagTimerId = vertx.setPeriodic(Math.max(1000, lagPollMs), ignored -> refreshLag());
    }

    private void refreshLag() {
        if (consumer == null) {
            return;
        }
        consumer.assignment()
                .onSuccess(partitions -> {
                    if (partitions == null || partitions.isEmpty()) {
                        return;
                    }
                    consumer.endOffsets(partitions)
                            .onSuccess(endOffsets -> {
                                Map<String, Long> lag = new HashMap<>();
                                for (TopicPartition partition : partitions) {
                                    Long end = endOffsets.get(partition);
                                    if (end == null) {
                                        continue;
                                    }
                                    Long position = latestCommittedPosition.get(partition);
                                    long committed = position == null ? 0L : position;
                                    lag.put(partition.getTopic() + "-" + partition.getPartition(),
                                            Math.max(0L, end - committed));
                                }
                                partitionLag.clear();
                                partitionLag.putAll(lag);
                            })
                            .onFailure(err -> log.warn("Failed to read Kafka end offsets for lag metric", err));
                })
                .onFailure(err -> log.warn("Failed to read Kafka assignment for lag metric", err));
    }

    private Map<String, String> producerConfig() {
        Map<String, String> config = KafkaSecurityConfig.producer(bootstrapServers, "all", true);
        config.put("retries", "10");
        config.put("retry.backoff.ms", String.valueOf(retryBackoffMs));
        return config;
    }

    private void handleRecord(KafkaConsumerRecord<String, JsonObject> record) {
        pendingRecords.offerLast(record);
        consumer.pause();
        drainNextRecord();
    }

    private void drainNextRecord() {
        if (!processing.compareAndSet(false, true)) {
            return;
        }
        KafkaConsumerRecord<String, JsonObject> record = pendingRecords.pollFirst();
        if (record == null) {
            processing.set(false);
            consumer.resume();
            return;
        }
        processRecord(record.topic(), record.partition(), record.offset(),
                record.key(), record.value(), 0)
                .chain(() -> commitAndResume(record))
                .subscribe().with(
                        ignored -> {
                            processing.set(false);
                            drainNextRecord();
                        },
                        error -> {
                            processing.set(false);
                            // Never commit a later offset after this record failed: Kafka commits are
                            // monotonic per partition and would acknowledge the failed record too.
                            // Keep already-buffered records behind the failed one so they cannot be
                            // skipped while the consumer remains paused.
                            pendingRecords.offerFirst(record);
                            log.error("Email record was not committed; pausing drain for retry topic={} partition={} offset={}",
                                    record.topic(), record.partition(), record.offset(), error);
                            vertx.setTimer(Math.max(250, retryBackoffMs), ignored -> drainNextRecord());
                        });
    }

    /**
     * Process one logical notification. Package-private so {@link RetryProcessor}
     * can re-enter with the original source coordinates and the attempt counter.
     *
     * <p>Records {@code email_processing_duration_seconds} (Phase 6) for every
     * processing attempt — terminal outcome or transient failure — so the
     * metric matches the contract exposed by POS and Payment.
     */
    Uni<Void> processRecord(String srcTopic, int srcPartition, long srcOffset, String key,
            JsonObject payload, int attempt) {
        long startNanos = System.nanoTime();
        return processRecordInternal(srcTopic, srcPartition, srcOffset, key, payload, attempt)
                .onItemOrFailure().invoke((ignored, error) -> {
                    if (tracingMetrics != null) {
                        tracingMetrics.recordEmailProcessingDuration(
                                (System.nanoTime() - startNanos) / 1_000_000_000.0);
                    }
                });
    }

    private Uni<Void> processRecordInternal(String srcTopic, int srcPartition, long srcOffset, String key,
            JsonObject payload, int attempt) {
        if (!hasValidEnvelope(payload)) {
            if (tracingMetrics != null) {
                tracingMetrics.recordEmailInvalid();
            }
            return sendDeadLetter(payload, key, srcTopic, srcPartition, srcOffset, "invalid_event_envelope")
                    .onItem().transformToUni(ignored -> Uni.createFrom().voidItem());
        }

        String eventId = payload.getString("event_id");
        return claimEvent(eventId)
                .chain(result -> {
                    switch (result) {
                        case DUPLICATE:
                            // Phase 3: terminal SENT state — replay of the same
                            // event_id must never send a second email.
                            log.info("Skipping duplicate email event_id={}", eventId);
                            if (tracingMetrics != null) {
                                tracingMetrics.recordEmailDuplicate();
                            }
                            return Uni.createFrom().voidItem();
                        case BUSY:
                            // Another consumer holds the lease — do not commit; the
                            // drain loop retries once the lease expires.
                            return Uni.createFrom().failure(new IllegalStateException(
                                    "Email idempotency claim busy, retrying event_id=" + eventId));
                        default: // CLAIMED
                            return sendEmail(payload)
                                    .onItem().transformToUni(ignored -> markSent(eventId))
                                    .onFailure().recoverWithUni(error ->
                                            releaseEvent(eventId)
                                                    .chain(ignored -> routeToRetryOrDlq(payload, key,
                                                            srcTopic, srcPartition, srcOffset, attempt, error)));
                    }
                });
    }

    private Uni<Void> routeToRetryOrDlq(JsonObject payload, String key, String srcTopic,
            int srcPartition, long srcOffset, int attempt, Throwable error) {
        if (tracingMetrics != null) {
            tracingMetrics.recordEmailFailed();
        }
        int nextAttempt = attempt + 1;
        if (nextAttempt >= maxMailAttempts) {
            return sendDeadLetter(payload, key, srcTopic, srcPartition, srcOffset, "max_retries_exceeded");
        }
        if (tracingMetrics != null) {
            tracingMetrics.recordEmailRetried();
        }

        JsonObject retry = payload.copy()
                .put("_srcTopic", srcTopic)
                .put("_srcPartition", srcPartition)
                .put("_srcOffset", srcOffset)
                .put("_attempt", nextAttempt)
                .put("_retryAt", System.currentTimeMillis() + backoffMs(nextAttempt))
                .put("_reason", error.getMessage());

        log.warn("🔁 Email send failed; scheduled retry {} of {} | event_id={} reason={}",
                nextAttempt, maxMailAttempts, payload.getString("event_id"), error.getMessage());
        return sendToTopic(RETRY_TOPIC, key, retry);
    }

    /**
     * Atomically claims {@code eventId} for processing (Phase 3). The claim is
     * a short lease (PROCESSING state): if the process crashes after claiming
     * but before sending, the key expires and the event is re-claimed instead
     * of being blocked for the full dedup TTL. Fail-open: on Redis outage the
     * in-memory set is used and the email goes out (availability &gt; exactly-once).
     */
    private Uni<ClaimResult> claimEvent(String eventId) {
        String key = idempotencyKey(eventId);
        if (redisService == null) {
            return Uni.createFrom().item(
                    processedEventIds.add(eventId) ? ClaimResult.CLAIMED : ClaimResult.DUPLICATE);
        }
        try {
            return redisService.setIfAbsentWithExpirationReactive(key, STATE_PROCESSING, leaseSeconds)
                    .onItem().transformToUni(claimed -> {
                        if (claimed) {
                            return Uni.createFrom().item(ClaimResult.CLAIMED);
                        }
                        // Key exists: terminal SENT or an active lease held elsewhere.
                        return redisService.getReactive(key)
                                .onItem().transform(state ->
                                        STATE_SENT.equals(state) ? ClaimResult.DUPLICATE : ClaimResult.BUSY);
                    })
                    .onFailure().recoverWithUni(error -> {
                        log.warn("Redis idempotency unavailable; using local dedup fallback", error);
                        return Uni.createFrom().item(
                                processedEventIds.add(eventId) ? ClaimResult.CLAIMED : ClaimResult.DUPLICATE);
                    });
        } catch (RuntimeException error) {
            log.warn("Redis idempotency unavailable; using local dedup fallback", error);
        }
        return Uni.createFrom().item(
                processedEventIds.add(eventId) ? ClaimResult.CLAIMED : ClaimResult.DUPLICATE);
    }

    /** Flips the event to the terminal SENT state (full dedup TTL). */
    private Uni<Void> markSent(String eventId) {
        if (redisService == null) {
            return Uni.createFrom().voidItem();
        }
        try {
            return redisService.setWithExpirationReactive(idempotencyKey(eventId), STATE_SENT, idempotencyTtlSeconds)
                    .onFailure().recoverWithItem((Void) null);
        } catch (RuntimeException error) {
            return Uni.createFrom().voidItem();
        }
    }

    /** Releases the claim so a failed send can be retried. */
    private Uni<Void> releaseEvent(String eventId) {
        processedEventIds.remove(eventId);
        if (redisService == null) {
            return Uni.createFrom().voidItem();
        }
        try {
            return redisService.deleteReactive(idempotencyKey(eventId))
                    .onFailure().recoverWithItem((Void) null);
        } catch (RuntimeException error) {
            return Uni.createFrom().voidItem();
        }
    }

    private String idempotencyKey(String eventId) {
        return "email:idempotency:" + eventId;
    }

    private boolean hasValidEnvelope(JsonObject payload) {
        return payload != null
                && payload.getString("event_id") != null
                && payload.getInteger("schema_version", 0) == EVENT_SCHEMA_VERSION
                && payload.getString("event_type") != null
                && payload.getString("email") != null
                && payload.getString("subject") != null
                && payload.getString("body") != null;
    }

    private Uni<Void> sendEmail(JsonObject payload) {
        if (payload == null
                || payload.getString("email") == null
                || payload.getString("subject") == null
                || payload.getString("body") == null) {
            // Log only envelope presence, never the raw payload (may contain PII).
            log.warn("Received incomplete email payload (event_id={}, has_email={}, has_subject={}, has_body={})",
                    payload == null ? null : payload.getString("event_id"),
                    payload != null && payload.getString("email") != null,
                    payload != null && payload.getString("subject") != null,
                    payload != null && payload.getString("body") != null);
            return Uni.createFrom().voidItem();
        }
        Mail mail = Mail.withHtml(payload.getString("email"), payload.getString("subject"), payload.getString("body"));
        return mailer.send(mail)
                .replaceWithVoid()
                .invoke(() -> {
                    if (tracingMetrics != null) {
                        tracingMetrics.recordEmailSent();
                    }
                    log.info("Email sent event_id={} recipient={}", payload.getString("event_id"),
                            payload.getString("email"));
                });
    }

    private Uni<Void> sendDeadLetter(JsonObject payload, String key, String srcTopic,
            int srcPartition, long srcOffset, String reason) {
        if (producer == null) {
            return Uni.createFrom().failure(new IllegalStateException("Kafka producer not initialized"));
        }
        JsonObject dlq = payload == null ? new JsonObject() : payload.copy();
        for (String field : INTERNAL_FIELDS) {
            dlq.remove(field);
        }
        dlq.put("_reason", reason)
                .put("_srcTopic", srcTopic)
                .put("_srcPartition", srcPartition)
                .put("_srcOffset", srcOffset);
        KafkaProducerRecord<String, String> dlqRecord = KafkaProducerRecord.create(
                DEAD_LETTER_TOPIC, key, dlq.encode());
        return Uni.createFrom().emitter(emitter -> producer.send(dlqRecord)
                .onSuccess(metadata -> {
                    if (tracingMetrics != null) {
                        tracingMetrics.recordDlqSent();
                    }
                    log.warn("Email event routed to DLQ event_id={} offset={} reason={}",
                            payload == null ? null : payload.getString("event_id"), srcOffset, reason);
                    emitter.complete(null);
                })
                .onFailure(emitter::fail));
    }

    private Uni<Void> sendToTopic(String topic, String key, JsonObject payload) {
        return Uni.createFrom().emitter(emitter -> {
            KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, payload.encode());
            producer.send(record)
                    .onSuccess(metadata -> emitter.complete(null))
                    .onFailure(err -> {
                        log.error("Failed to publish to topic={}: {}", topic, err.getMessage());
                        emitter.fail(err);
                    });
        });
    }

    private long backoffMs(int attempt) {
        return Math.min((long) attempt * retryBackoffMs, 300_000L);
    }

    private Uni<Void> commitAndResume(KafkaConsumerRecord<String, JsonObject> record) {
        Map<TopicPartition, OffsetAndMetadata> offsets = Map.of(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1, null));
        return Uni.createFrom().emitter(emitter -> consumer.commit(offsets)
                .onSuccess(ignored -> {
                    latestCommittedPosition.put(
                            new TopicPartition(record.topic(), record.partition()),
                            record.offset() + 1);
                    emitter.complete(null);
                })
                .onFailure(emitter::fail));
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("Stopping Email Service...");
        if (consumer != null) {
            consumer.close()
                    .onSuccess(v -> log.info("Kafka consumer closed successfully"))
                    .onFailure(err -> log.error("Failed to close Kafka consumer", err));
        }
        if (producer != null) {
            producer.close()
                    .onSuccess(v -> log.info("Kafka producer closed successfully"))
                    .onFailure(err -> log.error("Failed to close Kafka producer", err));
        }
        if (lagTimerId >= 0) {
            vertx.cancelTimer(lagTimerId);
        }
        pendingRecords.clear();
    }
}