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

import com.sanedge.gateway.dto.MerchantDetailDto;
import com.sanedge.gateway.service.MerchantDetailService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class MerchantDetailResourceTest {
    @Mock
    MerchantDetailService merchantDetailService;
    private MerchantDetailResource merchantDetailResource;

    @BeforeEach
    void setUp() throws Exception {
        merchantDetailResource = new MerchantDetailResource();
        Field f = MerchantDetailResource.class.getDeclaredField("merchantDetailService");
        f.setAccessible(true);
        f.set(merchantDetailResource, merchantDetailService);
    }

    private MerchantDetailDto.MerchantDetailResponse mk(int id) {
        return new MerchantDetailDto.MerchantDetailResponse(
                id, 1, "Display", "", "", "desc", "https://x", List.of(), "", "");
    }

    @Test
    void findAll_Success() {
        MerchantDetailDto.ApiResponsePaginationDetail dto = new MerchantDetailDto.ApiResponsePaginationDetail(
                List.of(mk(1)), "success", "ok");
        lenient().when(merchantDetailService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        MerchantDetailDto.ApiResponsePaginationDetail dto = new MerchantDetailDto.ApiResponsePaginationDetail(
                List.of(), "success", "ok");
        lenient().when(merchantDetailService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        MerchantDetailDto.ApiResponsePaginationDetail dto = new MerchantDetailDto.ApiResponsePaginationDetail(
                List.of(), "success", "ok");
        lenient().when(merchantDetailService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        MerchantDetailDto.ApiResponseDetail dto = new MerchantDetailDto.ApiResponseDetail(mk(1), "success", "ok");
        lenient().when(merchantDetailService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        MerchantDetailDto.ApiResponseDetail dto = new MerchantDetailDto.ApiResponseDetail(mk(1), "success", "ok");
        lenient().when(merchantDetailService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest req = pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest.newBuilder().build();
        Response r = merchantDetailResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void update_Success() {
        MerchantDetailDto.ApiResponseDetail dto = new MerchantDetailDto.ApiResponseDetail(mk(1), "success", "ok");
        lenient().when(merchantDetailService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest req = pb.merchant_detail.MerchantDetailCommand.UpdateMerchantDetailRequest.newBuilder().build();
        Response r = merchantDetailResource.update(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_Success() {
        MerchantDetailDto.ApiResponseDetail dto = new MerchantDetailDto.ApiResponseDetail(mk(1), "success", "ok");
        lenient().when(merchantDetailService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.delete(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restore_Success() {
        MerchantDetailDto.ApiResponseDetail dto = new MerchantDetailDto.ApiResponseDetail(mk(1), "success", "ok");
        lenient().when(merchantDetailService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.restore(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deletePermanent_Success() {
        MerchantDetailDto.SimpleResponse dto = new MerchantDetailDto.SimpleResponse("success", "ok");
        lenient().when(merchantDetailService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.deletePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAll_Success() {
        MerchantDetailDto.SimpleResponse dto = new MerchantDetailDto.SimpleResponse("success", "ok");
        lenient().when(merchantDetailService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.restoreAll().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllPermanent_Success() {
        MerchantDetailDto.SimpleResponse dto = new MerchantDetailDto.SimpleResponse("success", "ok");
        lenient().when(merchantDetailService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDetailResource.deleteAllPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
