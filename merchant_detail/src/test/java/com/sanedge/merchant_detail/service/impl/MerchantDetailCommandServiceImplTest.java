package com.sanedge.merchant_detail.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant_detail.domain.response.MerchantDetailResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailResponseDeleteAt;
import com.sanedge.merchant_detail.entity.MerchantDetail;
import com.sanedge.merchant_detail.repository.MerchantDetailCommandRepository;
import com.sanedge.merchant_detail.repository.MerchantDetailQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.merchant.MerchantCommon;
import pb.merchant.MerchantQueryService;
import pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest;
import pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest;

@ExtendWith(MockitoExtension.class)
class MerchantDetailCommandServiceImplTest {

    @Mock
    private MerchantDetailQueryRepository merchantDetailQueryRepository;

    @Mock
    private MerchantDetailCommandRepository merchantDetailCommandRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    @Mock
    private MerchantQueryService merchantQueryService;

    private MerchantDetailCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MerchantDetailCommandServiceImpl(
                merchantDetailQueryRepository,
                merchantDetailCommandRepository,
                redisService,
                tracingMetrics,
                merchantQueryService);

        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any(Supplier.class));

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(2);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(anyString(), anyString(), any(Supplier.class));

        lenient().when(merchantDetailCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(merchantDetailCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
    }

    private MerchantDetail createTestMerchantDetail(Long id, Integer merchantId, String displayName) {
        MerchantDetail detail = new MerchantDetail();
        detail.setId(id);
        detail.setMerchantId(merchantId);
        detail.setDisplayName(displayName);
        detail.setShortDescription("short");
        detail.setWebsiteUrl("http://example.com");
        detail.setCoverImageUrl("http://img.com/c.jpg");
        detail.setLogoUrl("http://img.com/l.jpg");
        detail.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        detail.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return detail;
    }

    private CreateMerchantDetailRequest createValidCreateRequest() {
        return CreateMerchantDetailRequest.newBuilder()
                .setMerchantId(1)
                .setDisplayName("Test Detail")
                .setShortDescription("desc")
                .setWebsiteUrl("http://web.com")
                .setCoverImageUrl("http://img.com/cover.jpg")
                .setLogoUrl("http://img.com/logo.jpg")
                .build();
    }

    private UpdateMerchantDetailRequest createValidUpdateRequest() {
        return UpdateMerchantDetailRequest.newBuilder()
                .setMerchantDetailId(1)
                .setDisplayName("Updated Detail")
                .setShortDescription("updated desc")
                .setWebsiteUrl("http://newweb.com")
                .setCoverImageUrl("http://img.com/newcover.jpg")
                .setLogoUrl("http://img.com/newlogo.jpg")
                .build();
    }

    private void mockMerchantExists(Integer merchantId) {
        when(merchantQueryService.findById(
                any(MerchantCommon.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(
                        MerchantCommon.ApiResponseMerchant.newBuilder()
                                .setData(MerchantCommon.MerchantResponse.newBuilder()
                                        .setId(merchantId)
                                        .setUserId(100)
                                        .build())
                                .build()));
    }

    private void mockMerchantNotFound() {
        when(merchantQueryService.findById(
                any(MerchantCommon.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(
                        MerchantCommon.ApiResponseMerchant.newBuilder().build()));
    }

    @Nested
    @DisplayName("create merchant detail tests")
    class CreateTests {

        @Test
        @DisplayName("should successfully create merchant detail")
        void create_Success() {
            CreateMerchantDetailRequest req = createValidCreateRequest();
            mockMerchantExists(req.getMerchantId());

            when(merchantDetailCommandRepository.persist(any(MerchantDetail.class)))
                    .thenAnswer(inv -> {
                        MerchantDetail d = inv.getArgument(0);
                        d.setId(1L);
                        return Uni.createFrom().item(d);
                    });

            ApiResponse<MerchantDetailResponse> response = service.createMerchant(req).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant detail created successfully!");
            assertThat(response.data().getId()).isEqualTo(1L);
            assertThat(response.data().getDisplayName()).isEqualTo("Test Detail");
            verify(redisService).deleteReactive("merchantdetail:id:1");
        }

        @Test
        @DisplayName("should fail when merchant not found")
        void create_MerchantNotFound() {
            CreateMerchantDetailRequest req = createValidCreateRequest();
            mockMerchantNotFound();

            assertThatThrownBy(() -> service.createMerchant(req).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant not found with id " + req.getMerchantId());
        }
    }

    @Nested
    @DisplayName("update merchant detail tests")
    class UpdateTests {

        @Test
        @DisplayName("should successfully update merchant detail")
        void update_Success() {
            UpdateMerchantDetailRequest req = createValidUpdateRequest();
            MerchantDetail existing = createTestMerchantDetail(1L, 1, "Old Name");

            when(merchantDetailQueryRepository.findById(1L))
                    .thenReturn(Uni.createFrom().item(existing));
            when(merchantDetailCommandRepository.persist(any(MerchantDetail.class)))
                    .thenAnswer(inv -> {
                        MerchantDetail d = inv.getArgument(0);
                        return Uni.createFrom().item(d);
                    });

            ApiResponse<MerchantDetailResponse> response = service.updateMerchant(req).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant detail updated successfully!");
            assertThat(response.data().getDisplayName()).isEqualTo("Updated Detail");
            verify(redisService).deleteReactive("merchantdetail:id:1");
        }

        @Test
        @DisplayName("should fail when detail not found")
        void update_NotFound() {
            UpdateMerchantDetailRequest req = createValidUpdateRequest();
            when(merchantDetailQueryRepository.findById(1L)).thenReturn(Uni.createFrom().nullItem());

            assertThatThrownBy(() -> service.updateMerchant(req).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant detail not found with id 1");
        }
    }

    @Nested
    @DisplayName("trash merchant detail tests")
    class TrashTests {

        @Test
        @DisplayName("should successfully trash detail")
        void trash_Success() {
            Long id = 1L;
            MerchantDetail detail = createTestMerchantDetail(id, 1, "To Trash");
            when(merchantDetailCommandRepository.trashed(id)).thenReturn(Uni.createFrom().item(detail));

            ApiResponse<MerchantDetailResponseDeleteAt> response = service.trashedMerchant(id).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant detail trashed successfully!");
            assertThat(response.data().getId()).isEqualTo(id);
            verify(redisService).deleteReactive("merchantdetail:id:1");
        }

        @Test
        @DisplayName("should fail when detail not found")
        void trash_NotFound() {
            when(merchantDetailCommandRepository.trashed(999L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.trashedMerchant(999L).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant detail not found or already trashed");
        }
    }

    @Nested
    @DisplayName("restore merchant detail tests")
    class RestoreTests {

        @Test
        @DisplayName("should successfully restore detail")
        void restore_Success() {
            Long id = 1L;
            MerchantDetail detail = createTestMerchantDetail(id, 1, "Restored");
            when(merchantDetailCommandRepository.restore(id)).thenReturn(Uni.createFrom().item(detail));

            ApiResponse<MerchantDetailResponseDeleteAt> response = service.restoreMerchant(id).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant detail restored successfully!");
            verify(redisService).deleteReactive("merchantdetail:id:1");
        }

        @Test
        @DisplayName("should fail when detail not trashed")
        void restore_NotTrashed() {
            when(merchantDetailCommandRepository.restore(1L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.restoreMerchant(1L).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant detail not found or not trashed");
        }
    }

    @Nested
    @DisplayName("delete permanent detail tests")
    class DeletePermanentTests {

        @Test
        @DisplayName("should permanently delete detail")
        void deletePermanent_Success() {
            Long id = 1L;
            MerchantDetail detail = createTestMerchantDetail(id, 1, "Deleted");
            detail.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(merchantDetailCommandRepository.deletePermanent(id)).thenReturn(Uni.createFrom().item(detail));

            ApiResponse<Void> response = service.deleteMerchantPermanent(id).await().indefinitely();
            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant detail permanently deleted");
            verify(redisService).deleteReactive("merchantdetail:id:1");
        }

        @Test
        @DisplayName("should fail when not found or not trashed")
        void deletePermanent_NotFound() {
            when(merchantDetailCommandRepository.deletePermanent(999L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.deleteMerchantPermanent(999L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Merchant detail not found or must be trashed before permanent deletion");
        }
    }

    @Nested
    @DisplayName("restore all details tests")
    class RestoreAllTests {

        @Test
        @DisplayName("should restore all trashed details")
        void restoreAll_Success() {
            ApiResponse<Void> response = service.restoreAllMerchant().await().indefinitely();
            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("All trashed merchant details restored");
        }

        @Test
        @DisplayName("should fail when no trashed details")
        void restoreAll_NoTrashed() {
            when(merchantDetailCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAllMerchant().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed merchant details found");
        }
    }

    @Nested
    @DisplayName("delete all permanent details tests")
    class DeleteAllTests {

        @Test
        @DisplayName("should delete all trashed details")
        void deleteAll_Success() {
            ApiResponse<Void> response = service.deleteAllMerchantPermanent().await().indefinitely();
            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("All trashed merchant details permanently deleted");
        }

        @Test
        @DisplayName("should fail when no trashed details")
        void deleteAll_NoTrashed() {
            when(merchantDetailCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAllMerchantPermanent().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed merchant details found");
        }
    }
}