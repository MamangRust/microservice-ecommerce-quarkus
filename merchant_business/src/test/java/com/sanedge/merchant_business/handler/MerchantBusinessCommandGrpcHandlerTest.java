package com.sanedge.merchant_business.handler;

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
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;
import com.sanedge.merchant_business.service.MerchantBusinessCommandService;

import io.smallrye.mutiny.Uni;
import pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness;
import pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt;
import pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest;
import pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest;
import pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest;

@ExtendWith(MockitoExtension.class)
class MerchantBusinessCommandGrpcHandlerTest {

        @Mock
        MerchantBusinessCommandService merchantBusinessCommandService;

        @InjectMocks
        MerchantBusinessCommandGrpcHandler handler;

        private MerchantBusinessResponse merchantBusinessResponse;
        private MerchantBusinessResponseDeleteAt merchantBusinessResponseDeleteAt;

        @BeforeEach
        void setUp() {
                org.mockito.MockitoAnnotations.openMocks(this);

                merchantBusinessResponse = MerchantBusinessResponse.builder()
                                .id(1L)
                                .merchantId(1)
                                .businessType("Retail")
                                .taxId("12.345.678.9-012.345")
                                .establishedYear(2020)
                                .numberOfEmployees(50)
                                .websiteUrl("https://example.com")
                                .createdAt("2024-01-01T00:00:00")
                                .updatedAt("2024-01-01T00:00:00")
                                .build();

                merchantBusinessResponseDeleteAt = MerchantBusinessResponseDeleteAt.builder()
                                .id(1L)
                                .merchantId(1)
                                .businessType("Retail")
                                .taxId("12.345.678.9-012.345")
                                .establishedYear(2020)
                                .numberOfEmployees(50)
                                .websiteUrl("https://example.com")
                                .createdAt("2024-01-01T00:00:00")
                                .updatedAt("2024-01-01T00:00:00")
                                .deletedAt("2024-01-02T00:00:00")
                                .build();
        }

        @Test
        void create_shouldReturnSuccess() {
                ApiResponse<MerchantBusinessResponse> serviceResponse = ApiResponse
                                .success("Merchant business info created successfully", merchantBusinessResponse);
                lenient().when(merchantBusinessCommandService.createMerchantBusiness(any()))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                CreateMerchantBusinessRequest request = CreateMerchantBusinessRequest.newBuilder()
                                .setMerchantId(1)
                                .setBusinessType("Retail")
                                .setTaxId("12.345.678.9-012.345")
                                .setEstablishedYear(2020)
                                .setNumberOfEmployees(50)
                                .setWebsiteUrl("https://example.com")
                                .build();

                ApiResponseMerchantBusiness response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant business info created successfully");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getBusinessType()).isEqualTo("Retail");
        }

        @Test
        void create_shouldReturnFailure_onServiceError() {
                lenient().when(merchantBusinessCommandService.createMerchantBusiness(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Merchant not found")));

                CreateMerchantBusinessRequest request = CreateMerchantBusinessRequest.newBuilder()
                                .setMerchantId(999)
                                .setBusinessType("Retail")
                                .setTaxId("12.345.678.9-012.345")
                                .setEstablishedYear(2020)
                                .setNumberOfEmployees(50)
                                .setWebsiteUrl("https://example.com")
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.create(request).await().indefinitely());
        }

        @Test
        void update_shouldReturnSuccess() {
                ApiResponse<MerchantBusinessResponse> serviceResponse = ApiResponse
                                .success("Merchant business info updated successfully", merchantBusinessResponse);
                lenient().when(merchantBusinessCommandService.updateMerchantBusiness(any()))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                UpdateMerchantBusinessRequest request = UpdateMerchantBusinessRequest.newBuilder()
                                .setMerchantBusinessInfoId(1)
                                .setBusinessType("Updated Retail")
                                .setTaxId("98.765.432.1-098.765")
                                .setEstablishedYear(2021)
                                .setNumberOfEmployees(100)
                                .setWebsiteUrl("https://updated.com")
                                .build();

                ApiResponseMerchantBusiness response = handler.update(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant business info updated successfully");
                assertThat(response.hasData()).isTrue();
        }

        @Test
        void update_shouldReturnFailure_onServiceError() {
                lenient().when(merchantBusinessCommandService.updateMerchantBusiness(any()))
                                .thenReturn(Uni.createFrom().failure(
                                                new RuntimeException("Merchant business info not found with id 999")));

                UpdateMerchantBusinessRequest request = UpdateMerchantBusinessRequest.newBuilder()
                                .setMerchantBusinessInfoId(999)
                                .setBusinessType("Retail")
                                .setTaxId("12.345.678.9-012.345")
                                .setEstablishedYear(2020)
                                .setNumberOfEmployees(50)
                                .setWebsiteUrl("https://example.com")
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.update(request).await().indefinitely());
        }

        @Test
        void trashMerchantBusiness_shouldReturnSuccess() {
                ApiResponse<MerchantBusinessResponseDeleteAt> serviceResponse = ApiResponse
                                .success("Merchant business info trashed successfully",
                                                merchantBusinessResponseDeleteAt);
                lenient().when(merchantBusinessCommandService.trashedMerchantBusiness(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                FindByIdMerchantBusinessRequest request = FindByIdMerchantBusinessRequest.newBuilder()
                                .setId(1)
                                .build();

                ApiResponseMerchantBusinessDeleteAt response = handler.trashedMerchantBusiness(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant business info trashed successfully");
                assertThat(response.hasData()).isTrue();
        }

        @Test
        void trashMerchantBusiness_shouldReturnFailure_onServiceError() {
                lenient().when(merchantBusinessCommandService.trashedMerchantBusiness(999L))
                                .thenReturn(Uni.createFrom().failure(
                                                new RuntimeException(
                                                                "Merchant business info not found or already trashed")));

                FindByIdMerchantBusinessRequest request = FindByIdMerchantBusinessRequest.newBuilder()
                                .setId(999)
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.trashedMerchantBusiness(request).await().indefinitely());
        }

        @Test
        void restoreMerchantBusiness_shouldReturnSuccess() {
                ApiResponse<MerchantBusinessResponseDeleteAt> serviceResponse = ApiResponse
                                .success("Merchant business info restored successfully",
                                                merchantBusinessResponseDeleteAt);
                lenient().when(merchantBusinessCommandService.restoreMerchantBusiness(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                FindByIdMerchantBusinessRequest request = FindByIdMerchantBusinessRequest.newBuilder()
                                .setId(1)
                                .build();

                ApiResponseMerchantBusinessDeleteAt response = handler.restoreMerchantBusiness(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant business info restored successfully");
        }

        @Test
        void restoreMerchantBusiness_shouldReturnFailure_onServiceError() {
                lenient().when(merchantBusinessCommandService.restoreMerchantBusiness(999L))
                                .thenReturn(Uni.createFrom().failure(
                                                new RuntimeException(
                                                                "Merchant business info not found or not trashed")));

                FindByIdMerchantBusinessRequest request = FindByIdMerchantBusinessRequest.newBuilder()
                                .setId(999)
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.restoreMerchantBusiness(request).await().indefinitely());
        }

        @Test
        void deleteMerchantBusinessPermanent_shouldReturnSuccess() {
                ApiResponse<Void> serviceResponse = ApiResponse
                                .success("Merchant business info permanently deleted");
                lenient().when(merchantBusinessCommandService.deleteMerchantBusinessPermanent(1L))
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                FindByIdMerchantBusinessRequest request = FindByIdMerchantBusinessRequest.newBuilder()
                                .setId(1)
                                .build();

                pb.merchant.MerchantCommon.ApiResponseMerchantDelete response = handler
                                .deleteMerchantBusinessPermanent(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant business info permanently deleted");
        }

        @Test
        void deleteMerchantBusinessPermanent_shouldReturnFailure_onServiceError() {
                lenient().when(merchantBusinessCommandService.deleteMerchantBusinessPermanent(999L))
                                .thenReturn(Uni.createFrom().failure(
                                                new RuntimeException(
                                                                "Merchant business info not found or must be trashed before permanent deletion")));

                FindByIdMerchantBusinessRequest request = FindByIdMerchantBusinessRequest.newBuilder()
                                .setId(999)
                                .build();

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.deleteMerchantBusinessPermanent(request).await().indefinitely());
        }

        @Test
        void restoreAllMerchantBusiness_shouldReturnSuccess() {
                ApiResponse<Void> serviceResponse = ApiResponse
                                .success("All trashed merchant business info restored");
                lenient().when(merchantBusinessCommandService.restoreAllMerchantBusiness())
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.merchant.MerchantCommon.ApiResponseMerchantAll response = handler
                                .restoreAllMerchantBusiness(com.google.protobuf.Empty.getDefaultInstance()).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All trashed merchant business info restored");
        }

        @Test
        void restoreAllMerchantBusiness_shouldReturnFailure_whenNoTrashedMerchantBusiness() {
                lenient().when(merchantBusinessCommandService.restoreAllMerchantBusiness())
                                .thenReturn(Uni.createFrom().failure(
                                                new RuntimeException("No trashed merchant business info found")));

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.restoreAllMerchantBusiness(com.google.protobuf.Empty.getDefaultInstance())
                                                .await()
                                                .indefinitely());
        }

        @Test
        void deleteAllMerchantBusinessPermanent_shouldReturnSuccess() {
                ApiResponse<Void> serviceResponse = ApiResponse
                                .success("All trashed merchant business info permanently deleted");
                lenient().when(merchantBusinessCommandService.deleteAllMerchantBusinessPermanent())
                                .thenReturn(Uni.createFrom().item(serviceResponse));

                pb.merchant.MerchantCommon.ApiResponseMerchantAll response = handler
                                .deleteAllMerchantBusinessPermanent(com.google.protobuf.Empty.getDefaultInstance())
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All trashed merchant business info permanently deleted");
        }

        @Test
        void deleteAllMerchantBusinessPermanent_shouldReturnFailure_whenNoTrashedMerchantBusiness() {
                lenient().when(merchantBusinessCommandService.deleteAllMerchantBusinessPermanent())
                                .thenReturn(Uni.createFrom().failure(
                                                new RuntimeException("No trashed merchant business info found")));

                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                                () -> handler.deleteAllMerchantBusinessPermanent(
                                                com.google.protobuf.Empty.getDefaultInstance()).await()
                                                .indefinitely());
        }
}
