package com.sanedge.auth.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.KafkaResource;
import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.common.test.RedisResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration test for the dependency-aware readiness endpoint with a REAL
 * Kafka broker: {@code /q/health/ready} must report {@code 200} and include
 * {@code kafka-broker-connectivity} as {@code UP} when the broker configured
 * via {@code kafka.bootstrap.servers} is reachable.
 */
@QuarkusTest
@QuarkusTestResource(value = KafkaResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = PostgreSqlResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = RedisResource.class, restrictToAnnotatedClass = true)
class KafkaHealthCheckIT {

    @TestHTTPResource("/q/health/ready")
    URI readyUri;

    @Test
    void readyReportsUpWhenKafkaReachable() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(readyUri).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"kafka-broker-connectivity\"")
                .contains("\"UP\"");
    }
}
