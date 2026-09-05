package com.sanedge.common.observability;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Test doubles that extend {@link TracingMetrics} use the no-arg constructor
 * (meter == null). The domain metric methods must be no-ops in that state so
 * mocks never NPE when services record metrics.
 */
class TracingMetricsNullGuardTest {

    private static final class NullMeterMetrics extends TracingMetrics {
        NullMeterMetrics() {
            super();
        }
    }

    @Test
    void domainMetricMethods_areNoOps_withNullMeter() {
        NullMeterMetrics metrics = new NullMeterMetrics();

        assertThatCode(() -> {
            metrics.recordKafkaPublish("success");
            metrics.recordKafkaPublish("failure");
            metrics.recordKafkaRetry();
            metrics.recordOutboxPublished("transaction");
            metrics.recordDlqSent();
            metrics.recordEmailProcessingDuration(0.5);
            metrics.recordCache("hit");
            metrics.recordStockCompensation("success", 3);
            metrics.registerOutboxBacklogGauge("transaction", () -> 5L);
            metrics.registerKafkaLagGauge("email-group", () -> Map.of("email-topic-0", 2L));
        }).doesNotThrowAnyException();
    }
}
