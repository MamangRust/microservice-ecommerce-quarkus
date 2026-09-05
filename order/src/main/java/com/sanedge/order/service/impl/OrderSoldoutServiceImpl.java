package com.sanedge.order.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.response.OrderMonthlyResponse;
import com.sanedge.order.domain.response.OrderYearlyResponse;
import com.sanedge.order.repository.stats.OrderSoldOutRepository;
import com.sanedge.order.service.OrderSoldoutService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderSoldoutServiceImpl implements OrderSoldoutService {
    private static final Logger logger = LoggerFactory.getLogger(OrderSoldoutServiceImpl.class);

    private final OrderSoldOutRepository orderSoldOutRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final TracingMetrics tracingMetrics;

    private static final long STATS_CACHE_TTL_SECONDS = 600;

    @Inject
    public OrderSoldoutServiceImpl(OrderSoldOutRepository orderSoldOutRepository,
            RedisService redisService,
            ObjectMapper objectMapper,
            TracingMetrics tracingMetrics) {
        this.orderSoldOutRepository = orderSoldOutRepository;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.tracingMetrics = tracingMetrics;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing object to JSON", e);
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing JSON to object", e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    @Override
    public Uni<ApiResponse<List<OrderMonthlyResponse>>> findMonthlyOrders(Integer year, Integer month) {
        if (year == null || month == null) {
            return Uni.createFrom().item(ApiResponse.error("Year and Month must not be null"));
        }
        if (month < 1 || month > 12) {
            return Uni.createFrom().item(ApiResponse.error("Month must be between 1 and 12"));
        }

        String cacheKey = String.format("order:soldout:monthly:%d:%d", year, month);

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponse<List<OrderMonthlyResponse>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponse<List<OrderMonthlyResponse>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                    return tracingMetrics.traceAndMeasure("findMonthlyOrders", "find_monthly_orders",
                            Attributes.builder()
                                    .put("year", year.toString())
                                    .put("month", month.toString())
                                    .build(),
                            () -> orderSoldOutRepository.findMonthlyOrders(year, month)
                                    .chain(rawData -> {
                                        List<OrderMonthlyResponse> responseList = rawData.stream()
                                                .map(OrderMonthlyResponse::from)
                                                .collect(Collectors.toList());

                                        ApiResponse<List<OrderMonthlyResponse>> response = ApiResponse.success(
                                                "Monthly order data retrieved successfully", responseList);

                                        return redisService
                                                .setWithExpirationReactive(cacheKey, toJson(response),
                                                        STATS_CACHE_TTL_SECONDS)
                                                .map(v -> {
                                                    logger.info("Cached monthly stats for key: {}", cacheKey);
                                                    return response;
                                                });
                                    })
                                    .onFailure()
                                    .invoke(e -> logger.error("Error fetching monthly orders for year={}, month={}",
                                            year, month, e)));
                });
    }

    @Override
    public Uni<ApiResponse<List<OrderYearlyResponse>>> findYearlyOrders(Integer year) {
        if (year == null) {
            return Uni.createFrom().item(ApiResponse.error("Year must not be null"));
        }

        String cacheKey = "order:soldout:yearly:" + year;

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponse<List<OrderYearlyResponse>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponse<List<OrderYearlyResponse>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                    return tracingMetrics.traceAndMeasure("findYearlyOrders", "find_yearly_orders",
                            Attributes.builder().put("year", year.toString()).build(),
                            () -> orderSoldOutRepository.findYearlyOrders(year)
                                    .chain(rawData -> {
                                        List<OrderYearlyResponse> responseList = rawData.stream()
                                                .map(OrderYearlyResponse::from)
                                                .collect(Collectors.toList());

                                        ApiResponse<List<OrderYearlyResponse>> response = ApiResponse.success(
                                                "Yearly order data retrieved successfully", responseList);

                                        return redisService
                                                .setWithExpirationReactive(cacheKey, toJson(response),
                                                        STATS_CACHE_TTL_SECONDS)
                                                .map(v -> {
                                                    logger.info("Cached yearly stats for key: {}", cacheKey);
                                                    return response;
                                                });
                                    })
                                    .onFailure()
                                    .invoke(e -> logger.error("Error fetching yearly orders for year={}", year, e)));
                });
    }
}