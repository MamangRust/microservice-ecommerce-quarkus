package com.sanedge.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class ReactiveCacheTest {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private ReactiveCache cache;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger loads = new AtomicInteger();

    @BeforeEach
    void setUp() {
        cache = new ReactiveCache(redisService, objectMapper, tracingMetrics);
        loads.set(0);
    }

    private Uni<List<String>> loader() {
        return Uni.createFrom().item(() -> {
            loads.incrementAndGet();
            return List.of("db-value");
        });
    }

    @Test
    void getOrCache_hit_returnsCachedAndSkipsLoader() {
        when(redisService.getReactive("k:v")).thenReturn(Uni.createFrom().item("[\"cached\"]"));

        List<String> result = cache.getOrCache("k:v", 300, STRING_LIST, this::loader).await().indefinitely();

        assertThat(result).containsExactly("cached");
        assertThat(loads.get()).isZero();
        verify(tracingMetrics).recordCache("hit");
        verify(redisService, never()).setWithExpirationReactive(anyString(), anyString(), anyLong());
    }

    @Test
    void getOrCache_miss_loadsAndCaches() {
        when(redisService.getReactive("k:v")).thenReturn(Uni.createFrom().nullItem());
        when(redisService.setWithExpirationReactive(eq("k:v"), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        List<String> result = cache.getOrCache("k:v", 300, STRING_LIST, this::loader).await().indefinitely();

        assertThat(result).containsExactly("db-value");
        assertThat(loads.get()).isEqualTo(1);
        verify(tracingMetrics).recordCache("miss");
        verify(redisService).setWithExpirationReactive(eq("k:v"), anyString(), eq(300L));
    }

    @Test
    void getOrCache_redisFailure_fallsBackToLoader() {
        when(redisService.getReactive("k:v"))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("redis down")));

        List<String> result = cache.getOrCache("k:v", 300, STRING_LIST, this::loader).await().indefinitely();

        assertThat(result).containsExactly("db-value");
        assertThat(loads.get()).isEqualTo(1);
        verify(tracingMetrics).recordCache("fallback");
    }

    @Test
    void getOrCache_corruptJson_treatedAsMiss() {
        when(redisService.getReactive("k:v")).thenReturn(Uni.createFrom().item("not-json{{"));
        when(redisService.setWithExpirationReactive(eq("k:v"), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());

        List<String> result = cache.getOrCache("k:v", 300, STRING_LIST, this::loader).await().indefinitely();

        assertThat(result).containsExactly("db-value");
        assertThat(loads.get()).isEqualTo(1);
        verify(tracingMetrics).recordCache("miss");
    }

    @Test
    void evict_delegatesToRedisDelete() {
        when(redisService.deleteReactive("k:v")).thenReturn(Uni.createFrom().voidItem());

        cache.evict("k:v").await().indefinitely();

        verify(redisService).deleteReactive("k:v");
    }
}
