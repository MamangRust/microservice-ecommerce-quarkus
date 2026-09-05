package com.sanedge.auth.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.common.test.RedisResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Integration test for the dependency-aware readiness endpoint with an
 * UNAVAILABLE Kafka broker: {@code /q/health/ready} must report {@code 503}
 * and mark {@code kafka-broker-connectivity} as {@code DOWN} when
 * {@code kafka.bootstrap.servers} points at a closed port.
 */
@QuarkusTest
@QuarkusTestResource(value = PostgreSqlResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = RedisResource.class, restrictToAnnotatedClass = true)
@TestProfile(KafkaHealthCheckDownIT.BrokerDownProfile.class)
class KafkaHealthCheckDownIT {

    public static class BrokerDownProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "kafka.bootstrap.servers", "127.0.0.1:59999",
                    "kafka.health.timeout.ms", "1000");
        }
    }

    @TestHTTPResource("/q/health/ready")
    URI readyUri;

    @Test
    void readyReportsDownWhenKafkaUnreachable() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(readyUri).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body())
                .contains("\"kafka-broker-connectivity\"")
                .contains("\"DOWN\"");
    }
}
