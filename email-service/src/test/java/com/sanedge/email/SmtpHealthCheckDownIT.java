package com.sanedge.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.KafkaResource;
import com.sanedge.common.test.RedisResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Integration test for the dependency-aware readiness endpoint with an
 * UNAVAILABLE SMTP server: {@code /q/health/ready} must report {@code 503}
 * and mark {@code smtp-connectivity} as {@code DOWN} when
 * {@code quarkus.mailer.port} points at a closed port.
 */
@QuarkusTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = RedisResource.class, restrictToAnnotatedClass = true)
@TestProfile(SmtpHealthCheckDownIT.SmtpDownProfile.class)
class SmtpHealthCheckDownIT {

    public static class SmtpDownProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.mailer.host", "127.0.0.1",
                    "quarkus.mailer.port", "59998",
                    "quarkus.mailer.mock", "false",
                    "smtp.health.enabled", "true",
                    "smtp.health.timeout.ms", "1000");
        }
    }

    @TestHTTPResource("/q/health/ready")
    URI readyUri;

    @Test
    void readyReportsDownWhenSmtpUnreachable() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(readyUri).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body())
                .contains("\"smtp-connectivity\"")
                .contains("\"DOWN\"");
    }
}
