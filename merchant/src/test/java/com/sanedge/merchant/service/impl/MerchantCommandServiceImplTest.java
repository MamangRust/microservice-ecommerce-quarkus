package com.sanedge.merchant.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.domain.requests.CreateMerchantRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantRequest;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.repository.MerchantCommandRepository;
import com.sanedge.merchant.repository.MerchantQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.user.UserCommon;
import pb.user.UserQueryService;

@ExtendWith(MockitoExtension.class)
class MerchantCommandServiceImplTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private MerchantQueryRepository merchantQueryRepository;

    @Mock
    private MerchantCommandRepository merchantCommandRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private MerchantCommandServiceImpl merchantCommandService;

    @BeforeEach
    void setUp() {
        merchantCommandService = new MerchantCommandServiceImpl(
                userQueryService,
                merchantQueryRepository,
                merchantCommandRepository,
                redisService,
                tracingMetrics);
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                        .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().when(redisService.deleteReactive(anyString()))
                .thenReturn(Uni.createFrom().voidItem());
    }

    private Merchant createMockMerchant(Integer userId, String name, Status status) {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(1L);
        merchant.setUserId(userId);
        merchant.setName(name);
        merchant.setDescription("Test Description");
        merchant.setAddress("Test Address");
        merchant.setContactEmail("test@merchant.com");
        merchant.setContactPhone("081234567890");
        merchant.setStatus(status);
        merchant.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        merchant.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return merchant;
    }

    private UserCommon.ApiResponseUser createMockUserResponse() {
        UserCommon.UserResponse userResponse = UserCommon.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john.doe@example.com")
                .build();

        return UserCommon.ApiResponseUser.newBuilder()
                .setStatus("success")
                .setMessage("User found")
                .setData(userResponse)
                .build();
    }

    @Test
    void createMerchant_success_createsNewMerchant() {
        CreateMerchantRequest request = new CreateMerchantRequest();
        request.setName("New Merchant");
        request.setUserId(1);
        request.setDescription("Test Description");
        request.setAddress("Test Address");
        request.setContactEmail("test@merchant.com");
        request.setContactPhone("081234567890");
        request.setStatus("PENDING");

        lenient().when(userQueryService.findById(
                UserCommon.FindByIdUserRequest.newBuilder().setId(1).build()))
                .thenReturn(Uni.createFrom().item(createMockUserResponse()));
        lenient().when(merchantQueryRepository.existsByName("New Merchant"))
                .thenReturn(Uni.createFrom().item(false));
        lenient().when(merchantCommandRepository.persist(any(Merchant.class)))
                .thenAnswer(invocation -> {
                    Merchant m = invocation.getArgument(0);
                    m.setMerchantId(1L);
                    return Uni.createFrom().item(m);
                });

        ApiResponse<MerchantResponse> response = merchantCommandService.createMerchant(request).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchant created successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("New Merchant");
    }

    @Test
    void createMerchant_alreadyExists_throwsResourceAlreadyExistsException() {
        CreateMerchantRequest request = new CreateMerchantRequest();
        request.setName("Existing Merchant");
        request.setUserId(1);
        request.setDescription("Test Description");
        request.setAddress("Test Address");
        request.setContactEmail("test@merchant.com");
        request.setContactPhone("081234567890");
        request.setStatus("PENDING");

        lenient().when(userQueryService.findById(
                UserCommon.FindByIdUserRequest.newBuilder().setId(1).build()))
                .thenReturn(Uni.createFrom().item(createMockUserResponse()));
        when(merchantQueryRepository.existsByName("Existing Merchant"))
                .thenReturn(Uni.createFrom().item(true));

        try {
            merchantCommandService.createMerchant(request).await().indefinitely();
            Assertions.fail("Expected ResourceAlreadyExistsException");
        } catch (ResourceAlreadyExistsException e) {
            assertThat(e.getMessage()).contains("already taken");
        }
    }

    @Test
    void createMerchant_userNotFound_throwsResourceNotFoundException() {
        CreateMerchantRequest request = new CreateMerchantRequest();
        request.setName("New Merchant");
        request.setUserId(999);
        request.setDescription("Test Description");
        request.setAddress("Test Address");
        request.setContactEmail("test@merchant.com");
        request.setContactPhone("081234567890");
        request.setStatus("PENDING");

        UserCommon.ApiResponseUser userResponse = UserCommon.ApiResponseUser.newBuilder()
                .setStatus("error")
                .setMessage("User not found")
                .build();

        when(userQueryService.findById(
                UserCommon.FindByIdUserRequest.newBuilder().setId(999).build()))
                .thenReturn(Uni.createFrom().item(userResponse));

        try {
            merchantCommandService.createMerchant(request).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("User not found");
        }
    }

    @Test
    void updateMerchant_success_updatesMerchant() {
        UpdateMerchantRequest request = new UpdateMerchantRequest();
        request.setMerchantId(1);
        request.setName("Updated Merchant");
        request.setUserId(1);
        request.setDescription("Updated Description");
        request.setAddress("Updated Address");
        request.setContactEmail("updated@merchant.com");
        request.setContactPhone("089876543210");
        request.setStatus("SUCCESS");

        Merchant existingMerchant = createMockMerchant(1, "Old Merchant", Status.PENDING);

        lenient().when(merchantQueryRepository.findMerchantById(1L))
                .thenReturn(Uni.createFrom().item(existingMerchant));
        lenient().when(userQueryService.findById(
                UserCommon.FindByIdUserRequest.newBuilder().setId(1).build()))
                .thenReturn(Uni.createFrom().item(createMockUserResponse()));
        lenient().when(merchantCommandRepository.persist(any(Merchant.class)))
                .thenReturn(Uni.createFrom().item(existingMerchant));

        ApiResponse<MerchantResponse> response = merchantCommandService.updateMerchant(request).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchant updated successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("Updated Merchant");
    }

    @Test
    void updateMerchant_notFound_throwsResourceNotFoundException() {
        UpdateMerchantRequest request = new UpdateMerchantRequest();
        request.setMerchantId(999);
        request.setName("Updated Merchant");
        request.setUserId(1);
        request.setDescription("Updated Description");
        request.setAddress("Updated Address");
        request.setContactEmail("updated@merchant.com");
        request.setContactPhone("089876543210");
        request.setStatus("SUCCESS");

        when(merchantQueryRepository.findMerchantById(999L))
                .thenReturn(Uni.createFrom().nullItem());

        try {
            merchantCommandService.updateMerchant(request).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Merchant not found");
        }
    }

    @Test
    void trashMerchant_success_trashesMerchant() {
        Merchant merchant = createMockMerchant(1, "Merchant to Trash", Status.PENDING);
        merchant.setDeletedAt(new Timestamp(System.currentTimeMillis()));

        lenient().when(merchantCommandRepository.trashed(1L))
                .thenReturn(Uni.createFrom().item(merchant));

        ApiResponse<MerchantResponseDeleteAt> response = merchantCommandService.trashMerchant(1L).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchant trashed successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("Merchant to Trash");
    }

    @Test
    void trashMerchant_notFound_throwsResourceNotFoundException() {
        when(merchantCommandRepository.trashed(999L))
                .thenReturn(Uni.createFrom().nullItem());

        try {
            merchantCommandService.trashMerchant(999L).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Merchant not found");
        }
    }

    @Test
    void restoreMerchant_success_restoresMerchant() {
        Merchant restoredMerchant = createMockMerchant(1, "Restored Merchant", Status.PENDING);
        restoredMerchant.setDeletedAt(null);

        lenient().when(merchantCommandRepository.restore(1L))
                .thenReturn(Uni.createFrom().item(restoredMerchant));

        ApiResponse<MerchantResponseDeleteAt> response = merchantCommandService.restoreMerchant(1L).await()
                .indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchant restored successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("Restored Merchant");
    }

    @Test
    void restoreMerchant_notFound_throwsResourceNotFoundException() {
        when(merchantCommandRepository.restore(999L))
                .thenReturn(Uni.createFrom().nullItem());

        try {
            merchantCommandService.restoreMerchant(999L).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Merchant not found");
        }
    }

    @Test
    void deleteMerchant_success_deletesMerchant() {
        Merchant deletedMerchant = createMockMerchant(1, "Deleted Merchant", Status.PENDING);
        deletedMerchant.setDeletedAt(new Timestamp(System.currentTimeMillis()));

        lenient().when(merchantCommandRepository.deletePermanent(1L))
                .thenReturn(Uni.createFrom().item(deletedMerchant));

        ApiResponse<Void> response = merchantCommandService.deleteMerchant(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Merchant permanently deleted");
    }

    @Test
    void deleteMerchant_notFound_throwsInvalidRequestException() {
        when(merchantCommandRepository.deletePermanent(999L))
                .thenReturn(Uni.createFrom().nullItem());

        try {
            merchantCommandService.deleteMerchant(999L).await().indefinitely();
            Assertions.fail("Expected InvalidRequestException");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage()).contains("not found or must be trashed");
        }
    }

    @Test
    void restoreAll_success_restoresAll() {
        lenient().when(merchantCommandRepository.restoreAllDeleted())
                .thenReturn(Uni.createFrom().item(true));

        ApiResponse<Void> response = merchantCommandService.restoreAll().await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).contains("Restored all trashed merchants");
    }

    @Test
    void restoreAll_noneFound_throwsResourceNotFoundException() {
        when(merchantCommandRepository.restoreAllDeleted())
                .thenReturn(Uni.createFrom().item(false));

        try {
            merchantCommandService.restoreAll().await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No trashed merchants found");
        }
    }

    @Test
    void deleteAll_success_deletesAll() {
        lenient().when(merchantCommandRepository.deleteAllDeleted())
                .thenReturn(Uni.createFrom().item(true));

        ApiResponse<Void> response = merchantCommandService.deleteAll().await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).contains("Deleted all trashed merchants");
    }

    @Test
    void deleteAll_noneFound_throwsResourceNotFoundException() {
        when(merchantCommandRepository.deleteAllDeleted())
                .thenReturn(Uni.createFrom().item(false));

        try {
            merchantCommandService.deleteAll().await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No trashed merchants found");
        }
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