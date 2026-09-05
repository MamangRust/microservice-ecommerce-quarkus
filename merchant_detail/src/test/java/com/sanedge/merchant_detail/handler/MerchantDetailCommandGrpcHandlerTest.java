package com.sanedge.merchant_detail.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailResponseDeleteAt;
import com.sanedge.merchant_detail.service.MerchantDetailCommandService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest;
import pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest;
import pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail;
import pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt;
import pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest;

@ExtendWith(MockitoExtension.class)
class MerchantDetailCommandGrpcHandlerTest {

    @Mock
    private MerchantDetailCommandService merchantDetailCommandService;

    @InjectMocks
    private MerchantDetailCommandGrpcHandler handler;

    private MerchantDetailResponse merchantDetailResponse;
    private MerchantDetailResponseDeleteAt merchantDetailResponseDeleteAt;

    @BeforeEach
    void setUp() {
        merchantDetailResponse = MerchantDetailResponse.builder()
                .id(1L)
                .merchantId(1)
                .displayName("Test Merchant")
                .shortDescription("Test Description")
                .websiteUrl("https://example.com")
                .coverImageUrl("https://example.com/cover.jpg")
                .logoUrl("https://example.com/logo.png")
                .createdAt("2024-01-01T00:00:00")
                .updatedAt("2024-01-01T00:00:00")
                .build();

        merchantDetailResponseDeleteAt = MerchantDetailResponseDeleteAt.builder()
                .id(1L)
                .merchantId(1)
                .displayName("Test Merchant")
                .shortDescription("Test Description")
                .websiteUrl("https://example.com")
                .coverImageUrl("https://example.com/cover.jpg")
                .logoUrl("https://example.com/logo.png")
                .createdAt("2024-01-01T00:00:00")
                .updatedAt("2024-01-01T00:00:00")
                .deletedAt("2024-01-02T00:00:00")
                .build();
    }

    @Test
    void create_shouldReturnSuccess() {
        ApiResponse<MerchantDetailResponse> serviceResponse = ApiResponse.success(
                "Merchant detail created successfully!", merchantDetailResponse);
        when(merchantDetailCommandService.createMerchant(any()))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        CreateMerchantDetailRequest request = CreateMerchantDetailRequest.newBuilder()
                .setMerchantId(1)
                .setDisplayName("Test Merchant")
                .setShortDescription("Test Description")
                .setWebsiteUrl("https://example.com")
                .setCoverImageUrl("https://example.com/cover.jpg")
                .setLogoUrl("https://example.com/logo.png")
                .build();

        ApiResponseMerchantDetail response = handler.create(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Merchant detail created successfully!");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().getDisplayName()).isEqualTo("Test Merchant");
        assertThat(response.getData().getMerchantId()).isEqualTo(1);
    }

    @Test
    void create_shouldHandleNullData() {
        ApiResponse<MerchantDetailResponse> serviceResponse = ApiResponse.success(
                "Merchant detail created successfully!", null);
        when(merchantDetailCommandService.createMerchant(any()))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        CreateMerchantDetailRequest request = CreateMerchantDetailRequest.newBuilder()
                .setMerchantId(1)
                .setDisplayName("Test Merchant")
                .build();

        ApiResponseMerchantDetail response = handler.create(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.hasData()).isFalse();
    }

    @Test
    void update_shouldReturnSuccess() {
        ApiResponse<MerchantDetailResponse> serviceResponse = ApiResponse.success(
                "Merchant detail updated successfully!", merchantDetailResponse);
        when(merchantDetailCommandService.updateMerchant(any()))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        UpdateMerchantDetailRequest request = UpdateMerchantDetailRequest.newBuilder()
                .setMerchantDetailId(1)
                .setDisplayName("Updated Merchant")
                .setShortDescription("Updated Description")
                .setWebsiteUrl("https://updated.com")
                .setCoverImageUrl("https://updated.com/cover.jpg")
                .setLogoUrl("https://updated.com/logo.png")
                .build();

        ApiResponseMerchantDetail response = handler.update(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Merchant detail updated successfully!");
        assertThat(response.hasData()).isTrue();
    }

    @Test
    void update_InvalidMerchantDetailId_ReturnsInvalidArgument() {
        UpdateMerchantDetailRequest request = UpdateMerchantDetailRequest.newBuilder()
                .setMerchantDetailId(0)
                .setDisplayName("Test")
                .build();

        assertThatThrownBy(() -> handler.update(request).await().indefinitely())
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(e -> {
                    StatusRuntimeException sre = (StatusRuntimeException) e;
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
                    assertThat(sre.getStatus().getDescription()).contains("MerchantDetail id");
                });
    }

    @Test
    void update_shouldMapResourceNotFoundException_toNotFoundStatus() {
        when(merchantDetailCommandService.updateMerchant(any()))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Merchant detail not found")));

        UpdateMerchantDetailRequest request = UpdateMerchantDetailRequest.newBuilder()
                .setMerchantDetailId(999)
                .setDisplayName("Test")
                .build();

        handler.update(request)
                .subscribe().with(
                        resp -> assertThat(resp.getStatus()).isEqualTo("failed"),
                        err -> assertThat(err.getMessage()).contains("Merchant detail not found")
                );
    }

    @Test
    void trashedMerchantDetail_shouldReturnSuccess() {
        ApiResponse<MerchantDetailResponseDeleteAt> serviceResponse = ApiResponse.success(
                "Merchant detail trashed successfully!", merchantDetailResponseDeleteAt);
        when(merchantDetailCommandService.trashedMerchant(1L))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        FindByIdMerchantDetailRequest request = FindByIdMerchantDetailRequest.newBuilder()
                .setId(1)
                .build();

        ApiResponseMerchantDetailDeleteAt response = handler.trashedMerchantDetail(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Merchant detail trashed successfully!");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().getDeletedAt().getValue()).isNotNull();
    }

    @Test
    void trashedMerchantDetail_shouldReturnFailure_whenNotFound() {
        when(merchantDetailCommandService.trashedMerchant(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Merchant detail not found or already trashed")));

        FindByIdMerchantDetailRequest request = FindByIdMerchantDetailRequest.newBuilder()
                .setId(999)
                .build();

        handler.trashedMerchantDetail(request)
                .subscribe().with(
                        resp -> assertThat(resp.getStatus()).isEqualTo("failed"),
                        err -> assertThat(err.getMessage()).contains("not found or already trashed")
                );
    }

    @Test
    void restoreMerchantDetail_shouldReturnSuccess() {
        ApiResponse<MerchantDetailResponseDeleteAt> serviceResponse = ApiResponse.success(
                "Merchant detail restored successfully!", merchantDetailResponseDeleteAt);
        when(merchantDetailCommandService.restoreMerchant(1L))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        FindByIdMerchantDetailRequest request = FindByIdMerchantDetailRequest.newBuilder()
                .setId(1)
                .build();

        ApiResponseMerchantDetailDeleteAt response = handler.restoreMerchantDetail(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Merchant detail restored successfully!");
    }

    @Test
    void restoreMerchantDetail_shouldReturnFailure_whenNotFound() {
        when(merchantDetailCommandService.restoreMerchant(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Merchant detail not found or not trashed")));

        FindByIdMerchantDetailRequest request = FindByIdMerchantDetailRequest.newBuilder()
                .setId(999)
                .build();

        handler.restoreMerchantDetail(request)
                .subscribe().with(
                        resp -> assertThat(resp.getStatus()).isEqualTo("failed"),
                        err -> assertThat(err.getMessage()).contains("not found or not trashed")
                );
    }

    @Test
    void deleteMerchantDetailPermanent_shouldReturnSuccess() {
        ApiResponse<Void> serviceResponse = ApiResponse.success("Merchant detail permanently deleted");
        when(merchantDetailCommandService.deleteMerchantPermanent(1L))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        FindByIdMerchantDetailRequest request = FindByIdMerchantDetailRequest.newBuilder()
                .setId(1)
                .build();

        var response = handler.deleteMerchantDetailPermanent(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Merchant detail permanently deleted");
    }

    @Test
    void deleteMerchantDetailPermanent_shouldReturnFailure_whenNotFound() {
        when(merchantDetailCommandService.deleteMerchantPermanent(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.InvalidRequestException(
                                "Merchant detail not found or must be trashed before permanent deletion")));

        FindByIdMerchantDetailRequest request = FindByIdMerchantDetailRequest.newBuilder()
                .setId(999)
                .build();

        handler.deleteMerchantDetailPermanent(request)
                .subscribe().with(
                        resp -> assertThat(resp.getStatus()).isEqualTo("failed"),
                        err -> assertThat(err.getMessage()).contains("must be trashed")
                );
    }

    @Test
    void restoreAllMerchantDetail_shouldReturnSuccess() {
        ApiResponse<Void> serviceResponse = ApiResponse.success("All trashed merchant details restored");
        when(merchantDetailCommandService.restoreAllMerchant())
                .thenReturn(Uni.createFrom().item(serviceResponse));

        var response = handler.restoreAllMerchantDetail(com.google.protobuf.Empty.getDefaultInstance()).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All trashed merchant details restored");
    }

    @Test
    void restoreAllMerchantDetail_shouldReturnFailure_whenNoTrashedDetails() {
        when(merchantDetailCommandService.restoreAllMerchant())
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("No trashed merchant details found")));

        handler.restoreAllMerchantDetail(com.google.protobuf.Empty.getDefaultInstance())
                .subscribe().with(
                        resp -> assertThat(resp.getStatus()).isEqualTo("failed"),
                        err -> assertThat(err.getMessage()).contains("No trashed")
                );
    }

    @Test
    void deleteAllMerchantDetailPermanent_shouldReturnSuccess() {
        ApiResponse<Void> serviceResponse = ApiResponse.success("All trashed merchant details permanently deleted");
        when(merchantDetailCommandService.deleteAllMerchantPermanent())
                .thenReturn(Uni.createFrom().item(serviceResponse));

        var response = handler.deleteAllMerchantDetailPermanent(com.google.protobuf.Empty.getDefaultInstance()).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All trashed merchant details permanently deleted");
    }

    @Test
    void deleteAllMerchantDetailPermanent_shouldReturnFailure_whenNoTrashedDetails() {
        when(merchantDetailCommandService.deleteAllMerchantPermanent())
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("No trashed merchant details found")));

        handler.deleteAllMerchantDetailPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .subscribe().with(
                        resp -> assertThat(resp.getStatus()).isEqualTo("failed"),
                        err -> assertThat(err.getMessage()).contains("No trashed")
                );
    }
}
