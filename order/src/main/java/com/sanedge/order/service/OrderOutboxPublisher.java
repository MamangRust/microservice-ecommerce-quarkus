package com.sanedge.order.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.entity.OrderOutbox;
import com.sanedge.order.repository.OrderOutboxRepository;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OrderOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderOutboxPublisher.class);

    @Inject OrderOutboxRepository repository;
    @Inject KafkaService kafkaService;
    @Inject TracingMetrics tracingMetrics;

    private final AtomicBoolean polling = new AtomicBoolean();
    private final AtomicLong backlog = new AtomicLong(0);

    @Scheduled(every = "1s", delay = 5, delayUnit = TimeUnit.SECONDS)
    Uni<Void> poll() {
        if (!polling.compareAndSet(false, true)) return Uni.createFrom().voidItem();
        return publishDue()
                .onItem().transformToUni(v -> repository.countPending())
                .invoke(count -> { backlog.set(count); polling.set(false); })
                .replaceWithVoid()
                .onFailure().invoke(err -> { log.error("Order outbox poll failed", err); polling.set(false); });
    }

    private Uni<Void> publishDue() {
        return repository.findDue(50).chain(this::publishSequentially);
    }

    private Uni<Void> publishSequentially(List<OrderOutbox> events) {
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (OrderOutbox event : events) {
            chain = chain.chain(() -> repository.claim(event)
                    .chain(claimed -> {
                        if (claimed == null) return Uni.createFrom().voidItem();
                        return kafkaService.sendExistingEvent(claimed.getTopic(), claimed.getEventKey(),
                                new JsonObject(claimed.getPayload()))
                                .chain(() -> repository.markSent(claimed))
                                .invoke(() -> tracingMetrics.recordOutboxPublished("order"))
                                .onFailure().recoverWithUni(error -> repository.markRetry(claimed, error,
                                        Math.min(300, 1L << Math.min(8, Math.max(0, claimed.getAttempts() - 1)))));
                    }));
        }
        return chain;
    }
}
