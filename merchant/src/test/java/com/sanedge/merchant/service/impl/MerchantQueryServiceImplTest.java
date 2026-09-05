package com.sanedge.merchant.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.sanedge.common.enums.Status;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.domain.requests.FindAllMerchants;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.repository.MerchantQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class MerchantQueryServiceImplTest {

    @Mock
    private MerchantQueryRepository merchantQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private MerchantQueryServiceImpl merchantQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        merchantQueryService = new MerchantQueryServiceImpl(
                merchantQueryRepository,
                redisService,
                objectMapper,
                tracingMetrics);
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                        .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
    }

    private Merchant createMockMerchant(Long id, Integer userId, String name, Status status) {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(id);
        merchant.setUserId(userId);
        merchant.setName(name);
        merchant.setDescription("Test Description");
        merchant.setAddress("Test Address");
        merchant.setContactEmail("test@merchant.com");
        merchant.setContactPhone("081234567890");
        merchant.setStatus(status);
        merchant.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        merchant.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return merchant;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize in test helper", e);
        }
    }

    @Test
    void findAll_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllMerchants req = new FindAllMerchants();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "merchants:all:1:10:null";

        Merchant merchant1 = createMockMerchant(1L, 1, "Merchant1", Status.PENDING);
        Merchant merchant2 = createMockMerchant(2L, 2, "Merchant2", Status.FAILED);
        PagedResult<Merchant> pagedResult = new PagedResult<>(List.of(merchant1, merchant2), 2);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(merchantQueryRepository.findMerchants(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<MerchantResponse>> response = merchantQueryService.findAll(req).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchants retrieved successfully");
        assertThat(response.data()).hasSize(2);
        assertThat(response.pagination()).isNotNull();
        assertThat(response.pagination().totalRecords()).isEqualTo(2);

        verify(merchantQueryRepository).findMerchants(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findByActive_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllMerchants req = new FindAllMerchants();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "merchants:active:1:10:null";

        Merchant activeMerchant = createMockMerchant(1L, 1, "ActiveMerchant", Status.SUCCESS);
        PagedResult<Merchant> pagedResult = new PagedResult<>(List.of(activeMerchant), 1);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(merchantQueryRepository.findActiveMerchants(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<MerchantResponseDeleteAt>> response = merchantQueryService.findByActive(req).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Active merchants retrieved successfully");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).getName()).isEqualTo("ActiveMerchant");

        verify(merchantQueryRepository).findActiveMerchants(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findByTrashed_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllMerchants req = new FindAllMerchants();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "merchants:trashed:1:10:null";

        Merchant trashedMerchant = createMockMerchant(2L, 1, "TrashedMerchant", Status.PENDING);
        trashedMerchant.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
        PagedResult<Merchant> pagedResult = new PagedResult<>(List.of(trashedMerchant), 1);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(merchantQueryRepository.findTrashedMerchants(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<MerchantResponseDeleteAt>> response = merchantQueryService.findByTrashed(req).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Trashed merchants retrieved successfully");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).getName()).isEqualTo("TrashedMerchant");

        verify(merchantQueryRepository).findTrashedMerchants(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findById_cacheMiss_fetchesFromDbAndSavesToCache() {
        Merchant merchant = createMockMerchant(1L, 1, "TestMerchant", Status.PENDING);

        when(redisService.getReactive("merchant:id:1")).thenReturn(Uni.createFrom().nullItem());
        when(merchantQueryRepository.findMerchantById(1L)).thenReturn(Uni.createFrom().item(merchant));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<MerchantResponse> response = merchantQueryService.findById(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchant retrieved successfully");
        assertThat(response.data().getName()).isEqualTo("TestMerchant");

        verify(merchantQueryRepository).findMerchantById(1L);
        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findById_notFound_throwsNotFoundException() {
        when(redisService.getReactive("merchant:id:999")).thenReturn(Uni.createFrom().nullItem());
        when(merchantQueryRepository.findMerchantById(999L)).thenReturn(Uni.createFrom().nullItem());

        Uni<ApiResponse<MerchantResponse>> resultUni = merchantQueryService.findById(999L);

        assertThrows(NotFoundException.class, () -> resultUni.await().indefinitely());

        verify(redisService, never()).setReactive(anyString(), anyString());
    }

    @Test
    void findByUserId_success() {
        Long userId = 1L;
        String cacheKey = "merchant:user:1";

        Merchant merchant1 = createMockMerchant(1L, 1, "Merchant1", Status.PENDING);
        Merchant merchant2 = createMockMerchant(2L, 1, "Merchant2", Status.FAILED);
        List<Merchant> merchants = List.of(merchant1, merchant2);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(merchantQueryRepository.findByUserId(1)).thenReturn(Uni.createFrom().item(merchants));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<List<MerchantResponse>> response = merchantQueryService.findByUserId(userId).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchants retrieved successfully");
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).getName()).isEqualTo("Merchant1");

        verify(merchantQueryRepository).findByUserId(1);
        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findByUserId_empty() {
        Long userId = 99L;
        String cacheKey = "merchant:user:99";

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(merchantQueryRepository.findByUserId(99)).thenReturn(Uni.createFrom().item(List.of()));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<List<MerchantResponse>> response = merchantQueryService.findByUserId(userId).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchants retrieved successfully");
        assertThat(response.data()).isEmpty();

        verify(merchantQueryRepository).findByUserId(99);
        verify(redisService).setReactive(anyString(), anyString());
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