package com.sanedge.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.response.OrderMonthlyResponse;
import com.sanedge.order.domain.response.OrderYearlyResponse;
import com.sanedge.order.entity.OrderMonthly;
import com.sanedge.order.entity.OrderYearly;
import com.sanedge.order.repository.stats.OrderSoldOutRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderSoldoutServiceImplTest {

    @Mock
    private OrderSoldOutRepository orderSoldOutRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private OrderSoldoutServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new OrderSoldoutServiceImpl(orderSoldOutRepository, redisService, objectMapper, tracingMetrics);
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
    }

    private OrderMonthly createMonthly() {
        return new OrderMonthly("Jan", 10, 1000L, 50);
    }

    private OrderYearly createYearly() {
        return new OrderYearly("2024", 100, 100000L, 500, 10, 20);
    }

    @Test
    void findMonthlyOrders_nullYear_returnsError() {
        ApiResponse<List<OrderMonthlyResponse>> result = service.findMonthlyOrders(null, 1).await().indefinitely();

        assertThat(result.status()).isEqualTo("error");
    }

    @Test
    void findMonthlyOrders_nullMonth_returnsError() {
        ApiResponse<List<OrderMonthlyResponse>> result = service.findMonthlyOrders(2024, null).await().indefinitely();

        assertThat(result.status()).isEqualTo("error");
    }

    @Test
    void findMonthlyOrders_invalidMonth_returnsError() {
        ApiResponse<List<OrderMonthlyResponse>> result = service.findMonthlyOrders(2024, 13).await().indefinitely();

        assertThat(result.status()).isEqualTo("error");
    }

    @Test
    void findMonthlyOrders_cacheMiss_returnsFromDb() {
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(orderSoldOutRepository.findMonthlyOrders(anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(List.of(createMonthly())));
        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponse<List<OrderMonthlyResponse>> result = service.findMonthlyOrders(2024, 1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void findYearlyOrders_nullYear_returnsError() {
        ApiResponse<List<OrderYearlyResponse>> result = service.findYearlyOrders(null).await().indefinitely();

        assertThat(result.status()).isEqualTo("error");
    }

    @Test
    void findYearlyOrders_cacheMiss_returnsFromDb() {
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(orderSoldOutRepository.findYearlyOrders(anyInt()))
                .thenReturn(Uni.createFrom().item(List.of(createYearly())));
        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponse<List<OrderYearlyResponse>> result = service.findYearlyOrders(2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }
}
