package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.MerchantPolicyDto;
import com.sanedge.gateway.service.MerchantPolicyService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class MerchantPolicyResourceTest {
    @Mock
    MerchantPolicyService merchantPolicyService;
    private MerchantPolicyResource merchantPolicyResource;

    @BeforeEach
    void setUp() throws Exception {
        merchantPolicyResource = new MerchantPolicyResource();
        Field f = MerchantPolicyResource.class.getDeclaredField("merchantPolicyService");
        f.setAccessible(true);
        f.set(merchantPolicyResource, merchantPolicyService);
    }

    private MerchantPolicyDto.MerchantPoliciesResponse mk(int id) {
        return new MerchantPolicyDto.MerchantPoliciesResponse(id, 1, "REFUND", "Title", "desc", "", "", "M");
    }

    @Test
    void findAll_Success() {

        MerchantPolicyDto.ApiResponsePaginationPolicy dto = new MerchantPolicyDto.ApiResponsePaginationPolicy(
                "success", "ok", List.of(mk(1)));
        lenient().when(merchantPolicyService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        MerchantPolicyDto.ApiResponsePaginationPolicy dto = new MerchantPolicyDto.ApiResponsePaginationPolicy(
                "success", "ok", List.of());
        lenient().when(merchantPolicyService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        MerchantPolicyDto.ApiResponsePaginationPolicy dto = new MerchantPolicyDto.ApiResponsePaginationPolicy(
                "success", "ok", List.of());
        lenient().when(merchantPolicyService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {

        MerchantPolicyDto.ApiResponsePolicy dto = new MerchantPolicyDto.ApiResponsePolicy("success", "ok", mk(1));
        lenient().when(merchantPolicyService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        MerchantPolicyDto.ApiResponsePolicy dto = new MerchantPolicyDto.ApiResponsePolicy("success", "ok", mk(1));
        lenient().when(merchantPolicyService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest req = pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest.newBuilder().build();
        Response r = merchantPolicyResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void update_Success() {
        MerchantPolicyDto.ApiResponsePolicy dto = new MerchantPolicyDto.ApiResponsePolicy("success", "ok", mk(1));
        lenient().when(merchantPolicyService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest req = pb.merchant_policy.MerchantPolicyCommand.UpdateMerchantPoliciesRequest.newBuilder().build();
        Response r = merchantPolicyResource.update(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_Success() {
        MerchantPolicyDto.ApiResponsePolicy dto = new MerchantPolicyDto.ApiResponsePolicy("success", "ok", mk(1));
        lenient().when(merchantPolicyService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.delete(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restore_Success() {
        MerchantPolicyDto.ApiResponsePolicy dto = new MerchantPolicyDto.ApiResponsePolicy("success", "ok", mk(1));
        lenient().when(merchantPolicyService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.restore(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deletePermanent_Success() {
        MerchantPolicyDto.SimpleResponse dto = new MerchantPolicyDto.SimpleResponse("success", "ok");
        lenient().when(merchantPolicyService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.deletePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAll_Success() {
        MerchantPolicyDto.SimpleResponse dto = new MerchantPolicyDto.SimpleResponse("success", "ok");
        lenient().when(merchantPolicyService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.restoreAll().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllPermanent_Success() {
        MerchantPolicyDto.SimpleResponse dto = new MerchantPolicyDto.SimpleResponse("success", "ok");
        lenient().when(merchantPolicyService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantPolicyResource.deleteAllPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
