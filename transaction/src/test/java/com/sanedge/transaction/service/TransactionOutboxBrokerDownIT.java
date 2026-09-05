package com.sanedge.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.transaction.entity.TransactionOutbox;
import com.sanedge.transaction.repository.TransactionOutboxRepository;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;

/**
 * Transaction outbox resilience against an UNAVAILABLE Kafka broker: no Kafka
 * container is started; {@code kafka.bootstrap.servers} points at a closed port
 * so every send fails with a real connection error. Verifies the event stays
 * PENDING with backoff, is not re-attempted before the backoff, and that the
 * Kafka publish failure metric is recorded.
 */
@QuarkusTest
@QuarkusTestResource(value = PostgreSqlResource.class, restrictToAnnotatedClass = true)
@TestProfile(TransactionOutboxBrokerDownIT.NoBrokerProfile.class)
@RunOnVertxContext
class TransactionOutboxBrokerDownIT {

    public static class NoBrokerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.scheduler.enabled", "false",
                    "kafka.bootstrap.servers", "127.0.0.1:59999",
                    "kafka.retries", "1",
                    "kafka.request.timeout.ms", "1000",
                    "kafka.delivery.timeout.ms", "2000",
                    "kafka.max.block.ms", "1000");
        }
    }

    @Inject
    TransactionOutboxPublisher publisher;

    @Inject
    TransactionOutboxRepository repository;

    @InjectMock
    TracingMetrics tracingMetrics;

    private Uni<TransactionOutbox> persistPending(String eventId) {
        TransactionOutbox event = new TransactionOutbox();
        event.setEventId(eventId);
        event.setTopic("email-service-topic-transaction-create-it-down");
        event.setEventKey("txn-1");
        event.setPayload(new JsonObject()
                .put("event_id", eventId)
                .put("schema_version", 1)
                .put("event_type", "test")
                .put("email", "down@example.test")
                .put("subject", "s")
                .put("body", "b")
                .encode());
        return Panache.withTransaction(() -> TransactionOutbox.persist(event).replaceWith(event));
    }

    private Uni<TransactionOutbox> findByEventId(String eventId) {
        return Panache.withSession(() -> repository.find("eventId", eventId).firstResult());
    }

    @Test
    void eventStaysPendingWithBackoffWhenBrokerUnavailable(UniAsserter asserter) {
        String eventId = "txn-it-down-" + UUID.randomUUID();
        TransactionOutbox[] ref = new TransactionOutbox[1];

        Timestamp before = Timestamp.valueOf(LocalDateTime.now());

        asserter.execute(() -> persistPending(eventId).invoke(e -> ref[0] = e));
        asserter.execute(() -> publisher.poll());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getStatus()).isEqualTo("PENDING");
            assertThat(saved.getAttempts()).isEqualTo(1);
            assertThat(saved.getLastError()).isNotBlank();
            // nextAttemptAt must be scheduled strictly after the poll began.
            assertThat(saved.getNextAttemptAt()).isAfter(before);
        });
        // A re-poll while the event is still within its backoff window must not
        // re-attempt it: pin nextAttemptAt far in the future (as the backoff did)
        // and verify a subsequent poll leaves attempts untouched. Deterministic
        // even when the test JVM is slow (the real 1s backoff could otherwise
        // expire between the two polls).
        asserter.execute(() -> Panache.withTransaction(() -> repository.update(
                "nextAttemptAt = ?1 WHERE id = ?2",
                Timestamp.valueOf(LocalDateTime.now().plusMinutes(30)), ref[0].id)));
        asserter.execute(() -> publisher.poll());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getAttempts()).isEqualTo(1);
            assertThat(saved.getStatus()).isEqualTo("PENDING");
        });
    }

    @Test
    void kafkaPublishFailureMetricRecorded(UniAsserter asserter) {
        String eventId = "txn-it-down-metric-" + UUID.randomUUID();

        asserter.execute(() -> persistPending(eventId));
        asserter.execute(() -> publisher.poll());
        asserter.execute(() -> {
            verify(tracingMetrics, atLeastOnce()).recordKafkaPublish("failure");
            verify(tracingMetrics, never()).recordKafkaPublish("success");
        });
        asserter.assertThat(() -> repository.countPending(),
                pending -> assertThat(pending).isGreaterThanOrEqualTo(1));
    }
}
