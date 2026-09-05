package com.sanedge.common.cache;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Standard cache-aside helper shared by query services.
 *
 * <p>Contract:</p>
 * <ul>
 *   <li>Keys are built with {@link CacheKeys} so naming/TTL stay uniform.</li>
 *   <li>Redis failures never break the caller: the loader (DB) is used as a
 *       fallback and the outcome is recorded via {@code cache_outcome_total}.</li>
 *   <li>Corrupt JSON is treated as a miss and transparently reloaded.</li>
 *   <li>Eviction after mutations goes through {@link #evict(String)}.</li>
 * </ul>
 */
@ApplicationScoped
public class ReactiveCache {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final TracingMetrics tracingMetrics;

    protected ReactiveCache() {
        this.redisService = null;
        this.objectMapper = null;
        this.tracingMetrics = null;
    }

    @Inject
    public ReactiveCache(RedisService redisService, ObjectMapper objectMapper, TracingMetrics tracingMetrics) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.tracingMetrics = tracingMetrics;
    }

    /**
     * Returns the cached value for {@code key} or loads it via {@code loader},
     * caching the result for {@code ttlSeconds}. A Redis outage degrades to a
     * plain DB read instead of failing the request.
     */
    public <T> Uni<T> getOrCache(String key, long ttlSeconds, TypeReference<T> type, Supplier<Uni<T>> loader) {
        if (redisService == null) {
            return loader.get();
        }
        AtomicBoolean redisFailed = new AtomicBoolean();
        return redisService.getReactive(key)
                .onFailure().invoke(error -> redisFailed.set(true))
                .onFailure().recoverWithItem((String) null)
                .chain(cached -> {
                    if (cached != null) {
                        try {
                            T value = objectMapper.readValue(cached, type);
                            record("hit");
                            return Uni.createFrom().item(value);
                        } catch (JsonProcessingException e) {
                            // Corrupt cache entry: reload from the source of truth.
                        }
                    }
                    record(redisFailed.get() ? "fallback" : "miss");
                    return loadAndCache(key, ttlSeconds, type, loader);
                });
    }

    /** Evicts a key after a mutation so the next read reloads fresh data. */
    public Uni<Void> evict(String key) {
        if (redisService == null) {
            return Uni.createFrom().voidItem();
        }
        return redisService.deleteReactive(key);
    }

    private <T> Uni<T> loadAndCache(String key, long ttlSeconds, TypeReference<T> type, Supplier<Uni<T>> loader) {
        return loader.get()
                .chain(value -> redisService.setWithExpirationReactive(key, toJson(value), ttlSeconds)
                        .replaceWith(value)
                        .onFailure().recoverWithItem(value));
    }

    private void record(String outcome) {
        if (tracingMetrics != null) {
            tracingMetrics.recordCache(outcome);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cache value", e);
        }
    }
}
