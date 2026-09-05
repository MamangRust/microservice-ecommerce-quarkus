package com.sanedge.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.requests.MonthOrderMerchantRequest;
import com.sanedge.order.domain.requests.YearOrderMerchantRequest;
import com.sanedge.order.domain.response.OrderMonthlyResponse;
import com.sanedge.order.domain.response.OrderYearlyResponse;
import com.sanedge.order.entity.OrderMonthly;
import com.sanedge.order.entity.OrderYearly;
import com.sanedge.order.repository.statsbymerchant.OrderSoldOutByMerchantRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderSoldOutByMerchantServiceImplTest {

        @Mock
        private OrderSoldOutByMerchantRepository orderSoldOutByMerchantRepository;

        @Mock
        private RedisService redisService;

        @Mock
        private TracingMetrics tracingMetrics;

        private OrderSoldOutByMerchantServiceImpl service;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                service = new OrderSoldOutByMerchantServiceImpl(
                                orderSoldOutByMerchantRepository,
                                redisService,
                                objectMapper,
                                tracingMetrics);

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

        private OrderMonthly createMockOrderMonthly() {
                return new OrderMonthly("Jan", 10, 1000L, 50);
        }

        private OrderYearly createMockOrderYearly() {
                return new OrderYearly("2024", 100, 100000L, 500, 10, 20);
        }

        private String toJson(Object obj) {
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize in test helper", e);
                }
        }

        @Test
        void findMonthlyOrdersByMerchant_nullMerchantId_returnsError() {
                MonthOrderMerchantRequest req = new MonthOrderMerchantRequest();
                req.setYear(2024);
                req.setMonth(1);

                ApiResponse<List<OrderMonthlyResponse>> response = service.findMonthlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("error");
                assertThat(response.message()).contains("MerchantId");
        }

        @Test
        void findMonthlyOrdersByMerchant_nullYear_returnsError() {
                MonthOrderMerchantRequest req = new MonthOrderMerchantRequest();
                req.setMerchantId(1);
                req.setMonth(1);

                ApiResponse<List<OrderMonthlyResponse>> response = service.findMonthlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("error");
                assertThat(response.message()).contains("Year");
        }

        @Test
        void findMonthlyOrdersByMerchant_nullMonth_returnsError() {
                MonthOrderMerchantRequest req = new MonthOrderMerchantRequest();
                req.setMerchantId(1);
                req.setYear(2024);

                ApiResponse<List<OrderMonthlyResponse>> response = service.findMonthlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("error");
                assertThat(response.message()).contains("Month");
        }

        @Test
        void findMonthlyOrdersByMerchant_invalidMonth_returnsError() {
                MonthOrderMerchantRequest req = new MonthOrderMerchantRequest();
                req.setMerchantId(1);
                req.setYear(2024);
                req.setMonth(13);

                ApiResponse<List<OrderMonthlyResponse>> response = service.findMonthlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("error");
                assertThat(response.message()).contains("between 1 and 12");
        }

        @Test
        void findMonthlyOrdersByMerchant_cacheHit_returnsCachedWithoutHittingDb() {
                MonthOrderMerchantRequest req = new MonthOrderMerchantRequest();
                req.setMerchantId(1);
                req.setYear(2024);
                req.setMonth(1);

                OrderMonthlyResponse cachedData = OrderMonthlyResponse.from(createMockOrderMonthly());
                ApiResponse<List<OrderMonthlyResponse>> cachedResponse = new ApiResponse<>(
                                "success", "Monthly order data for merchant retrieved successfully",
                                List.of(cachedData));

                when(redisService.getReactive("order:soldout:merchant:monthly:1:2024:1"))
                                .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                ApiResponse<List<OrderMonthlyResponse>> response = service.findMonthlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("success");
                assertThat(response.data()).hasSize(1);
                assertThat(response.data().get(0).getMonth()).isEqualTo("Jan");

                verify(orderSoldOutByMerchantRepository, org.mockito.Mockito.never())
                                .findMonthlyOrdersByMerchant(any());
        }

        @Test
        void findMonthlyOrdersByMerchant_cacheMiss_fetchesFromDbAndCaches() {
                MonthOrderMerchantRequest req = new MonthOrderMerchantRequest();
                req.setMerchantId(1);
                req.setYear(2024);
                req.setMonth(1);

                List<OrderMonthly> rawData = List.of(createMockOrderMonthly());

                when(redisService.getReactive("order:soldout:merchant:monthly:1:2024:1"))
                                .thenReturn(Uni.createFrom().nullItem());
                when(orderSoldOutByMerchantRepository.findMonthlyOrdersByMerchant(req))
                                .thenReturn(Uni.createFrom().item(rawData));
                when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponse<List<OrderMonthlyResponse>> response = service.findMonthlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("success");
                assertThat(response.message()).contains("Monthly order data");
                assertThat(response.data()).hasSize(1);
                assertThat(response.data().get(0).getMonth()).isEqualTo("Jan");
                assertThat(response.data().get(0).getOrderCount()).isEqualTo(10);

                verify(orderSoldOutByMerchantRepository).findMonthlyOrdersByMerchant(req);
                verify(redisService).setWithExpirationReactive(anyString(), anyString(), anyLong());
        }

        @Test
        void findYearlyOrdersByMerchant_nullMerchantId_returnsError() {
                YearOrderMerchantRequest req = new YearOrderMerchantRequest();
                req.setYear(2024);

                ApiResponse<List<OrderYearlyResponse>> response = service.findYearlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("error");
                assertThat(response.message()).contains("MerchantId");
        }

        @Test
        void findYearlyOrdersByMerchant_nullYear_returnsError() {
                YearOrderMerchantRequest req = new YearOrderMerchantRequest();
                req.setMerchantId(1);

                ApiResponse<List<OrderYearlyResponse>> response = service.findYearlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("error");
                assertThat(response.message()).contains("Year");
        }

        @Test
        void findYearlyOrdersByMerchant_cacheHit_returnsCachedWithoutHittingDb() {
                YearOrderMerchantRequest req = new YearOrderMerchantRequest();
                req.setMerchantId(1);
                req.setYear(2024);

                OrderYearlyResponse cachedData = OrderYearlyResponse.from(createMockOrderYearly());
                ApiResponse<List<OrderYearlyResponse>> cachedResponse = new ApiResponse<>(
                                "success", "Yearly order data for merchant retrieved successfully",
                                List.of(cachedData));

                when(redisService.getReactive("order:soldout:merchant:yearly:1:2024"))
                                .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                ApiResponse<List<OrderYearlyResponse>> response = service.findYearlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("success");
                assertThat(response.data()).hasSize(1);
                assertThat(response.data().get(0).getYear()).isEqualTo("2024");

                verify(orderSoldOutByMerchantRepository, org.mockito.Mockito.never())
                                .findYearlyOrdersByMerchant(any(), any());
        }

        @Test
        void findYearlyOrdersByMerchant_cacheMiss_fetchesFromDbAndCaches() {
                YearOrderMerchantRequest req = new YearOrderMerchantRequest();
                req.setMerchantId(1);
                req.setYear(2024);

                List<OrderYearly> rawData = List.of(createMockOrderYearly());

                when(redisService.getReactive("order:soldout:merchant:yearly:1:2024"))
                                .thenReturn(Uni.createFrom().nullItem());
                when(orderSoldOutByMerchantRepository.findYearlyOrdersByMerchant(1, 2024))
                                .thenReturn(Uni.createFrom().item(rawData));
                when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponse<List<OrderYearlyResponse>> response = service.findYearlyOrdersByMerchant(req).await()
                                .indefinitely();

                assertThat(response.status()).isEqualTo("success");
                assertThat(response.message()).contains("Yearly order data");
                assertThat(response.data()).hasSize(1);
                assertThat(response.data().get(0).getYear()).isEqualTo("2024");
                assertThat(response.data().get(0).getActiveCashiers()).isEqualTo(10);
                assertThat(response.data().get(0).getUniqueProductsSold()).isEqualTo(20);

                verify(orderSoldOutByMerchantRepository).findYearlyOrdersByMerchant(1, 2024);
                verify(redisService).setWithExpirationReactive(anyString(), anyString(), anyLong());
        }
}
