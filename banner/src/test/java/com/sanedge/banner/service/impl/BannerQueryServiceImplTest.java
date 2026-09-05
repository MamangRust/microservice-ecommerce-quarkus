package com.sanedge.banner.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.banner.domain.requests.FindAllBannerRequest;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;
import com.sanedge.banner.entity.Banner;
import com.sanedge.banner.repository.BannerQueryRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class BannerQueryServiceImplTest {

        @Mock
        private BannerQueryRepository bannerQueryRepository;

        @Mock
        private RedisService redisService;

        @Mock
        private TracingMetrics tracingMetrics;

        private BannerQueryServiceImpl bannerQueryService;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(
                                                anyString(),
                                                anyString(),
                                                any(Attributes.class),
                                                any());

                bannerQueryService = new BannerQueryServiceImpl(
                                bannerQueryRepository,
                                redisService,
                                objectMapper,
                                tracingMetrics);
        }

        @Test
        void findAllPaginated_CacheHit() {
                FindAllBannerRequest request = FindAllBannerRequest.builder()
                                .page(1)
                                .pageSize(10)
                                .search("")
                                .build();

                String cachedJson = "{\"status\":\"success\",\"message\":\"Banners retrieved successfully\",\"data\":[],\"pagination\":{\"currentPage\":1,\"pageSize\":10,\"totalPages\":0,\"totalRecords\":0}}";

                lenient().when(redisService.getReactive(anyString()))
                                .thenReturn(Uni.createFrom().item(cachedJson));

                ApiResponsePagination<List<BannerResponse>> result = bannerQueryService.findAllPaginated(request)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                verify(redisService).getReactive(anyString());
        }

        @Test
        void findAllPaginated_CacheMiss_FetchesFromDb() {
                FindAllBannerRequest request = FindAllBannerRequest.builder()
                                .page(1)
                                .pageSize(10)
                                .search("")
                                .build();

                Banner banner = createTestBanner(1L, "Test Banner");
                PagedResult<Banner> pagedResult = new PagedResult<>(List.of(banner), 1);

                lenient().when(redisService.getReactive(anyString()))
                                .thenReturn(Uni.createFrom().nullItem());
                lenient().when(bannerQueryRepository.findBanners(request))
                                .thenReturn(Uni.createFrom().item(pagedResult));
                lenient().when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponsePagination<List<BannerResponse>> result = bannerQueryService.findAllPaginated(request)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.data()).hasSize(1);
                assertThat(result.data().get(0).getName()).isEqualTo("Test Banner");
                verify(bannerQueryRepository).findBanners(request);
                verify(redisService).setWithExpirationReactive(anyString(), anyString(), anyLong());
        }

        @Test
        void findById_CacheHit() throws Exception {
                Long bannerId = 1L;
                String cacheKey = "banner:" + bannerId;

                Banner banner = createTestBanner(bannerId, "Cached Banner");
                BannerResponse bannerResponse = BannerResponse.from(banner);

                lenient().when(redisService.getReactive(cacheKey))
                                .thenReturn(Uni.createFrom().item(objectMapper.writeValueAsString(bannerResponse)));

                ApiResponse<BannerResponse> result = bannerQueryService.findById(bannerId)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.data().getName()).isEqualTo("Cached Banner");
                verify(redisService).getReactive(cacheKey);
        }

        @Test
        void findById_CacheMiss_FetchesFromDb() {
                Long bannerId = 1L;
                String cacheKey = "banner:" + bannerId;

                Banner banner = createTestBanner(bannerId, "DB Banner");

                lenient().when(redisService.getReactive(cacheKey))
                                .thenReturn(Uni.createFrom().nullItem());
                lenient().when(bannerQueryRepository.findById(bannerId))
                                .thenReturn(Uni.createFrom().item(banner));
                lenient().when(redisService.setReactive(anyString(), anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponse<BannerResponse> result = bannerQueryService.findById(bannerId)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.data().getName()).isEqualTo("DB Banner");
                verify(bannerQueryRepository).findById(bannerId);
                verify(redisService).setReactive(anyString(), anyString());
        }

        @Test
        void findById_NotFound_ThrowsException() {
                Long bannerId = 999L;
                String cacheKey = "banner:" + bannerId;

                lenient().when(redisService.getReactive(cacheKey))
                                .thenReturn(Uni.createFrom().nullItem());
                lenient().when(bannerQueryRepository.findById(bannerId))
                                .thenReturn(Uni.createFrom().nullItem());

                assertThatThrownBy(() -> bannerQueryService.findById(bannerId).await().indefinitely())
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Banner not found with id: " + bannerId);
        }

        @Test
        void findActivePaginated_CacheMiss_FetchesFromDb() {
                FindAllBannerRequest request = FindAllBannerRequest.builder()
                                .page(1)
                                .pageSize(10)
                                .search("")
                                .build();

                Banner banner = createTestBanner(1L, "Active Banner");
                banner.setIsActive(true);
                PagedResult<Banner> pagedResult = new PagedResult<>(List.of(banner), 1);

                lenient().when(redisService.getReactive(anyString()))
                                .thenReturn(Uni.createFrom().nullItem());
                lenient().when(bannerQueryRepository.findActiveBanners(request))
                                .thenReturn(Uni.createFrom().item(pagedResult));
                lenient().when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponsePagination<List<BannerResponseDeleteAt>> result = bannerQueryService
                                .findActivePaginated(request)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.data()).hasSize(1);
                assertThat(result.data().get(0).getName()).isEqualTo("Active Banner");
                verify(bannerQueryRepository).findActiveBanners(request);
        }

        @Test
        void findTrashedPaginated_CacheMiss_FetchesFromDb() {
                FindAllBannerRequest request = FindAllBannerRequest.builder()
                                .page(1)
                                .pageSize(10)
                                .search("")
                                .build();

                Banner banner = createTestBanner(1L, "Trashed Banner");
                banner.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
                PagedResult<Banner> pagedResult = new PagedResult<>(List.of(banner), 1);

                lenient().when(redisService.getReactive(anyString()))
                                .thenReturn(Uni.createFrom().nullItem());
                lenient().when(bannerQueryRepository.findTrashedBanners(request))
                                .thenReturn(Uni.createFrom().item(pagedResult));
                lenient().when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponsePagination<List<BannerResponseDeleteAt>> result = bannerQueryService
                                .findTrashedPaginated(request)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.data()).hasSize(1);
                assertThat(result.data().get(0).getName()).isEqualTo("Trashed Banner");
                verify(bannerQueryRepository).findTrashedBanners(request);
        }

        private Banner createTestBanner(Long id, String name) {
                Banner banner = new Banner();
                try {
                        Field idField = banner.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(banner, id);
                } catch (Exception e) {
                        throw new RuntimeException("Failed to set banner id", e);
                }
                banner.setName(name);
                banner.setStartDate(Date.valueOf(LocalDate.now()));
                banner.setEndDate(Date.valueOf(LocalDate.now().plusMonths(1)));
                banner.setStartTime(Time.valueOf(LocalTime.of(9, 0)));
                banner.setEndTime(Time.valueOf(LocalTime.of(18, 0)));
                banner.setIsActive(true);
                banner.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                banner.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return banner;
        }
}
