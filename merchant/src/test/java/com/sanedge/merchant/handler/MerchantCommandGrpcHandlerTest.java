package com.sanedge.merchant.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.service.MerchantCommandService;
import com.sanedge.merchant.service.MerchantQueryService;

import io.smallrye.mutiny.Uni;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;
import pb.merchant.MerchantCommon.ApiResponseMerchant;
import pb.merchant.MerchantCommon.ApiResponseMerchantAll;
import pb.merchant.MerchantCommon.ApiResponseMerchantDelete;
import pb.merchant.MerchantCommon.ApiResponseMerchantDeleteAt;
import pb.merchant.MerchantCommon.FindByIdMerchantRequest;
import pb.user.UserCommon.UserResponse;

@ExtendWith(MockitoExtension.class)
class MerchantCommandGrpcHandlerTest {

        @Mock
        MerchantCommandService merchantCommandService;

        @Mock
        MerchantQueryService merchantQueryService;

        @InjectMocks
        MerchantCommandGrpcHandler handler;

        private MerchantResponse merchantResponse;
        private MerchantResponseDeleteAt merchantResponseDeleteAt;

        @BeforeEach
        void setUp() {
                org.mockito.MockitoAnnotations.openMocks(this);

                merchantResponse = MerchantResponse.builder()
                                .id(1L)
                                .userId(1)
                                .name("Test Merchant")
                                .description("Test Description")
                                .address("Test Address")
                                .contactEmail("test@merchant.com")
                                .contactPhone("081234567890")
                                .status("ACTIVE")
                                .createdAt("2024-01-01T00:00:00")
                                .updatedAt("2024-01-01T00:00:00")
                                .build();

                merchantResponseDeleteAt = MerchantResponseDeleteAt.builder()
                                .id(1L)
                                .userId(1)
                                .name("Test Merchant")
                                .description("Test Description")
                                .address("Test Address")
                                .contactEmail("test@merchant.com")
                                .contactPhone("081234567890")
                                .status("PENDING")
                                .createdAt("2024-01-01T00:00:00")
                                .updatedAt("2024-01-01T00:00:00")
                                .deletedAt("2024-01-02T00:00:00")
                                .build();
        }

        @Test
        void create_shouldReturnSuccess() {
                ApiResponse<MerchantResponse> serviceResponse = ApiResponse.success("Merchant created successfully",
                                merchantResponse);
                lenient().when(merchantCommandService.createMerchant(any()))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                CreateMerchantRequest request = CreateMerchantRequest.newBuilder()
                                .setName("Test Merchant")
                                .setUserId(1)
                                .build();

                ApiResponseMerchant response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant created successfully");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getName()).isEqualTo("Test Merchant");
        }

        @Test
        void create_shouldReturnFailure_onServiceError() {
                lenient().when(merchantCommandService.createMerchant(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("User not found")));

                CreateMerchantRequest request = CreateMerchantRequest.newBuilder()
                                .setName("Test Merchant")
                                .setUserId(999)
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.create(request).await().indefinitely());
        }

        @Test
        void update_shouldReturnSuccess() {
                ApiResponse<MerchantResponse> serviceResponse = ApiResponse.success("Merchant updated successfully",
                                merchantResponse);
                lenient().when(merchantCommandService.updateMerchant(any()))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                UpdateMerchantRequest request = UpdateMerchantRequest.newBuilder()
                                .setMerchantId(1)
                                .setName("Updated Merchant")
                                .setUserId(1)
                                .setStatus("ACTIVE")
                                .build();

                ApiResponseMerchant response = handler.update(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant updated successfully");
                assertThat(response.hasData()).isTrue();
        }

        @Test
        void update_shouldReturnFailure_onServiceError() {
                lenient().when(merchantCommandService.updateMerchant(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Merchant not found")));

                UpdateMerchantRequest request = UpdateMerchantRequest.newBuilder()
                                .setMerchantId(999)
                                .setName("Test")
                                .setUserId(1)
                                .setStatus("ACTIVE")
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.update(request).await().indefinitely());
        }

        @Test
        void updateStatus_shouldReturnSuccess() {
                ApiResponse<MerchantResponse> findByIdResponse = ApiResponse.success("found", merchantResponse);
                lenient().when(merchantQueryService.findById(1L))
                                .thenReturn(Uni.createFrom().item(findByIdResponse));

                ApiResponse<MerchantResponse> updateResponse = ApiResponse.success("Merchant updated successfully",
                                merchantResponse);
                lenient().when(merchantCommandService.updateMerchant(any()))
                                .thenReturn(Uni.createFrom().item(updateResponse));

                UpdateMerchantStatusRequest request = UpdateMerchantStatusRequest.newBuilder()
                                .setMerchantId(1)
                                .setStatus("ACTIVE")
                                .build();

                ApiResponseMerchant response = handler.updateStatus(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        void updateStatus_shouldReturnFailure_whenMerchantNotFound() {
                ApiResponse<MerchantResponse> findByIdResponse = ApiResponse.success("not found", null);
                lenient().when(merchantQueryService.findById(999L))
                                .thenReturn(Uni.createFrom().item(findByIdResponse));

                UpdateMerchantStatusRequest request = UpdateMerchantStatusRequest.newBuilder()
                                .setMerchantId(999)
                                .setStatus("ACTIVE")
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.updateStatus(request).await().indefinitely());
        }

        @Test
        void trashMerchant_shouldReturnSuccess() {
                ApiResponse<MerchantResponseDeleteAt> serviceResponse = ApiResponse
                                .success("Merchant trashed successfully", merchantResponseDeleteAt);
                lenient().when(merchantCommandService.trashMerchant(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                                .setId(1)
                                .build();

                ApiResponseMerchantDeleteAt response = handler.trashedMerchant(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant trashed successfully");
                assertThat(response.hasData()).isTrue();
        }

        @Test
        void trashMerchant_shouldReturnFailure_onServiceError() {
                lenient().when(merchantCommandService.trashMerchant(999L))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Merchant not found")));

                FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                                .setId(999)
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.trashedMerchant(request).await().indefinitely());
        }

        @Test
        void restoreMerchant_shouldReturnSuccess() {
                ApiResponse<MerchantResponseDeleteAt> serviceResponse = ApiResponse
                                .success("Merchant restored successfully", merchantResponseDeleteAt);
                lenient().when(merchantCommandService.restoreMerchant(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                                .setId(1)
                                .build();

                ApiResponseMerchant response = handler.restoreMerchant(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant restored successfully");
        }

        @Test
        void restoreMerchant_shouldReturnFailure_onServiceError() {
                lenient().when(merchantCommandService.restoreMerchant(999L))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Merchant not found")));

                FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                                .setId(999)
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.restoreMerchant(request).await().indefinitely());
        }

        @Test
        void deleteMerchantPermanent_shouldReturnSuccess() {
                ApiResponse<Void> serviceResponse = ApiResponse.success("Merchant permanently deleted");
                lenient().when(merchantCommandService.deleteMerchant(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                                .setId(1)
                                .build();

                ApiResponseMerchantDelete response = handler.deleteMerchantPermanent(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant permanently deleted");
        }

        @Test
        void deleteMerchantPermanent_shouldReturnFailure_onServiceError() {
                lenient().when(merchantCommandService.deleteMerchant(999L))
                                .thenReturn(Uni.createFrom().failure(
                                                new RuntimeException("Merchant not found or must be trashed")));

                FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                                .setId(999)
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.deleteMerchantPermanent(request).await().indefinitely());
        }

        @Test
        void restoreAllMerchant_shouldReturnSuccess() {
                ApiResponse<Void> serviceResponse = ApiResponse.success("Restored all trashed merchants");
                lenient().when(merchantCommandService.restoreAll())
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                ApiResponseMerchantAll response = handler
                                .restoreAllMerchant(com.google.protobuf.Empty.getDefaultInstance()).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Restored all trashed merchants");
        }

        @Test
        void restoreAllMerchant_shouldReturnFailure_whenNoTrashedMerchants() {
                lenient().when(merchantCommandService.restoreAll())
                                .thenReturn(Uni.createFrom()
                                                .failure(new RuntimeException("No trashed merchants found")));

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.restoreAllMerchant(com.google.protobuf.Empty.getDefaultInstance()).await()
                                                .indefinitely());
        }

        @Test
        void deleteAllMerchantPermanent_shouldReturnSuccess() {
                ApiResponse<Void> serviceResponse = ApiResponse.success("Deleted all trashed merchants");
                lenient().when(merchantCommandService.deleteAll())
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                ApiResponseMerchantAll response = handler
                                .deleteAllMerchantPermanent(com.google.protobuf.Empty.getDefaultInstance()).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Deleted all trashed merchants");
        }

        @Test
        void deleteAllMerchantPermanent_shouldReturnFailure_whenNoTrashedMerchants() {
                lenient().when(merchantCommandService.deleteAll())
                                .thenReturn(Uni.createFrom()
                                                .failure(new RuntimeException("No trashed merchants found")));

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.deleteAllMerchantPermanent(com.google.protobuf.Empty.getDefaultInstance())
                                                .await().indefinitely());
        }
}
