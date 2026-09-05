package com.sanedge.merchant_policy.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.sanedge.merchant_policy.domain.requests.CreateMerchantPolicyRequest;
import com.sanedge.merchant_policy.domain.requests.UpdateMerchantPolicyRequest;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;
import com.sanedge.merchant_policy.entity.MerchantPolicy;
import com.sanedge.merchant_policy.repository.MerchantPolicyCommandRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.merchant.MerchantQueryService;
import pb.merchant.MerchantCommon;

@ExtendWith(MockitoExtension.class)
class MerchantPolicyCommandServiceImplTest {

    @Mock
    private MerchantPolicyCommandRepository merchantPolicyCommandRepository;

    @Mock
    private Validator validator;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    @Mock
    private MerchantQueryService merchantQueryService;

    private MerchantPolicyCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MerchantPolicyCommandServiceImpl(
                merchantPolicyCommandRepository,
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

        lenient().when(merchantPolicyCommandRepository.restoreAllDeleted())
                .thenReturn(Uni.createFrom().item(true));
        lenient().when(merchantPolicyCommandRepository.deleteAllDeleted())
                .thenReturn(Uni.createFrom().item(true));
    }

    private MerchantPolicy createTestPolicy(Long id, Integer merchantId, String title) {
        MerchantPolicy policy = new MerchantPolicy();
        policy.id = 1L;
        policy.setMerchantId(merchantId);
        policy.setPolicyType("RETURN");
        policy.setTitle(title);
        policy.setDescription("Test description");
        policy.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        policy.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return policy;
    }

    private CreateMerchantPolicyRequest createValidCreateRequest() {
        CreateMerchantPolicyRequest req = new CreateMerchantPolicyRequest();
        req.setMerchantId(1);
        req.setPolicyType("RETURN");
        req.setTitle("Return Policy");
        req.setDescription("Return within 30 days");
        return req;
    }

    private UpdateMerchantPolicyRequest createValidUpdateRequest() {
        UpdateMerchantPolicyRequest req = new UpdateMerchantPolicyRequest();
        req.setMerchantPolicyId(1);
        req.setPolicyType("PRIVACY");
        req.setTitle("Privacy Policy");
        req.setDescription("We respect your privacy");
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
    @DisplayName("create merchant policy tests")
    class CreateTests {

        @Test
        @DisplayName("should successfully create policy")
        void create_Success() {
            CreateMerchantPolicyRequest req = createValidCreateRequest();
            mockMerchantExists(req.getMerchantId());

            when(merchantPolicyCommandRepository.persist(any(MerchantPolicy.class)))
                    .thenAnswer(inv -> {
                        MerchantPolicy p = inv.getArgument(0);
                        p.id = 1L;
                        return Uni.createFrom().item(p);
                    });

            ApiResponse<MerchantPoliciesResponse> response = service.create(req)
                    .await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant policy created successfully!");
            assertThat(response.data().getId()).isEqualTo(1L);
            assertThat(response.data().getTitle()).isEqualTo("Return Policy");
            verify(redisService).deleteReactive("merchantpolicy:id:1");
        }

        @Test
        @DisplayName("should fail when merchant not found")
        void create_MerchantNotFound() {
            CreateMerchantPolicyRequest req = createValidCreateRequest();
            mockMerchantNotFound();

            assertThatThrownBy(() -> service.create(req).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant not found with id " + req.getMerchantId());
        }

        @Test
        @DisplayName("should throw ConstraintViolationException when validation fails")
        void create_ValidationFailure() {
            CreateMerchantPolicyRequest invalidReq = new CreateMerchantPolicyRequest(); // empty
            Set<ConstraintViolation<CreateMerchantPolicyRequest>> violations = new HashSet<>();
            ConstraintViolation<CreateMerchantPolicyRequest> mockViolation =
                    org.mockito.Mockito.mock(ConstraintViolation.class);
            jakarta.validation.Path mockPath = org.mockito.Mockito.mock(jakarta.validation.Path.class);
            lenient().when(mockViolation.getPropertyPath()).thenReturn(mockPath);
            lenient().when(mockViolation.getMessage()).thenReturn("must not be blank");
            violations.add(mockViolation);

            when(validator.validate(any(CreateMerchantPolicyRequest.class))).thenReturn(violations);

            assertThatThrownBy(() -> service.create(invalidReq).await().indefinitely())
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("update merchant policy tests")
    class UpdateTests {

        @Test
        @DisplayName("should successfully update policy")
        void update_Success() {
            UpdateMerchantPolicyRequest req = createValidUpdateRequest();
            MerchantPolicy existing = createTestPolicy(1L, 1, "Old Policy");

            when(merchantPolicyCommandRepository.findById(1L))
                    .thenReturn(Uni.createFrom().item(existing));
            when(merchantPolicyCommandRepository.persist(any(MerchantPolicy.class)))
                    .thenAnswer(inv -> {
                        MerchantPolicy p = inv.getArgument(0);
                        return Uni.createFrom().item(p);
                    });

            ApiResponse<MerchantPoliciesResponse> response = service.update(req)
                    .await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant policy updated successfully!");
            assertThat(response.data().getTitle()).isEqualTo("Privacy Policy");
            verify(redisService).deleteReactive("merchantpolicy:id:1");
        }

        @Test
        @DisplayName("should fail when policy not found")
        void update_NotFound() {
            UpdateMerchantPolicyRequest req = createValidUpdateRequest();
            when(merchantPolicyCommandRepository.findById(1L))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThatThrownBy(() -> service.update(req).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant policy not found with id 1");
        }

        @Test
        @DisplayName("should fail when merchantPolicyId is null")
        void update_NullId() {
            UpdateMerchantPolicyRequest req = new UpdateMerchantPolicyRequest(); // null id

            assertThatThrownBy(() -> service.update(req).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("MerchantPolicyId is required");
        }
    }

    @Nested
    @DisplayName("trash merchant policy tests")
    class TrashTests {

        @Test
        @DisplayName("should successfully trash policy")
        void trash_Success() {
            Long id = 1L;
            MerchantPolicy policy = createTestPolicy(id, 1, "Trashable");
            when(merchantPolicyCommandRepository.trash(id)).thenReturn(Uni.createFrom().item(policy));

            ApiResponse<MerchantPoliciesResponseDeleteAt> response = service.trash(id).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant policy trashed successfully!");
            assertThat(response.data().getId()).isEqualTo(id);
            verify(redisService).deleteReactive("merchantpolicy:id:1");
        }

        @Test
        @DisplayName("should fail when policy not found or already trashed")
        void trash_NotFound() {
            when(merchantPolicyCommandRepository.trash(999L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.trash(999L).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant policy not found or already trashed");
        }
    }

    @Nested
    @DisplayName("restore merchant policy tests")
    class RestoreTests {

        @Test
        @DisplayName("should successfully restore policy")
        void restore_Success() {
            Long id = 1L;
            MerchantPolicy policy = createTestPolicy(id, 1, "Restorable");
            when(merchantPolicyCommandRepository.restore(id)).thenReturn(Uni.createFrom().item(policy));

            ApiResponse<MerchantPoliciesResponseDeleteAt> response = service.restore(id).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant policy restored successfully!");
            verify(redisService).deleteReactive("merchantpolicy:id:1");
        }

        @Test
        @DisplayName("should fail when policy is not trashed")
        void restore_NotTrashed() {
            when(merchantPolicyCommandRepository.restore(1L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.restore(1L).await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Merchant policy not found or not trashed");
        }
    }

    @Nested
    @DisplayName("delete permanent merchant policy tests")
    class DeletePermanentTests {

        @Test
        @DisplayName("should permanently delete policy")
        void deletePermanent_Success() {
            Long id = 1L;
            MerchantPolicy policy = createTestPolicy(id, 1, "ToDelete");
            policy.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(merchantPolicyCommandRepository.deletePermanent(id)).thenReturn(Uni.createFrom().item(policy));

            ApiResponse<Void> response = service.delete(id).await().indefinitely();

            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Merchant policy permanently deleted!");
            verify(redisService).deleteReactive("merchantpolicy:id:1");
        }

        @Test
        @DisplayName("should fail when policy not found or not trashed")
        void deletePermanent_NotFound() {
            when(merchantPolicyCommandRepository.deletePermanent(999L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.delete(999L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Merchant policy not found or must be trashed before permanent deletion");
        }
    }

    @Nested
    @DisplayName("restore all merchant policies tests")
    class RestoreAllTests {

        @Test
        @DisplayName("should restore all trashed policies")
        void restoreAll_Success() {
            ApiResponse<Void> response = service.restoreAll().await().indefinitely();
            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("All merchant policies restored successfully!");
        }

        @Test
        @DisplayName("should fail when no trashed policies found")
        void restoreAll_NoTrashed() {
            when(merchantPolicyCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed merchant policies found");
        }
    }

    @Nested
    @DisplayName("delete all permanent merchant policies tests")
    class DeleteAllTests {

        @Test
        @DisplayName("should delete all trashed policies")
        void deleteAll_Success() {
            ApiResponse<Void> response = service.deleteAll().await().indefinitely();
            assertThat(response.status()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("All merchant policies permanently deleted!");
        }

        @Test
        @DisplayName("should fail when no trashed policies found")
        void deleteAll_NoTrashed() {
            when(merchantPolicyCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed merchant policies found");
        }
    }
}