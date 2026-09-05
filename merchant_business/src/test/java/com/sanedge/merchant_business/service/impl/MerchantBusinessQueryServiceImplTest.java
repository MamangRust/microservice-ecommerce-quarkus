package com.sanedge.merchant_business.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant_business.domain.requests.FindAllMerchantRequest;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;
import com.sanedge.merchant_business.entity.MerchantBusinessInformation;
import com.sanedge.merchant_business.repository.MerchantBusinessQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class MerchantBusinessQueryServiceImplTest {

    @Mock
    private MerchantBusinessQueryRepository repository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private MerchantBusinessQueryServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new MerchantBusinessQueryServiceImpl(repository, redisService, objectMapper, tracingMetrics);
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                        .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
    }

    private MerchantBusinessInformation createMock(Long id) {
        MerchantBusinessInformation e = new MerchantBusinessInformation();
        e.id = id;
        e.setMerchantId(1);
        e.setBusinessType("Retail");
        e.setTaxId("12.345.678.9-012.345");
        e.setEstablishedYear(2020);
        e.setNumberOfEmployees(50);
        e.setWebsiteUrl("https://example.com");
        e.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        e.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return e;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findAll_cacheHit_returnsCachedList() {
        FindAllMerchantRequest req = new FindAllMerchantRequest();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch("");

        MerchantBusinessResponse r1 = MerchantBusinessResponse.from(createMock(1L));
        ApiResponsePagination<List<MerchantBusinessResponse>> mockResponse = new ApiResponsePagination<>(
                "success", "ok", List.of(r1), null);
        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponsePagination<List<MerchantBusinessResponse>> response = service.findAll(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).hasSize(1);
        verify(repository, never()).findMerchantBusinessInformation(any());
    }

    @Test
    void findAll_cacheMiss_fetchesFromDb() {
        FindAllMerchantRequest req = new FindAllMerchantRequest();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch("");

        PagedResult<MerchantBusinessInformation> paged = new PagedResult<>(List.of(createMock(1L)), 1);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(repository.findMerchantBusinessInformation(any())).thenReturn(Uni.createFrom().item(paged));
        when(redisService.setWithExpirationReactive(anyString(), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<MerchantBusinessResponse>> response = service.findAll(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).hasSize(1);
    }

    @Test
    void findByActive_cacheMiss_fetchesFromDb() {
        FindAllMerchantRequest req = new FindAllMerchantRequest();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch("");

        PagedResult<MerchantBusinessInformation> paged = new PagedResult<>(List.of(createMock(1L)), 1);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(repository.findActiveMerchantBusinessInformation(any())).thenReturn(Uni.createFrom().item(paged));
        when(redisService.setWithExpirationReactive(anyString(), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>> response = service.findByActive(req).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).hasSize(1);
    }

    @Test
    void findByTrashed_cacheMiss_fetchesFromDb() {
        FindAllMerchantRequest req = new FindAllMerchantRequest();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch("");

        PagedResult<MerchantBusinessInformation> paged = new PagedResult<>(List.of(createMock(1L)), 1);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(repository.findTrashedMerchantBusinessInformation(any())).thenReturn(Uni.createFrom().item(paged));
        when(redisService.setWithExpirationReactive(anyString(), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>> response = service.findByTrashed(req).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).hasSize(1);
    }

    @Test
    void findById_cacheMiss_fetchesFromDb() {
        MerchantBusinessInformation business = createMock(1L);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(repository.findMerchantBusinessInformationById(1L)).thenReturn(Uni.createFrom().item(business));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<MerchantBusinessResponse> response = service.findById(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data().getTaxId()).isEqualTo("12.345.678.9-012.345");
    }

    @Test
    void findById_notFound_throwsNotFound() {
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(repository.findMerchantBusinessInformationById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class, () -> service.findById(999L).await().indefinitely());
    }

    /**
     * Finds the Supplier argument in the invocation regardless of whether it was
     * passed positionally in the 3-arg overload (arg index 2) or 4-arg overload
     * (arg index 3), then invokes it and returns the resulting Uni. This lets
     * a single Answer<?> body serve both traceAndMeasure overloads.
     */
    private Answer<Uni<?>> invokeSupplier() {
        return invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? (Uni<?>) supplier.get() : null;
        };
    }
}