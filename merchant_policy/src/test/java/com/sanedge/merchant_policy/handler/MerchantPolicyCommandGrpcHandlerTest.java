package com.sanedge.merchant_policy.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;
import com.sanedge.merchant_policy.service.MerchantPolicyCommandService;

import io.smallrye.mutiny.Uni;
import pb.merchant.MerchantCommon.ApiResponseMerchantAll;
import pb.merchant.MerchantCommon.ApiResponseMerchantDelete;
import pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPolicies;
import pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPoliciesDeleteAt;

@ExtendWith(MockitoExtension.class)
class MerchantPolicyCommandGrpcHandlerTest {

    @Mock
    private MerchantPolicyCommandService merchantPolicyCommandService;

    private MerchantPolicyCommandGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MerchantPolicyCommandGrpcHandler();

        try {
            var field = MerchantPolicyCommandGrpcHandler.class.getDeclaredField("merchantPolicyCommandService");
            field.setAccessible(true);
            field.set(handler, merchantPolicyCommandService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MerchantPoliciesResponse createTestResponse() {
        return MerchantPoliciesResponse.builder()
                .id(1L)
                .merchantId(1)
                .policyType("Refund")
                .title("Test Policy")
                .description("Test Description")
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();
    }

    private MerchantPoliciesResponseDeleteAt createTestDeleteAtResponse() {
        return MerchantPoliciesResponseDeleteAt.builder()
                .id(1L)
                .merchantId(1)
                .policyType("Refund")
                .title("Test Policy")
                .description("Test Description")
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .deletedAt(LocalDateTime.now().toString())
                .build();
    }

    @Test
    void create_Success() {

        var request = pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest.newBuilder()
                .setMerchantId(1)
                .setPolicyType("Refund")
                .setTitle("Test Policy")
                .setDescription("Test Description")
                .build();

        MerchantPoliciesResponse responseData = createTestResponse();
        ApiResponse<MerchantPoliciesResponse> serviceResponse = ApiResponse.success("Created", responseData);

        when(merchantPolicyCommandService.create(any()))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        ApiResponseMerchantPolicies response = handler.create(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Created");
        assertThat(response.getData().getId()).isEqualTo(1);

        verify(merchantPolicyCommandService).create(any());
    }

    @Test
    void create_ValidationError_ReturnsInternalError() {

        var request = pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest.newBuilder()
                .setMerchantId(1)
                .setPolicyType("Refund")
                .setTitle("Test Policy")
                .setDescription("Test Description")
                .build();

        when(merchantPolicyCommandService.create(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Validation failed")));

        try {
            handler.create(request).await().indefinitely();
        } catch (Exception e) {

            assertThat(e.getMessage()).contains("Validation failed");
        }
    }

    @Test
    void update_Success() {

        var request = pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest.newBuilder()
                .setMerchantPolicyId(1)
                .setPolicyType("Refund Updated")
                .setTitle("Updated Policy")
                .setDescription("Updated Description")
                .build();

        MerchantPoliciesResponse responseData = createTestResponse();
        responseData.setTitle("Updated Policy");
        ApiResponse<MerchantPoliciesResponse> serviceResponse = ApiResponse.success("Updated", responseData);

        when(merchantPolicyCommandService.update(any()))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        ApiResponseMerchantPolicies response = handler.update(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Updated");

        verify(merchantPolicyCommandService).update(any());
    }

    @Test
    void update_ResourceNotFound_ReturnsNotFoundStatus() {

        var request = pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest.newBuilder()
                .setMerchantPolicyId(999)
                .setPolicyType("Refund")
                .setTitle("Test")
                .setDescription("Test")
                .build();

        when(merchantPolicyCommandService.update(any()))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Policy not found")));

        try {
            handler.update(request).await().indefinitely();
        } catch (Exception e) {

            assertThat(e.getMessage()).contains("Policy not found");
        }
    }

    @Test
    void trashedMerchantPolicies_Success() {

        var request = pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                .setId(1)
                .build();

        MerchantPoliciesResponseDeleteAt responseData = createTestDeleteAtResponse();
        ApiResponse<MerchantPoliciesResponseDeleteAt> serviceResponse = ApiResponse.success("Trashed", responseData);

        when(merchantPolicyCommandService.trash(1L))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        ApiResponseMerchantPoliciesDeleteAt response = handler.trashedMerchantPolicies(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Trashed");
        assertThat(response.getData().hasDeletedAt()).isTrue();

        verify(merchantPolicyCommandService).trash(1L);
    }

    @Test
    void trashedMerchantPolicies_NotFound_ReturnsNotFoundStatus() {

        var request = pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                .setId(999)
                .build();

        when(merchantPolicyCommandService.trash(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException(
                                "Policy not found or already trashed")));

        try {
            handler.trashedMerchantPolicies(request).await().indefinitely();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Policy not found or already trashed");
        }
    }

    @Test
    void restoreMerchantPolicies_Success() {

        var request = pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                .setId(1)
                .build();

        MerchantPoliciesResponseDeleteAt responseData = createTestDeleteAtResponse();
        responseData.setDeletedAt(null);
        ApiResponse<MerchantPoliciesResponseDeleteAt> serviceResponse = ApiResponse.success("Restored", responseData);

        when(merchantPolicyCommandService.restore(1L))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        ApiResponseMerchantPoliciesDeleteAt response = handler.restoreMerchantPolicies(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Restored");

        verify(merchantPolicyCommandService).restore(1L);
    }

    @Test
    void restoreMerchantPolicies_NotFound_ReturnsNotFoundStatus() {

        var request = pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                .setId(999)
                .build();

        when(merchantPolicyCommandService.restore(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Policy not found or not trashed")));

        try {
            handler.restoreMerchantPolicies(request).await().indefinitely();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Policy not found or not trashed");
        }
    }

    @Test
    void deleteMerchantPoliciesPermanent_Success() {

        var request = pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                .setId(1)
                .build();

        ApiResponse<Void> serviceResponse = ApiResponse.success("Deleted permanently");

        when(merchantPolicyCommandService.delete(1L))
                .thenReturn(Uni.createFrom().item(serviceResponse));

        ApiResponseMerchantDelete response = handler.deleteMerchantPoliciesPermanent(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Deleted permanently");

        verify(merchantPolicyCommandService).delete(1L);
    }

    @Test
    void deleteMerchantPoliciesPermanent_NotFound_ReturnsNotFoundStatus() {

        var request = pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.newBuilder()
                .setId(999)
                .build();

        when(merchantPolicyCommandService.delete(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Policy not found")));

        try {
            handler.deleteMerchantPoliciesPermanent(request).await().indefinitely();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Policy not found");
        }
    }

    @Test
    void restoreAllMerchantPolicies_Success() {

        var request = com.google.protobuf.Empty.newBuilder().build();

        ApiResponse<Void> serviceResponse = ApiResponse.success("All restored");

        when(merchantPolicyCommandService.restoreAll())
                .thenReturn(Uni.createFrom().item(serviceResponse));

        ApiResponseMerchantAll response = handler.restoreAllMerchantPolicies(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All restored");

        verify(merchantPolicyCommandService).restoreAll();
    }

    @Test
    void deleteAllMerchantPoliciesPermanent_Success() {

        var request = com.google.protobuf.Empty.newBuilder().build();

        ApiResponse<Void> serviceResponse = ApiResponse.success("All deleted permanently");

        when(merchantPolicyCommandService.deleteAll())
                .thenReturn(Uni.createFrom().item(serviceResponse));

        ApiResponseMerchantAll response = handler.deleteAllMerchantPoliciesPermanent(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All deleted permanently");

        verify(merchantPolicyCommandService).deleteAll();
    }
}
