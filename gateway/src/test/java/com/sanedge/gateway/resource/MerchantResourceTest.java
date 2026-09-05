package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.sanedge.gateway.dto.MerchantDto;
import com.sanedge.gateway.service.MerchantService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class MerchantResourceTest {

    @Mock
    private MerchantService merchantService;

    private MerchantResource merchantResource;

    @BeforeEach
    void setUp() throws Exception {
        merchantResource = new MerchantResource();
        Field f = MerchantResource.class.getDeclaredField("merchantService");
        f.setAccessible(true);
        f.set(merchantResource, merchantService);
    }

    private MerchantDto.MerchantResponse mkMerchant(int id) {
        return new MerchantDto.MerchantResponse(id, 1, "name", "desc", "addr", "e@x", "p", "active", "", "");
    }

    @Test
    void listMerchants_Success() {
        MerchantDto.ApiResponsePaginationMerchant dto = new MerchantDto.ApiResponsePaginationMerchant(
                List.of(mkMerchant(1)), "success", "ok");
        lenient().when(merchantService.listMerchants(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.listMerchants(1, 10, "x").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getMerchant_Success() {
        MerchantDto.ApiResponseMerchant dto = new MerchantDto.ApiResponseMerchant(mkMerchant(1), "success", "ok");
        lenient().when(merchantService.getMerchant(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.getMerchant(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getActiveMerchants_Success() {
        MerchantDto.ApiResponsePaginationMerchantDeleteAt dto = new MerchantDto.ApiResponsePaginationMerchantDeleteAt(
                List.of(mkMerchant(1)), "success", "ok");
        lenient().when(merchantService.getActiveMerchants(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.getActiveMerchants(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getTrashedMerchants_Success() {
        MerchantDto.ApiResponsePaginationMerchantDeleteAt dto = new MerchantDto.ApiResponsePaginationMerchantDeleteAt(
                List.of(), "success", "ok");
        lenient().when(merchantService.getTrashedMerchants(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.getTrashedMerchants(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createMerchant_Success_Returns201() {
        MerchantDto.ApiResponseMerchant dto = new MerchantDto.ApiResponseMerchant(mkMerchant(1), "success", "ok");
        lenient().when(merchantService.createMerchant(any()))
                .thenReturn(Uni.createFrom().item(dto));
        pb.merchant.MerchantCommand.CreateMerchantRequest req = pb.merchant.MerchantCommand.CreateMerchantRequest
                .newBuilder().setName("x").build();
        Response r = merchantResource.createMerchant(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateMerchant_Success() {
        MerchantDto.ApiResponseMerchant dto = new MerchantDto.ApiResponseMerchant(mkMerchant(1), "success", "ok");
        lenient().when(merchantService.updateMerchant(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant.MerchantCommand.UpdateMerchantRequest req = pb.merchant.MerchantCommand.UpdateMerchantRequest
                .newBuilder().setName("x").build();
        Response r = merchantResource.updateMerchant(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void updateMerchantStatus_Success() {
        MerchantDto.ApiResponseMerchant dto = new MerchantDto.ApiResponseMerchant(mkMerchant(1), "success", "ok");
        lenient().when(merchantService.updateMerchantStatus(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant.MerchantCommand.UpdateMerchantStatusRequest req = pb.merchant.MerchantCommand.UpdateMerchantStatusRequest
                .newBuilder().setStatus("active").build();
        Response r = merchantResource.updateMerchantStatus(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteMerchant_Success() {
        MerchantDto.ApiResponseMerchantDeleteAt dto = new MerchantDto.ApiResponseMerchantDeleteAt(
                mkMerchant(1), "success", "ok");
        lenient().when(merchantService.deleteMerchant(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.deleteMerchant(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreMerchant_Success() {
        MerchantDto.ApiResponseMerchant dto = new MerchantDto.ApiResponseMerchant(mkMerchant(1), "success", "ok");
        lenient().when(merchantService.restoreMerchant(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.restoreMerchant(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteMerchantPermanent_Success() {
        MerchantDto.SimpleResponse dto = new MerchantDto.SimpleResponse("success", "ok");
        lenient().when(merchantService.deleteMerchantPermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.deleteMerchantPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllMerchant_Success() {
        MerchantDto.SimpleResponse dto = new MerchantDto.SimpleResponse("success", "ok");
        lenient().when(merchantService.restoreAllMerchant()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.restoreAllMerchant().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllMerchantPermanent_Success() {
        MerchantDto.SimpleResponse dto = new MerchantDto.SimpleResponse("success", "ok");
        lenient().when(merchantService.deleteAllMerchantPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantResource.deleteAllMerchantPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createMerchant_Failure_PropagatesError() {
        lenient().when(merchantService.createMerchant(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("boom")));
        pb.merchant.MerchantCommand.CreateMerchantRequest req = pb.merchant.MerchantCommand.CreateMerchantRequest
                .newBuilder().build();
        assertThatThrownBy(() -> merchantResource.createMerchant(req).await().indefinitely())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");
    }
}
