package com.sanedge.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sanedge.auth.entity.AuthOutbox;
import com.sanedge.auth.repository.AuthOutboxRepository;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.test.KafkaResource;
import com.sanedge.common.test.KafkaTestUtils;
import com.sanedge.common.test.PostgreSqlResource;

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
 * Integration tests for {@link AuthOutboxPublisher} against a REAL Kafka broker
 * (Testcontainers) and a real PostgreSQL. The scheduler is disabled so each
 * test drives {@code poll()} manually (via {@link UniAsserter} on the Vert.x
 * context) and asserts the full claim → send → mark-SENT lifecycle,
 * exactly-once behavior under concurrent polls, lease re-processing after a
 * crash window, and the outbox observability metrics.
 */
@QuarkusTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = PostgreSqlResource.class, restrictToAnnotatedClass = true)
@TestProfile(AuthOutboxPublisherIT.NoSchedulerProfile.class)
@RunOnVertxContext
class AuthOutboxPublisherIT {

    public static class NoSchedulerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.scheduler.enabled", "false",
                    "auth.outbox.lease.seconds", "2");
        }
    }

    static final String TOPIC_PREFIX = "email-service-topic-auth-register-it-";

    @Inject
    AuthOutboxPublisher publisher;

    @Inject
    AuthOutboxRepository repository;

    @InjectMock
    TracingMetrics tracingMetrics;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    private Uni<AuthOutbox> persistPending(String eventId, String topic) {
        AuthOutbox event = new AuthOutbox();
        event.setEventId(eventId);
        event.setTopic(topic);
        event.setEventKey("user-1");
        event.setPayload(new JsonObject()
                .put("event_id", eventId)
                .put("schema_version", 1)
                .put("event_type", topic)
                .put("email", "it@example.test")
                .put("subject", "IT subject")
                .put("body", "<p>it body</p>")
                .encode());
        return Panache.withTransaction(() -> AuthOutbox.persist(event).replaceWith(event));
    }

    private Uni<AuthOutbox> findByEventId(String eventId) {
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
        String eventId = "auth-it-drain-" + UUID.randomUUID();

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
                    assertThat(payload.getString("email")).isEqualTo("it@example.test");
                    // Observability: the Kafka publish success metric call-site fired.
                    verify(tracingMetrics, atLeastOnce()).recordKafkaPublish("success");
                });
    }

    @Test
    void concurrentPollsPublishExactlyOnce(UniAsserter asserter) {
        String topic = TOPIC_PREFIX + UUID.randomUUID();
        String eventId = "auth-it-exact-" + UUID.randomUUID();

        asserter.execute(() -> persistPending(eventId, topic));
        asserter.execute(() -> Uni.combine().all().unis(publisher.poll(), publisher.poll()).asTuple());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getStatus()).isEqualTo("SENT");
            // Exactly one claim won; the concurrent poller was rejected by the
            // in-flight guard and/or the atomic claim token.
            assertThat(saved.getAttempts()).isEqualTo(1);
        });
        asserter.assertThat(() -> countRecordsOnTopic(topic, eventId),
                count -> assertThat(count).isEqualTo(1));
    }

    @Test
    void orphanedClaimIsReprocessedAfterLeaseExpiry(UniAsserter asserter) {
        String topic = TOPIC_PREFIX + UUID.randomUUID();
        String eventId = "auth-it-lease-" + UUID.randomUUID();
        AuthOutbox[] ref = new AuthOutbox[1];

        asserter.execute(() -> persistPending(eventId, topic).invoke(e -> ref[0] = e));
        // Simulate a crash after claim: the row is claimed but the event was
        // never sent. Age the claim past the (2s) lease so it becomes due.
        asserter.assertThat(() -> repository.claim(ref[0]),
                claimed -> assertThat(claimed).isNotNull());
        asserter.execute(() -> Panache.withTransaction(() -> repository.update(
                "claimedAt = ?1 WHERE id = ?2",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(5)), ref[0].id)));
        asserter.execute(() -> publisher.poll());
        asserter.assertThat(() -> findByEventId(eventId), saved -> {
            assertThat(saved.getStatus()).isEqualTo("SENT");
            // 1 claim (stale, crashed) + 1 reprocessing claim after lease expiry.
            assertThat(saved.getAttempts()).isEqualTo(2);
        });
        asserter.assertThat(() -> countRecordsOnTopic(topic, eventId),
                count -> assertThat(count).isEqualTo(1));
    }

    @Test
    void sentEventIsNotRepublishedByLaterPolls(UniAsserter asserter) {
        String topic = TOPIC_PREFIX + UUID.randomUUID();
        String eventId = "auth-it-sent-" + UUID.randomUUID();

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
        String eventId = "auth-it-backoff-" + UUID.randomUUID();
        AuthOutbox[] ref = new AuthOutbox[1];

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
