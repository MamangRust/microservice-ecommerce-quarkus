package com.sanedge.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.test.KafkaResource;
import com.sanedge.common.test.KafkaTestUtils;
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
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Integration tests for {@link TransactionOutboxPublisher} against a REAL Kafka
 * broker (Testcontainers) and a real PostgreSQL: drain-to-SENT, exactly-once
 * under concurrent polls, lease re-processing after a crash window, no
 * republish of SENT events, and backoff-respecting polls.
 */
@QuarkusTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = PostgreSqlResource.class, restrictToAnnotatedClass = true)
@TestProfile(TransactionOutboxPublisherIT.NoSchedulerProfile.class)
@RunOnVertxContext
class TransactionOutboxPublisherIT {

    public static class NoSchedulerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.scheduler.enabled", "false",
                    "transaction.outbox.lease.seconds", "2");
        }
    }

    static final String TOPIC_PREFIX = "email-service-topic-transaction-create-it-";

    @Inject
    TransactionOutboxPublisher publisher;

    @Inject
    TransactionOutboxRepository repository;

    @InjectMock
    TracingMetrics tracingMetrics;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    private Uni<TransactionOutbox> persistPending(String eventId, String topic) {
        TransactionOutbox event = new TransactionOutbox();
        event.setEventId(eventId);
        event.setTopic(topic);
        event.setEventKey("txn-1");
        event.setPayload(new JsonObject()
                .put("event_id", eventId)
                .put("schema_version", 1)
                .put("event_type", topic)
                .put("email", "txn@example.test")
                .put("subject", "IT subject")
                .put("body", "<p>it body</p>")
                .encode());
        return Panache.withTransaction(() -> TransactionOutbox.persist(event).replaceWith(event));
    }

    private Uni<TransactionOutbox> findByEventId(String eventId) {
        return Panache.withSession(() -> repository.find("eventId", eventId).firstResult());
    }

    private Uni<Long> countRecordsOnTopic(String topic, String eventId) {
        return Uni.createFrom()
                .item(() -> KafkaTestUtils.countMatching(bootstrap, topic,
                        r -> eventId.equals(new JsonObject(r.value()).getString("event_id")),
                        Duration.ofSeconds(10)))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Test
    void publishesPendingEventToKafkaAndMarksSent(UniAsserter asserter) {
        String topic = TOPIC_PREFIX + UUID.randomUUID();
        String eventId = "txn-it-drain-" + UUID.randomUUID();

        asserter.execute(() -> persistPending(eventId, topic));
        asserter.execute(() -> publisher.poll());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getStatus()).isEqualTo("SENT");
            assertThat(saved.getLastError()).isNull();
        });
        asserter.assertThat(
                () -> Uni.createFrom()
                        .item(() -> KafkaTestUtils.awaitRecord(bootstrap, topic,
                                r -> eventId.equals(new JsonObject(r.value()).getString("event_id")),
                                Duration.ofSeconds(20)))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()),
                record -> {
                    assertThat(record).isNotNull();
                    JsonObject payload = new JsonObject(record.value());
                    assertThat(payload.getString("event_type")).isEqualTo(topic);
                    assertThat(payload.getString("email")).isEqualTo("txn@example.test");
                    // Observability: the Kafka publish success metric call-site fired.
                    verify(tracingMetrics, atLeastOnce()).recordKafkaPublish("success");
                });
    }

    @Test
    void concurrentPollsPublishExactlyOnce(UniAsserter asserter) {
        String topic = TOPIC_PREFIX + UUID.randomUUID();
        String eventId = "txn-it-exact-" + UUID.randomUUID();

        asserter.execute(() -> persistPending(eventId, topic));
        asserter.execute(() -> Uni.combine().all().unis(publisher.poll(), publisher.poll()).asTuple());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getStatus()).isEqualTo("SENT");
            assertThat(saved.getAttempts()).isEqualTo(1);
        });
        asserter.assertThat(() -> countRecordsOnTopic(topic, eventId),
                count -> assertThat(count).isEqualTo(1));
    }

    @Test
    void orphanedClaimIsReprocessedAfterLeaseExpiry(UniAsserter asserter) {
        String topic = TOPIC_PREFIX + UUID.randomUUID();
        String eventId = "txn-it-lease-" + UUID.randomUUID();
        TransactionOutbox[] ref = new TransactionOutbox[1];

        asserter.execute(() -> persistPending(eventId, topic).invoke(e -> ref[0] = e));
        asserter.assertThat(() -> Panache.withTransaction(() -> repository.claim(ref[0])),
                claimed -> assertThat(claimed).isNotNull());
        asserter.execute(() -> Panache.withTransaction(() -> repository.update(
                "claimedAt = ?1 WHERE id = ?2",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(5)), ref[0].id)));
        asserter.execute(() -> publisher.poll());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getStatus()).isEqualTo("SENT");
            assertThat(saved.getAttempts()).isEqualTo(2);
        });
        asserter.assertThat(() -> countRecordsOnTopic(topic, eventId),
                count -> assertThat(count).isEqualTo(1));
    }

    @Test
    void sentEventIsNotRepublishedByLaterPolls(UniAsserter asserter) {
        String topic = TOPIC_PREFIX + UUID.randomUUID();
        String eventId = "txn-it-sent-" + UUID.randomUUID();

        asserter.execute(() -> persistPending(eventId, topic));
        asserter.execute(() -> publisher.poll());
        asserter.execute(() -> publisher.poll());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getStatus()).isEqualTo("SENT");
            assertThat(saved.getAttempts()).isEqualTo(1);
        });
        asserter.assertThat(() -> countRecordsOnTopic(topic, eventId),
                count -> assertThat(count).isEqualTo(1));
    }

    @Test
    void nonDueEventWithFutureBackoffIsNotPolled(UniAsserter asserter) {
        String topic = TOPIC_PREFIX + UUID.randomUUID();
        String eventId = "txn-it-backoff-" + UUID.randomUUID();
        TransactionOutbox[] ref = new TransactionOutbox[1];

        asserter.execute(() -> persistPending(eventId, topic).invoke(e -> ref[0] = e));
        asserter.execute(() -> Panache.withTransaction(() -> repository.update(
                "nextAttemptAt = ?1 WHERE id = ?2",
                Timestamp.valueOf(LocalDateTime.now().plusMinutes(30)), ref[0].id)));
        asserter.execute(() -> publisher.poll());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getStatus()).isEqualTo("PENDING");
            assertThat(saved.getAttempts()).isZero();
        });
        asserter.assertThat(() -> countRecordsOnTopic(topic, eventId),
                count -> assertThat(count).isZero());
    }
}
