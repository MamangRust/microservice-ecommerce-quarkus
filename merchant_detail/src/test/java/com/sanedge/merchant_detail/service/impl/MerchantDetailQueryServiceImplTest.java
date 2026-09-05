package com.sanedge.merchant_detail.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponseDeleteAt;
import com.sanedge.merchant_detail.entity.MerchantDetailsRelation;
import com.sanedge.merchant_detail.repository.MerchantDetailQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantDetailQueryServiceImplTest {

    @Mock
    private MerchantDetailQueryRepository merchantDetailQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private MerchantDetailQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new MerchantDetailQueryServiceImpl(
                merchantDetailQueryRepository,
                redisService,
                objectMapper,
                tracingMetrics);

        lenient().doAnswer(inv -> {
            Supplier<Uni<?>> supplier = inv.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().doAnswer(inv -> {
            Supplier<Uni<?>> supplier = inv.getArgument(2);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Supplier.class));
    }

    private MerchantDetailsRelation createRelation(Integer id, Integer merchantId, String displayName) {
        MerchantDetailsRelation relation = new MerchantDetailsRelation();
        relation.setId(id);        // asumsi setter id menerima Long
        relation.setMerchantId(merchantId);
        relation.setDisplayName(displayName);
        relation.setShortDescription("Desc");
        relation.setWebsiteUrl("http://example.com");
        relation.setCoverImageUrl("http://img.com/c.jpg");
        relation.setLogoUrl("http://img.com/l.jpg");
        relation.setSocialMediaLinks(List.of());
        return relation;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private FindAllMerchantRequest buildRequest(int page, int size, String search) {
        FindAllMerchantRequest req = new FindAllMerchantRequest();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search);
        return req;
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {

        @Test
        @DisplayName("cache miss - fetch from DB and cache result")
        void cacheMiss_fetchesFromDb() {
            FindAllMerchantRequest req = buildRequest(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(merchantDetailQueryRepository.findAllWithSocialLinks(any()))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createRelation(1, 1, "Test")), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<MerchantDetailRelationResponse>> result = service.findAll(req)
                    .await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.message()).isEqualTo("Merchant details retrieved successfully");
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).getDisplayName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("cache hit - return cached response")
        void cacheHit_returnsCached() {
            FindAllMerchantRequest req = buildRequest(1, 10, "");
            MerchantDetailRelationResponse cachedData = MerchantDetailRelationResponse.from(createRelation(1, 1, "Cached"));
            ApiResponsePagination<List<MerchantDetailRelationResponse>> cachedResponse =
                    new ApiResponsePagination<>("success", "Merchant details retrieved successfully",
                            List.of(cachedData), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

            ApiResponsePagination<List<MerchantDetailRelationResponse>> result = service.findAll(req)
                    .await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).getDisplayName()).isEqualTo("Cached");
        }
    }

    @Nested
    @DisplayName("findByActive tests")
    class FindByActiveTests {

        @Test
        @DisplayName("cache miss - fetch from DB and cache result")
        void cacheMiss_fetchesFromDb() {
            FindAllMerchantRequest req = buildRequest(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(merchantDetailQueryRepository.findActiveWithSocialLinks(any()))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createRelation(1, 1, "Active")), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> result = service.findByActive(req)
                    .await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.message()).isEqualTo("Active merchant details retrieved successfully");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache hit - return cached response")
        void cacheHit_returnsCached() {
            FindAllMerchantRequest req = buildRequest(1, 10, "");
            MerchantDetailRelationResponseDeleteAt cachedData =
                    MerchantDetailRelationResponseDeleteAt.from(createRelation(1, 1, "CachedActive"));
            ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> cachedResponse =
                    new ApiResponsePagination<>("success", "Active merchant details retrieved successfully",
                            List.of(cachedData), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

            ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> result = service.findByActive(req)
                    .await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByTrashed tests")
    class FindByTrashedTests {

        @Test
        @DisplayName("cache miss - fetch from DB and cache result")
        void cacheMiss_fetchesFromDb() {
            FindAllMerchantRequest req = buildRequest(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(merchantDetailQueryRepository.findTrashedWithSocialLinks(any()))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createRelation(1, 1, "Trashed")), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> result = service.findByTrashed(req)
                    .await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.message()).isEqualTo("Trashed merchant details retrieved successfully");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache hit - return cached response")
        void cacheHit_returnsCached() {
            FindAllMerchantRequest req = buildRequest(1, 10, "");
            MerchantDetailRelationResponseDeleteAt cachedData =
                    MerchantDetailRelationResponseDeleteAt.from(createRelation(1, 1, "CachedTrashed"));
            ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> cachedResponse =
                    new ApiResponsePagination<>("success", "Trashed merchant details retrieved successfully",
                            List.of(cachedData), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

            ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> result = service.findByTrashed(req)
                    .await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {

        @Test
        @DisplayName("cache miss - fetch from DB and cache result")
        void cacheMiss_fetchesFromDb() {
            Long id = 1L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(merchantDetailQueryRepository.findByIdWithSocialLinks(id))
                    .thenReturn(Uni.createFrom().item(Optional.of(createRelation(id.intValue(), 1, "Single"))));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<MerchantDetailRelationResponse> result = service.findById(id).await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.message()).isEqualTo("Merchant Detail retrieved successfully");
            assertThat(result.data().getDisplayName()).isEqualTo("Single");
        }

        @Test
        @DisplayName("cache hit - return cached response")
        void cacheHit_returnsCached() {
            Long id = 1L;
            MerchantDetailRelationResponse cached = MerchantDetailRelationResponse.from(createRelation(id.intValue(), 1, "CachedSingle"));
            when(redisService.getReactive("merchantdetail:id:" + id)).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponse<MerchantDetailRelationResponse> result = service.findById(id).await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getDisplayName()).isEqualTo("CachedSingle");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void notFound_throwsException() {
            Long id = 999L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(merchantDetailQueryRepository.findByIdWithSocialLinks(id))
                    .thenReturn(Uni.createFrom().item(Optional.empty()));

            assertThatThrownBy(() -> service.findById(id).await().indefinitely())
                    .isInstanceOf(com.sanedge.common.exception.ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant detail not found with ID: 999");
        }
    }
}