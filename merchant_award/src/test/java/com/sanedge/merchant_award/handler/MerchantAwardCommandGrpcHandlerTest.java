package com.sanedge.merchant_award.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_award.domain.requests.CreateMerchantAwardRequest;
import com.sanedge.merchant_award.domain.requests.UpdateMerchantAwardRequest;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponseDeleteAt;
import com.sanedge.merchant_award.service.MerchantAwardCommandService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.merchant.MerchantCommon;
import pb.merchant_award.MerchantAwardCommand;
import pb.merchant_award.MerchantAwardCommon;

@ExtendWith(MockitoExtension.class)
class MerchantAwardCommandGrpcHandlerTest {

        @Mock
        private MerchantAwardCommandService merchantAwardCommandService;

        private MerchantAwardCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new MerchantAwardCommandGrpcHandler();
                handler.merchantAwardCommandService = merchantAwardCommandService;
        }

        @Test
        @DisplayName("create - should return success response when merchant award created successfully")
        void create_Success() {
                MerchantAwardCommand.CreateMerchantAwardRequest request = MerchantAwardCommand.CreateMerchantAwardRequest
                                .newBuilder()
                                .setMerchantId(1)
                                .setTitle("ISO 9001 Certification")
                                .setDescription("Quality Management Certification")
                                .setIssuedBy("ISO Organization")
                                .setIssueDate("2024-01-01")
                                .setExpiryDate("2025-01-01")
                                .setCertificateUrl("https://example.com/cert.pdf")
                                .build();

                MerchantAwardResponse mockResponse = MerchantAwardResponse.builder()
                                .id(1L)
                                .merchantId(1)
                                .title("ISO 9001 Certification")
                                .description("Quality Management Certification")
                                .issuedBy("ISO Organization")
                                .issueDate("2024-01-01")
                                .expiryDate("2025-01-01")
                                .certificateUrl("https://example.com/cert.pdf")
                                .createdAt("2024-01-01")
                                .updatedAt("2024-01-01")
                                .build();

                ApiResponse<MerchantAwardResponse> apiResponse = ApiResponse.success(
                                "Merchant award created successfully",
                                mockResponse);

                when(merchantAwardCommandService.createMerchantAward(any(CreateMerchantAwardRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                MerchantAwardCommon.ApiResponseMerchantAward response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant award created successfully");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getMerchantId()).isEqualTo(1);
                assertThat(response.getData().getTitle()).isEqualTo("ISO 9001 Certification");
                assertThat(response.getData().getDescription()).isEqualTo("Quality Management Certification");
                assertThat(response.getData().getIssuedBy()).isEqualTo("ISO Organization");
        }

        @Test
        @DisplayName("create - should return NOT_FOUND when ResourceNotFoundException thrown")
        void create_ResourceNotFound() {
                MerchantAwardCommand.CreateMerchantAwardRequest request = MerchantAwardCommand.CreateMerchantAwardRequest
                                .newBuilder()
                                .setMerchantId(999)
                                .setTitle("ISO 9001 Certification")
                                .setDescription("Quality Management Certification")
                                .setIssuedBy("ISO Organization")
                                .setIssueDate("2024-01-01")
                                .build();

                when(merchantAwardCommandService.createMerchantAward(any(CreateMerchantAwardRequest.class)))
                                .thenReturn(Uni.createFrom().failure(
                                                new com.sanedge.common.exception.ResourceNotFoundException(
                                                                "Merchant not found with id 999")));

                StatusRuntimeException exception = null;
                try {
                        handler.create(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
                assertThat(exception.getStatus().getDescription()).isEqualTo("Merchant not found with id 999");
        }

        @Test
        @DisplayName("create - should return INTERNAL when generic exception thrown")
        void create_InternalError() {
                MerchantAwardCommand.CreateMerchantAwardRequest request = MerchantAwardCommand.CreateMerchantAwardRequest
                                .newBuilder()
                                .setMerchantId(1)
                                .setTitle("ISO 9001 Certification")
                                .setDescription("Quality Management Certification")
                                .setIssuedBy("ISO Organization")
                                .setIssueDate("2024-01-01")
                                .build();

                when(merchantAwardCommandService.createMerchantAward(any(CreateMerchantAwardRequest.class)))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

                StatusRuntimeException exception = null;
                try {
                        handler.create(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
        }

        @Test
        @DisplayName("update - should return success response when merchant award updated successfully")
        void update_Success() {
                MerchantAwardCommand.UpdateMerchantAwardRequest request = MerchantAwardCommand.UpdateMerchantAwardRequest
                                .newBuilder()
                                .setMerchantCertificationId(1)
                                .setTitle("Updated ISO 9001 Certification")
                                .setDescription("Updated description")
                                .setIssuedBy("Updated Issuer")
                                .setIssueDate("2024-01-01")
                                .setExpiryDate("2026-01-01")
                                .build();

                MerchantAwardResponse mockResponse = MerchantAwardResponse.builder()
                                .id(1L)
                                .merchantId(1)
                                .title("Updated ISO 9001 Certification")
                                .description("Updated description")
                                .issuedBy("Updated Issuer")
                                .issueDate("2024-01-01")
                                .expiryDate("2026-01-01")
                                .build();

                ApiResponse<MerchantAwardResponse> apiResponse = ApiResponse.success(
                                "Merchant award updated successfully",
                                mockResponse);

                when(merchantAwardCommandService.updateMerchantAward(any(UpdateMerchantAwardRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                MerchantAwardCommon.ApiResponseMerchantAward response = handler.update(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant award updated successfully");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getTitle()).isEqualTo("Updated ISO 9001 Certification");
        }

        @Test
        @DisplayName("update - should return NOT_FOUND when merchant award not found")
        void update_ResourceNotFound() {
                MerchantAwardCommand.UpdateMerchantAwardRequest request = MerchantAwardCommand.UpdateMerchantAwardRequest
                                .newBuilder()
                                .setMerchantCertificationId(999)
                                .setTitle("Updated Title")
                                .setDescription("Updated description")
                                .setIssuedBy("Updated Issuer")
                                .setIssueDate("2024-01-01")
                                .build();

                when(merchantAwardCommandService.updateMerchantAward(any(UpdateMerchantAwardRequest.class)))
                                .thenReturn(Uni.createFrom().failure(
                                                new com.sanedge.common.exception.ResourceNotFoundException(
                                                                "Merchant award not found with id 999")));

                StatusRuntimeException exception = null;
                try {
                        handler.update(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("trashedMerchantAward - should return success response when merchant award trashed successfully")
        void trashedMerchantAward_Success() {
                MerchantAwardCommon.FindByIdMerchantAwardRequest request = MerchantAwardCommon.FindByIdMerchantAwardRequest
                                .newBuilder()
                                .setId(1)
                                .build();

                MerchantAwardResponseDeleteAt mockResponse = MerchantAwardResponseDeleteAt.builder()
                                .id(1L)
                                .merchantId(1)
                                .title("ISO 9001 Certification")
                                .description("Quality Management Certification")
                                .issuedBy("ISO Organization")
                                .issueDate("2024-01-01")
                                .expiryDate("2025-01-01")
                                .deletedAt("2024-01-02T10:00:00")
                                .build();

                ApiResponse<MerchantAwardResponseDeleteAt> apiResponse = ApiResponse
                                .success("Merchant award trashed successfully", mockResponse);

                when(merchantAwardCommandService.trashedMerchantAward(1L))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt response = handler.trashedMerchantAward(request)
                                .await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant award trashed successfully");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getDeletedAt().getValue()).isEqualTo("2024-01-02T10:00:00");
        }

        @Test
        @DisplayName("trashedMerchantAward - should return NOT_FOUND when merchant award not found")
        void trashedMerchantAward_NotFound() {
                MerchantAwardCommon.FindByIdMerchantAwardRequest request = MerchantAwardCommon.FindByIdMerchantAwardRequest
                                .newBuilder()
                                .setId(999)
                                .build();

                when(merchantAwardCommandService.trashedMerchantAward(999L))
                                .thenReturn(Uni.createFrom().failure(
                                                new com.sanedge.common.exception.ResourceNotFoundException(
                                                                "Merchant award not found or already trashed")));

                StatusRuntimeException exception = null;
                try {
                        handler.trashedMerchantAward(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("restoreMerchantAward - should return success response when merchant award restored successfully")
        void restoreMerchantAward_Success() {
                MerchantAwardCommon.FindByIdMerchantAwardRequest request = MerchantAwardCommon.FindByIdMerchantAwardRequest
                                .newBuilder()
                                .setId(1)
                                .build();

                MerchantAwardResponseDeleteAt mockResponse = MerchantAwardResponseDeleteAt.builder()
                                .id(1L)
                                .merchantId(1)
                                .title("ISO 9001 Certification")
                                .description("Quality Management Certification")
                                .issuedBy("ISO Organization")
                                .build();

                ApiResponse<MerchantAwardResponseDeleteAt> apiResponse = ApiResponse
                                .success("Merchant award restored successfully", mockResponse);

                when(merchantAwardCommandService.restoreMerchantAward(1L))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt response = handler.restoreMerchantAward(request)
                                .await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant award restored successfully");
        }

        @Test
        @DisplayName("restoreMerchantAward - should return NOT_FOUND when merchant award not in trash")
        void restoreMerchantAward_NotFound() {
                MerchantAwardCommon.FindByIdMerchantAwardRequest request = MerchantAwardCommon.FindByIdMerchantAwardRequest
                                .newBuilder()
                                .setId(999)
                                .build();

                when(merchantAwardCommandService.restoreMerchantAward(999L))
                                .thenReturn(Uni.createFrom().failure(
                                                new com.sanedge.common.exception.ResourceNotFoundException(
                                                                "Merchant award not found or not trashed")));

                StatusRuntimeException exception = null;
                try {
                        handler.restoreMerchantAward(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("deleteMerchantAwardPermanent - should return success when merchant award deleted permanently")
        void deleteMerchantAwardPermanent_Success() {
                MerchantAwardCommon.FindByIdMerchantAwardRequest request = MerchantAwardCommon.FindByIdMerchantAwardRequest
                                .newBuilder()
                                .setId(1)
                                .build();

                ApiResponse<Void> apiResponse = ApiResponse.success("Merchant award permanently deleted");

                when(merchantAwardCommandService.deleteMerchantAwardPermanent(1L))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                MerchantCommon.ApiResponseMerchantDelete response = handler.deleteMerchantAwardPermanent(request)
                                .await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Merchant award permanently deleted");
        }

        @Test
        @DisplayName("deleteMerchantAwardPermanent - should return INVALID_ARGUMENT when merchant award not found or not trashed")
        void deleteMerchantAwardPermanent_NotFound() {
                MerchantAwardCommon.FindByIdMerchantAwardRequest request = MerchantAwardCommon.FindByIdMerchantAwardRequest
                                .newBuilder()
                                .setId(999)
                                .build();

                when(merchantAwardCommandService.deleteMerchantAwardPermanent(999L))
                                .thenReturn(Uni.createFrom().failure(
                                                new com.sanedge.common.exception.InvalidRequestException(
                                                                "Merchant award not found or must be trashed before permanent deletion")));

                StatusRuntimeException exception = null;
                try {
                        handler.deleteMerchantAwardPermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
        }

        @Test
        @DisplayName("restoreAllMerchantAward - should return success when all merchant awards restored")
        void restoreAllMerchantAward_Success() {
                Empty request = Empty.getDefaultInstance();

                ApiResponse<Void> apiResponse = ApiResponse.success("All trashed merchant awards restored");

                when(merchantAwardCommandService.restoreAllMerchantAward())
                                .thenReturn(Uni.createFrom().item(apiResponse));

                MerchantCommon.ApiResponseMerchantAll response = handler.restoreAllMerchantAward(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All trashed merchant awards restored");
        }    @Test
    @DisplayName("restoreAllMerchantAward - should return NOT_FOUND when no trashed merchant awards found")
    void restoreAllMerchantAward_NotFound() {
                Empty request = Empty.getDefaultInstance();

                when(merchantAwardCommandService.restoreAllMerchantAward())
                                .thenReturn(Uni.createFrom().failure(
                                                new com.sanedge.common.exception.ResourceNotFoundException(
                                                                "No trashed merchant awards found")));

                StatusRuntimeException exception = null;
                try {
                        handler.restoreAllMerchantAward(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("deleteAllMerchantAwardPermanent - should return success when all merchant awards deleted")
        void deleteAllMerchantAwardPermanent_Success() {
                Empty request = Empty.getDefaultInstance();

                ApiResponse<Void> apiResponse = ApiResponse.success("All trashed merchant awards permanently deleted");

                when(merchantAwardCommandService.deleteAllMerchantAwardPermanent())
                                .thenReturn(Uni.createFrom().item(apiResponse));

                MerchantCommon.ApiResponseMerchantAll response = handler.deleteAllMerchantAwardPermanent(request)
                                .await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All trashed merchant awards permanently deleted");
        }    @Test
    @DisplayName("deleteAllMerchantAwardPermanent - should return NOT_FOUND when no trashed merchant awards found")
    void deleteAllMerchantAwardPermanent_NotFound() {
                Empty request = Empty.getDefaultInstance();

                when(merchantAwardCommandService.deleteAllMerchantAwardPermanent())
                                .thenReturn(Uni.createFrom().failure(
                                                new com.sanedge.common.exception.ResourceNotFoundException(
                                                                "No trashed merchant awards found")));

                StatusRuntimeException exception = null;
                try {
                        handler.deleteAllMerchantAwardPermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("create - should correctly map request fields to domain object")
        void create_RequestMapping() {
                MerchantAwardCommand.CreateMerchantAwardRequest request = MerchantAwardCommand.CreateMerchantAwardRequest
                                .newBuilder()
                                .setMerchantId(5)
                                .setTitle("New Certification")
                                .setDescription("New Description")
                                .setIssuedBy("New Issuer")
                                .setIssueDate("2024-06-01")
                                .setExpiryDate("2025-06-01")
                                .setCertificateUrl("https://example.com/new-cert.pdf")
                                .build();

                MerchantAwardResponse mockResponse = MerchantAwardResponse.builder()
                                .id(1L)
                                .merchantId(5)
                                .title("New Certification")
                                .build();

                ApiResponse<MerchantAwardResponse> apiResponse = ApiResponse.success("Merchant award created",
                                mockResponse);

                when(merchantAwardCommandService.createMerchantAward(any(CreateMerchantAwardRequest.class)))
                                .thenAnswer(invocation -> {
                                        CreateMerchantAwardRequest domainReq = invocation.getArgument(0);

                                        assertThat(domainReq.getMerchantId()).isEqualTo(5);
                                        assertThat(domainReq.getTitle()).isEqualTo("New Certification");
                                        assertThat(domainReq.getDescription()).isEqualTo("New Description");
                                        assertThat(domainReq.getIssuedBy()).isEqualTo("New Issuer");
                                        assertThat(domainReq.getIssueDate()).isEqualTo("2024-06-01");
                                        assertThat(domainReq.getExpiryDate()).isEqualTo("2025-06-01");
                                        assertThat(domainReq.getCertificateUrl())
                                                        .isEqualTo("https://example.com/new-cert.pdf");
                                        return Uni.createFrom().item(apiResponse);
                                });

                MerchantAwardCommon.ApiResponseMerchantAward response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }
}
