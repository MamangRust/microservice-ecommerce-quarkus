package com.sanedge.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.KafkaResource;
import com.sanedge.common.test.KafkaTestUtils;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Consumer idempotency when Redis is UNAVAILABLE: no Redis container is
 * started ({@code quarkus.redis.hosts} points at a closed port), so the
 * {@code SETNX} claim fails and the consumer falls back to the in-memory
 * dedup set. Producing the same {@code event_id} twice must deliver the email
 * exactly once and the offsets must still be committed (no crash loop).
 */
@QuarkusTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
@TestProfile(EmailServiceRedisDownDedupIT.RedisDownProfile.class)
class EmailServiceRedisDownDedupIT {

    public static class RedisDownProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.redis.hosts", "redis://127.0.0.1:59997",
                    "quarkus.mailer.mock", "true");
        }
    }

    static final String SOURCE_TOPIC = "email-service-topic-transaction-create";

    @Inject
    MockMailbox mailbox;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    private String envelope(String eventId) {
        return new JsonObject()
                .put("event_id", eventId)
                .put("schema_version", 1)
                .put("event_type", SOURCE_TOPIC)
                .put("email", "dedup@example.test")
                .put("subject", "Dedup IT")
                .put("body", "<p>body</p>")
                .encode();
    }

    @Test
    void duplicateEventsWithRedisDownAreDedupedLocallyAndCommitted() {
        mailbox.clear();

        String eventId = "dedup-it-" + UUID.randomUUID();
        String payload = envelope(eventId);
        KafkaTestUtils.produce(bootstrap, SOURCE_TOPIC, eventId, payload);
        KafkaTestUtils.produce(bootstrap, SOURCE_TOPIC, eventId, payload);

        // Exactly one delivery despite two identical records (local dedup fallback).
        // The Quarkus mock mailer records deliveries; poll until the first lands,
        // then assert no second copy follows.
        List<Mail> sent = mailbox.getMessagesSentTo("dedup@example.test");
        long deadline = System.currentTimeMillis() + 30_000;
        while (sent.isEmpty() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            sent = mailbox.getMessagesSentTo("dedup@example.test");
        }
        assertThat(sent).hasSize(1);

        // Both records were processed and committed -> no crash loop, lag can be derived.
        long committed = KafkaTestUtils.committedOffset(
                bootstrap, "email-service-group", SOURCE_TOPIC, Duration.ofSeconds(30));
        assertThat(committed).isGreaterThanOrEqualTo(2);
    }
}
