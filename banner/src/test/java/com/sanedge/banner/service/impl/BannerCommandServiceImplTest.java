package com.sanedge.banner.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.banner.domain.requests.CreateBannerRequest;
import com.sanedge.banner.domain.requests.UpdateBannerRequest;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;
import com.sanedge.banner.entity.Banner;
import com.sanedge.banner.repository.BannerCommandRepository;
import com.sanedge.banner.repository.BannerQueryRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class BannerCommandServiceImplTest {

        @Mock
        private BannerQueryRepository bannerQueryRepository;

        @Mock
        private BannerCommandRepository bannerCommandRepository;

        @Mock
        private RedisService redisService;

        @Mock
        private TracingMetrics tracingMetrics;

        private BannerCommandServiceImpl bannerCommandService;

        @BeforeEach
        void setUp() {
                lenient().doAnswer(invokeSupplier())
                                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
                lenient().doAnswer(invokeSupplier())
                                .when(tracingMetrics)
                                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

                bannerCommandService = new BannerCommandServiceImpl(
                                bannerQueryRepository,
                                bannerCommandRepository,
                                redisService,
                                tracingMetrics);
        }

        @Test
        void createBanner_Success() {
                CreateBannerRequest request = CreateBannerRequest.builder()
                                .name("Summer Sale Banner")
                                .startDate("2026-06-01")
                                .endDate("2026-08-31")
                                .startTime("09:00")
                                .endTime("18:00")
                                .isActive(true)
                                .build();

                Banner savedBanner = createTestBanner(1L, "Summer Sale Banner");
                savedBanner.setStartDate(Date.valueOf(LocalDate.parse("2026-06-01")));
                savedBanner.setEndDate(Date.valueOf(LocalDate.parse("2026-08-31")));
                savedBanner.setStartTime(Time.valueOf(LocalTime.parse("09:00")));
                savedBanner.setEndTime(Time.valueOf(LocalTime.parse("18:00")));
                savedBanner.setIsActive(true);

                lenient().when(bannerQueryRepository.findByName("Summer Sale Banner"))
                                .thenReturn(Uni.createFrom().nullItem());
                lenient().when(bannerCommandRepository.persist(any(Banner.class)))
                                .thenReturn(Uni.createFrom().item(savedBanner));

                ApiResponse<BannerResponse> result = bannerCommandService.createBanner(request)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.message()).isEqualTo("Banner created successfully!");
                assertThat(result.data()).isNotNull();
                assertThat(result.data().getName()).isEqualTo("Summer Sale Banner");
                verify(bannerQueryRepository).findByName("Summer Sale Banner");
                verify(bannerCommandRepository).persist(any(Banner.class));
        }

        @Test
        void createBanner_AlreadyExists_ThrowsException() {
                CreateBannerRequest request = CreateBannerRequest.builder()
                                .name("Existing Banner")
                                .startDate("2026-06-01")
                                .endDate("2026-08-31")
                                .startTime("09:00")
                                .endTime("18:00")
                                .isActive(true)
                                .build();

                Banner existingBanner = createTestBanner(1L, "Existing Banner");
                lenient().when(bannerQueryRepository.findByName("Existing Banner"))
                                .thenReturn(Uni.createFrom().item(existingBanner));

                assertThatThrownBy(() -> bannerCommandService.createBanner(request).await().indefinitely())
                                .isInstanceOf(ResourceAlreadyExistsException.class)
                                .hasMessageContaining("Banner with name 'Existing Banner' already exists");
        }

        @Test
        void updateBanner_Success() {
                Long bannerId = 1L;
                UpdateBannerRequest request = UpdateBannerRequest.builder()
                                .id(bannerId)
                                .name("Updated Banner")
                                .startDate("2026-07-01")
                                .endDate("2026-09-30")
                                .startTime("10:00")
                                .endTime("20:00")
                                .isActive(false)
                                .build();

                Banner existingBanner = createTestBanner(bannerId, "Old Banner");
                lenient().when(bannerCommandRepository.findById(bannerId))
                                .thenReturn(Uni.createFrom().item(existingBanner));
                lenient().when(bannerCommandRepository.persist(any(Banner.class)))
                                .thenReturn(Uni.createFrom().item(existingBanner));
                lenient().when(redisService.deleteReactive(anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponse<BannerResponse> result = bannerCommandService.updateBanner(request)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.message()).isEqualTo("Banner updated successfully!");
                verify(redisService).deleteReactive("banner:" + bannerId);
        }

        @Test
        void updateBanner_NotFound_ThrowsException() {
                Long bannerId = 999L;
                UpdateBannerRequest request = UpdateBannerRequest.builder()
                                .id(bannerId)
                                .name("Updated Banner")
                                .startDate("2026-07-01")
                                .endDate("2026-09-30")
                                .startTime("10:00")
                                .endTime("20:00")
                                .isActive(false)
                                .build();

                lenient().when(bannerCommandRepository.findById(bannerId))
                                .thenReturn(Uni.createFrom().nullItem());

                assertThatThrownBy(() -> bannerCommandService.updateBanner(request).await().indefinitely())
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Banner not found");
        }

        @Test
        void updateBanner_NullId_ThrowsException() {
                UpdateBannerRequest request = UpdateBannerRequest.builder()
                                .id(null)
                                .name("Updated Banner")
                                .startDate("2026-07-01")
                                .endDate("2026-09-30")
                                .startTime("10:00")
                                .endTime("20:00")
                                .isActive(false)
                                .build();

                assertThatThrownBy(() -> bannerCommandService.updateBanner(request).await().indefinitely())
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("banner_id is required");
        }

        @Test
        void trashedBanner_Success() {
                Long bannerId = 1L;
                Banner trashedBanner = createTestBanner(bannerId, "Trashed Banner");
                trashedBanner.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

                lenient().when(bannerCommandRepository.trash(bannerId))
                                .thenReturn(Uni.createFrom().item(trashedBanner));
                lenient().when(redisService.deleteReactive(anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponse<BannerResponseDeleteAt> result = bannerCommandService.trashedBanner(bannerId)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.message()).isEqualTo("Banner trashed successfully!");
                assertThat(result.data()).isNotNull();
                assertThat(result.data().getDeletedAt()).isNotNull();
                verify(redisService).deleteReactive("banner:" + bannerId);
        }

        @Test
        void trashedBanner_NotFound_ThrowsException() {
                Long bannerId = 999L;

                lenient().when(bannerCommandRepository.trash(bannerId))
                                .thenReturn(Uni.createFrom().nullItem());

                assertThatThrownBy(() -> bannerCommandService.trashedBanner(bannerId).await().indefinitely())
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Trashed banner not found with id: " + bannerId);
        }

        @Test
        void restoreBanner_Success() {
                Long bannerId = 1L;
                Banner restoredBanner = createTestBanner(bannerId, "Restored Banner");

                lenient().when(bannerCommandRepository.restore(bannerId))
                                .thenReturn(Uni.createFrom().item(restoredBanner));
                lenient().when(redisService.deleteReactive(anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponse<BannerResponseDeleteAt> result = bannerCommandService.restoreBanner(bannerId)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.message()).isEqualTo("Banner restored successfully!");
                verify(redisService).deleteReactive("banner:" + bannerId);
        }

        @Test
        void restoreBanner_NotFound_ThrowsException() {
                Long bannerId = 999L;

                lenient().when(bannerCommandRepository.restore(bannerId))
                                .thenReturn(Uni.createFrom().nullItem());

                assertThatThrownBy(() -> bannerCommandService.restoreBanner(bannerId).await().indefinitely())
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Restore banner not found with id: " + bannerId);
        }

        @Test
        void deleteBannerPermanent_Success() {
                Long bannerId = 1L;
                Banner banner = createTestBanner(bannerId, "To Delete");

                lenient().when(bannerCommandRepository.deletePermanent(bannerId))
                                .thenReturn(Uni.createFrom().item(banner));
                lenient().when(redisService.deleteReactive(anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                ApiResponse<Void> result = bannerCommandService.deleteBannerPermanent(bannerId)
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.message()).isEqualTo("Banner deleted permanently!");
                verify(redisService).deleteReactive("banner:" + bannerId);
        }

        @Test
        void deleteBannerPermanent_NotFound_ThrowsException() {
                Long bannerId = 999L;

                lenient().when(bannerCommandRepository.deletePermanent(bannerId))
                                .thenReturn(Uni.createFrom().nullItem());

                assertThatThrownBy(() -> bannerCommandService.deleteBannerPermanent(bannerId).await().indefinitely())
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Banner not found or must be trashed before permanent deletion");
        }

        @Test
        void restoreAllBanner_Success() {
                lenient().when(bannerCommandRepository.restoreAllDeleted())
                                .thenReturn(Uni.createFrom().item(true));

                ApiResponse<Void> result = bannerCommandService.restoreAllBanner()
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.message()).isEqualTo("All banners restored successfully!");
        }

        @Test
        void restoreAllBanner_NoTrashedBanners_ThrowsException() {
                lenient().when(bannerCommandRepository.restoreAllDeleted())
                                .thenReturn(Uni.createFrom().item(false));

                assertThatThrownBy(() -> bannerCommandService.restoreAllBanner().await().indefinitely())
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("No trashed banners found");
        }

        @Test
        void deleteAllBannerPermanent_Success() {
                lenient().when(bannerCommandRepository.deleteAllDeleted())
                                .thenReturn(Uni.createFrom().item(true));

                ApiResponse<Void> result = bannerCommandService.deleteAllBannerPermanent()
                                .await().indefinitely();

                assertThat(result.status()).isEqualTo("success");
                assertThat(result.message()).isEqualTo("All banners permanently deleted!");
        }

        @Test
        void deleteAllBannerPermanent_NoTrashedBanners_ThrowsException() {
                lenient().when(bannerCommandRepository.deleteAllDeleted())
                                .thenReturn(Uni.createFrom().item(false));

                assertThatThrownBy(() -> bannerCommandService.deleteAllBannerPermanent().await().indefinitely())
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("No trashed banners found");
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
