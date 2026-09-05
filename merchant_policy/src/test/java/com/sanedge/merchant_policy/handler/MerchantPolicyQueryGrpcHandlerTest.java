package com.sanedge.merchant_policy.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;
import com.sanedge.merchant_policy.service.MerchantPolicyQueryService;

import io.smallrye.mutiny.Uni;
import pb.merchant.MerchantQuery.FindAllMerchantRequest;
import pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest;

@ExtendWith(MockitoExtension.class)
class MerchantPolicyQueryGrpcHandlerTest {
    @Mock
    MerchantPolicyQueryService merchantPolicyQueryService;
    private MerchantPolicyQueryGrpcHandler merchantPolicyQueryGrpcHandler;

    @BeforeEach
    void setUp() throws Exception {
        merchantPolicyQueryGrpcHandler = new MerchantPolicyQueryGrpcHandler();
        Field f = MerchantPolicyQueryGrpcHandler.class.getDeclaredField("merchantPolicyQueryService");
        f.setAccessible(true);
        f.set(merchantPolicyQueryGrpcHandler, merchantPolicyQueryService);
    }

    @Test
    void findAll_Success() {
        ApiResponsePagination<List<MerchantPoliciesResponse>> resp = new ApiResponsePagination<>(
                "success", "ok", List.of(), null);
        lenient().when(merchantPolicyQueryService.findAll(any(com.sanedge.merchant_policy.domain.requests.FindAllMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(resp));

        var request = FindAllMerchantRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();
        var result = merchantPolicyQueryGrpcHandler.findAll(request).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getMessage()).isEqualTo("ok");
    }

    @Test
    void findById_Success() {
        lenient().when(merchantPolicyQueryService.findById(any(Long.class)))
                .thenAnswer(i -> Uni.createFrom().item(
                        ApiResponse.<MerchantPoliciesResponse>success("ok", null)));

        var request = FindByIdMerchantPoliciesRequest.newBuilder()
                .setId(1)
                .build();
        var result = merchantPolicyQueryGrpcHandler.findById(request).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getMessage()).isEqualTo("ok");
    }

    @Test
    void findByActive_Success() {
        ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>> resp = new ApiResponsePagination<>(
                "success", "active", List.of(), null);
        lenient().when(merchantPolicyQueryService.findByActive(any(com.sanedge.merchant_policy.domain.requests.FindAllMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(resp));

        var request = FindAllMerchantRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();
        var result = merchantPolicyQueryGrpcHandler.findByActive(request).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getMessage()).isEqualTo("active");
    }

    @Test
    void findByTrashed_Success() {
        ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>> resp = new ApiResponsePagination<>(
                "success", "trashed", List.of(), null);
        lenient().when(merchantPolicyQueryService.findByTrashed(any(com.sanedge.merchant_policy.domain.requests.FindAllMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(resp));

        var request = FindAllMerchantRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();
        var result = merchantPolicyQueryGrpcHandler.findByTrashed(request).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getMessage()).isEqualTo("trashed");
    }
}
