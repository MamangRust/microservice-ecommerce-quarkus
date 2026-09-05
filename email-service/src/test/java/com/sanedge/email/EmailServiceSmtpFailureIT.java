package com.sanedge.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.test.KafkaResource;
import com.sanedge.common.test.KafkaTestUtils;
import com.sanedge.common.test.RedisResource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * End-to-end SMTP failure resilience with a REAL Kafka broker: the mailer
 * points at a closed SMTP port, so {@code mailer.send} fails for every record.
 * Asserts the consumer retries (bounded) and then routes the event to the
 * dead-letter topic with a reason, and that the {@code dlq_events_total}
 * metric call-site fired.
 */
@QuarkusTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = RedisResource.class, restrictToAnnotatedClass = true)
@TestProfile(EmailServiceSmtpFailureIT.SmtpDownProfile.class)
class EmailServiceSmtpFailureIT {

    public static class SmtpDownProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.mailer.mock", "false",
                    "quarkus.mailer.host", "127.0.0.1",
                    "quarkus.mailer.port", "59998",
                    "kafka.email.retry.max-attempts", "2",
                    "kafka.email.retry.backoff.ms", "100");
        }
    }

    static final String SOURCE_TOPIC = "email-service-topic-auth-register";
    static final String DEAD_LETTER_TOPIC = "email-service-topic-email-dlq";

    @InjectMock
    TracingMetrics tracingMetrics;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    private String envelope(String eventId) {
        return new JsonObject()
                .put("event_id", eventId)
                .put("schema_version", 1)
                .put("event_type", SOURCE_TOPIC)
                .put("email", "smtp-fail@example.test")
                .put("subject", "SMTP failure IT")
                .put("body", "<p>body</p>")
                .encode();
    }

    @Test
    void smtpFailureRetriesThenRoutesEventToDeadLetter() {
        String eventId = "smtp-it-" + UUID.randomUUID();

        KafkaTestUtils.produce(bootstrap, SOURCE_TOPIC, eventId, envelope(eventId));

        ConsumerRecord<String, String> dlq = KafkaTestUtils.awaitRecord(bootstrap, DEAD_LETTER_TOPIC,
                r -> eventId.equals(new JsonObject(r.value()).getString("event_id")),
                Duration.ofSeconds(30));

        assertThat(dlq).isNotNull();
        JsonObject payload = new JsonObject(dlq.value());
        assertThat(payload.getString("_reason")).isEqualTo("max_retries_exceeded");
        assertThat(payload.getString("_srcTopic")).isEqualTo(SOURCE_TOPIC);
        assertThat(payload.getLong("_srcOffset", -1L)).isGreaterThanOrEqualTo(0L);
        // Observability: DLQ metric call-site fired.
        verify(tracingMetrics, atLeastOnce()).recordDlqSent();
        // The consumer must not crash-loop on SMTP failure: committed offset advanced.
        long committed = KafkaTestUtils.committedOffset(
                bootstrap, "email-service-group", SOURCE_TOPIC, Duration.ofSeconds(30));
        assertThat(committed).isGreaterThanOrEqualTo(1);
    }

    @Test
    void invalidEnvelopeIsRoutedToDeadLetterWithoutEmailAttempt() {
        String eventId = "smtp-it-invalid-" + UUID.randomUUID();
        // Missing schema_version / subject / body -> invalid envelope.
        String invalid = new JsonObject()
                .put("event_id", eventId)
                .put("email", "invalid@example.test")
                .encode();

        KafkaTestUtils.produce(bootstrap, SOURCE_TOPIC, eventId, invalid);

        ConsumerRecord<String, String> dlq = KafkaTestUtils.awaitRecord(bootstrap, DEAD_LETTER_TOPIC,
                r -> eventId.equals(new JsonObject(r.value()).getString("event_id")),
                Duration.ofSeconds(30));

        assertThat(dlq).isNotNull();
        assertThat(new JsonObject(dlq.value()).getString("_reason"))
                .isEqualTo("invalid_event_envelope");
        verify(tracingMetrics, atLeastOnce()).recordDlqSent();
    }
}
