package com.sanedge.statswriter;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Periodic flush scheduler for the stats-writer batch buffer.
 * Flushes every N seconds regardless of batch size.
 */
@ApplicationScoped
public class StatsFlushScheduler {
    private static final Logger log = LoggerFactory.getLogger(StatsFlushScheduler.class);

    @Inject
    StatsWriterConsumer consumer;

    @ConfigProperty(name = "stats-writer.flush.interval-seconds", defaultValue = "5")
    int flushIntervalSeconds;

    @Scheduled(every = "5s", delay = 10, delayUnit = TimeUnit.SECONDS)
    Uni<Void> periodicFlush() {
        return consumer.flush();
    }
}
