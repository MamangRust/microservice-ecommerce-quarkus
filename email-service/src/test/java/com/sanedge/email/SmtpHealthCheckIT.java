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
 * Integration test for the dependency-aware readiness endpoint with a REAL
 * (fake, protocol-correct) SMTP server: {@code /q/health/ready} must report
 * {@code 200} and mark {@code smtp-connectivity} as {@code UP}.
 */
@QuarkusTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = RedisResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = FakeSmtpResource.class, restrictToAnnotatedClass = true)
@TestProfile(SmtpHealthCheckIT.SmtpUpProfile.class)
class SmtpHealthCheckIT {

    /**
     * {@code smtp.health.enabled} is a build-time property, so it must be
     * declared here (TestProfile config is available at augmentation) rather
     * than returned from the FakeSmtpResource at runtime.
     */
    public static class SmtpUpProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.mailer.host", "127.0.0.1",
                    "quarkus.mailer.port", String.valueOf(FakeSmtpResource.SMTP_PORT),
                    "smtp.health.enabled", "true");
        }
    }

    @TestHTTPResource("/q/health/ready")
    URI readyUri;

    @Test
    void readyReportsUpWhenSmtpReachable() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(readyUri).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"smtp-connectivity\"")
                .contains("\"UP\"");
    }
}
