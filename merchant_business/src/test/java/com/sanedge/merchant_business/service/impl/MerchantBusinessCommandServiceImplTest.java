package com.sanedge.merchant_business.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

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
import com.sanedge.merchant_business.domain.requests.CreateMerchantBusinessRequest;
import com.sanedge.merchant_business.domain.requests.UpdateMerchantBusinessRequest;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;
import com.sanedge.merchant_business.entity.MerchantBusinessInformation;
import com.sanedge.merchant_business.repository.MerchantBusinessCommandRepository;
import com.sanedge.merchant_business.repository.MerchantBusinessQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.merchant.MerchantQueryService;
import pb.merchant.MerchantCommon;

@ExtendWith(MockitoExtension.class)
class MerchantBusinessCommandServiceImplTest {

    @Mock
    private MerchantBusinessCommandRepository merchantBusinessCommandRepository;

    @Mock
    private MerchantBusinessQueryRepository merchantBusinessQueryRepository;

    @Mock
    private Validator validator;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    @Mock
    private MerchantQueryService merchantQueryService;

    private MerchantBusinessCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MerchantBusinessCommandServiceImpl(
                merchantBusinessCommandRepository,
                merchantBusinessQueryRepository,
                validator,
                redisService,
                tracingMetrics,
                merchantQueryService);

        lenient().when(validator.validate(any())).thenReturn(new HashSet<>());

        lenient().when(redisService.deleteReactive(anyString()))
                .thenReturn(Uni.createFrom().voidItem());

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

        lenient().when(merchantBusinessCommandRepository.restoreAllDeleted())
                .thenReturn(Uni.createFrom().item(true));
        lenient().when(merchantBusinessCommandRepository.deleteAllDeleted())
                .thenReturn(Uni.createFrom().item(true));
    }

    private MerchantBusinessInformation createTestEntity(Long id, Integer merchantId, String taxId) {
        MerchantBusinessInformation info = new MerchantBusinessInformation();
        info.id = 1L;
        info.setMerchantId(merchantId);
        info.setBusinessType("Retail");
        info.setTaxId(taxId);
        info.setEstablishedYear(2020);
        info.setNumberOfEmployees(50);
        info.setWebsiteUrl("https://example.com");
        info.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        info.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return info;
    }

    private CreateMerchantBusinessRequest createValidCreateRequest() {
        CreateMerchantBusinessRequest req = new CreateMerchantBusinessRequest();
        req.setMerchantId(1);
        req.setBusinessType("Retail");
        req.setTaxId("TAX-001");
        req.setEstablishedYear(2020);
        req.setNumberOfEmployees(50);
        req.setWebsiteUrl("https://example.com");
        return req;
    }

    private UpdateMerchantBusinessRequest createValidUpdateRequest() {
        UpdateMerchantBusinessRequest req = new UpdateMerchantBusinessRequest();
        req.setMerchantBusinessInfoId(1); // set meskipun @Null, karena validasi dimock
        req.setBusinessType("Wholesale");
        req.setTaxId("TAX-002");
        req.setEstablishedYear(2021);
        req.setNumberOfEmployees(100);
        req.setWebsiteUrl("https://example.org");
        return req;
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
    @DisplayName("create merchant business tests")
    class CreateTests {

        @Test
        @DisplayName("should successfully create business info")
        void create_Success() {
            CreateMerchantBusinessRequest req = createValidCreateRequest();
            mockMerchantExists(req.getMerchantId());

            when(merchantBusinessCommandRepository.persist(any(MerchantBusinessInformation.class)))
                    .thenAnswer(inv -> {
                        MerchantBusinessInformation b = inv.getArgument(0);
                        b.id = 1L; // id diset oleh DB
                        return Uni.createFrom().item(b);
                    });

            ApiResponse<MerchantBusinessResponse> response = service.createMerchantBusiness(req)
                    .await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant business info created successfully");
            assertThat(response.data().getId()).isEqualTo(1L);
            verify(redisService).deleteReactive("merchantbusiness:id:1");
        }

        @Test
        @DisplayName("should fail when merchant not found")
        void create_MerchantNotFound() {
            CreateMerchantBusinessRequest req = createValidCreateRequest();
            mockMerchantNotFound();

            assertThatThrownBy(() -> service.createMerchantBusiness(req).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant not found with id " + req.getMerchantId());
        }

        @Test
        @DisplayName("should throw ConstraintViolationException when validation fails")
        void create_ValidationFailure() {
            CreateMerchantBusinessRequest req = new CreateMerchantBusinessRequest();
            Set<ConstraintViolation<CreateMerchantBusinessRequest>> violations = new HashSet<>();

            @SuppressWarnings("unchecked")
            ConstraintViolation<CreateMerchantBusinessRequest> mockViolation = org.mockito.Mockito
                    .mock(ConstraintViolation.class);
            jakarta.validation.Path mockPath = org.mockito.Mockito.mock(jakarta.validation.Path.class);

            lenient().when(mockViolation.getPropertyPath()).thenReturn(mockPath);
            lenient().when(mockViolation.getMessage()).thenReturn("must not be blank");
            violations.add(mockViolation);

            when(validator.validate(any(CreateMerchantBusinessRequest.class))).thenReturn(violations);

            assertThatThrownBy(() -> service.createMerchantBusiness(req).await().indefinitely())
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("update merchant business tests")
    class UpdateTests {

        @Test
        @DisplayName("should successfully update business info")
        void update_Success() {
            UpdateMerchantBusinessRequest req = createValidUpdateRequest();
            MerchantBusinessInformation existing = createTestEntity(1L, 1, "TAX-OLD");

            when(merchantBusinessQueryRepository.findMerchantBusinessInformationById(1L))
                    .thenReturn(Uni.createFrom().item(existing));
            when(merchantBusinessCommandRepository.persist(any(MerchantBusinessInformation.class)))
                    .thenAnswer(inv -> {
                        MerchantBusinessInformation b = inv.getArgument(0);
                        return Uni.createFrom().item(b);
                    });

            ApiResponse<MerchantBusinessResponse> response = service.updateMerchantBusiness(req)
                    .await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant business info updated successfully");
            assertThat(response.data().getTaxId()).isEqualTo("TAX-002");
            verify(redisService).deleteReactive("merchantbusiness:id:1");
        }

        @Test
        @DisplayName("should fail when business info not found")
        void update_NotFound() {
            UpdateMerchantBusinessRequest req = createValidUpdateRequest();
            when(merchantBusinessQueryRepository.findMerchantBusinessInformationById(1L))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThatThrownBy(() -> service.updateMerchantBusiness(req).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant business info not found with id 1");
        }

        @Test
        @DisplayName("should fail when merchantBusinessInfoId is null")
        void update_NullId() {
            UpdateMerchantBusinessRequest req = new UpdateMerchantBusinessRequest(); // id null
            assertThatThrownBy(() -> service.updateMerchantBusiness(req).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("MerchantBusinessInfoId is required");
        }
    }

    @Nested
    @DisplayName("trash merchant business tests")
    class TrashTests {

        @Test
        @DisplayName("should successfully trash business info")
        void trash_Success() {
            Long id = 1L;
            MerchantBusinessInformation info = createTestEntity(id, 1, "TAX-TRASH");
            when(merchantBusinessCommandRepository.trashed(id)).thenReturn(Uni.createFrom().item(info));

            ApiResponse<MerchantBusinessResponseDeleteAt> response = service.trashedMerchantBusiness(id)
                    .await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant business info trashed successfully");
            assertThat(response.data().getId()).isEqualTo(id);
            verify(redisService).deleteReactive("merchantbusiness:id:1");
        }

        @Test
        @DisplayName("should fail when business info not found")
        void trash_NotFound() {
            when(merchantBusinessCommandRepository.trashed(999L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.trashedMerchantBusiness(999L).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant business info not found or already trashed");
        }
    }

    @Nested
    @DisplayName("restore merchant business tests")
    class RestoreTests {

        @Test
        @DisplayName("should successfully restore business info")
        void restore_Success() {
            Long id = 1L;
            MerchantBusinessInformation info = createTestEntity(id, 1, "TAX-RESTORE");
            when(merchantBusinessCommandRepository.restore(id)).thenReturn(Uni.createFrom().item(info));

            ApiResponse<MerchantBusinessResponseDeleteAt> response = service.restoreMerchantBusiness(id)
                    .await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant business info restored successfully");
            verify(redisService).deleteReactive("merchantbusiness:id:1");
        }

        @Test
        @DisplayName("should fail when business info is not trashed")
        void restore_NotTrashed() {
            when(merchantBusinessCommandRepository.restore(1L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.restoreMerchantBusiness(1L).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant business info not found or not trashed");
        }
    }

    @Nested
    @DisplayName("delete permanent merchant business tests")
    class DeletePermanentTests {

        @Test
        @DisplayName("should permanently delete business info")
        void deletePermanent_Success() {
            Long id = 1L;
            MerchantBusinessInformation info = createTestEntity(id, 1, "TAX-DEL");
            info.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(merchantBusinessCommandRepository.deletePermanent(id)).thenReturn(Uni.createFrom().item(info));

            ApiResponse<Void> response = service.deleteMerchantBusinessPermanent(id).await().indefinitely();
            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant business info permanently deleted");
            verify(redisService).deleteReactive("merchantbusiness:id:1");
        }

        @Test
        @DisplayName("should fail when business info not found or not trashed")
        void deletePermanent_NotFound() {
            when(merchantBusinessCommandRepository.deletePermanent(999L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.deleteMerchantBusinessPermanent(999L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining(
                            "Merchant business info not found or must be trashed before permanent deletion");
        }
    }

    @Nested
    @DisplayName("restore all merchant business tests")
    class RestoreAllTests {

        @Test
        @DisplayName("should restore all trashed business info")
        void restoreAll_Success() {
            ApiResponse<Void> response = service.restoreAllMerchantBusiness().await().indefinitely();
            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("All trashed merchant business info restored");
        }

        @Test
        @DisplayName("should fail when no trashed business info")
        void restoreAll_NoTrashed() {
            when(merchantBusinessCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAllMerchantBusiness().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed merchant business info found");
        }
    }

    @Nested
    @DisplayName("delete all permanent merchant business tests")
    class DeleteAllTests {

        @Test
        @DisplayName("should delete all trashed business info")
        void deleteAll_Success() {
            ApiResponse<Void> response = service.deleteAllMerchantBusinessPermanent().await().indefinitely();
            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("All trashed merchant business info permanently deleted");
        }

        @Test
        @DisplayName("should fail when no trashed business info")
        void deleteAll_NoTrashed() {
            when(merchantBusinessCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAllMerchantBusinessPermanent().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed merchant business info found");
        }
    }
}