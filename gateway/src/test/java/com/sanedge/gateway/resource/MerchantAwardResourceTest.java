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

import com.sanedge.gateway.dto.MerchantAwardDto;
import com.sanedge.gateway.service.MerchantAwardService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class MerchantAwardResourceTest {
    @Mock
    MerchantAwardService merchantAwardService;
    private MerchantAwardResource merchantAwardResource;

    @BeforeEach
    void setUp() throws Exception {
        merchantAwardResource = new MerchantAwardResource();
        Field f = MerchantAwardResource.class.getDeclaredField("merchantAwardService");
        f.setAccessible(true);
        f.set(merchantAwardResource, merchantAwardService);
    }

    private MerchantAwardDto.MerchantAwardResponse mk(int id) {
        return new MerchantAwardDto.MerchantAwardResponse(id, 1, "Award", "desc", "Org", "", "", "", "", "", "M");
    }

    @Test
    void findAll_Success() {
        MerchantAwardDto.ApiResponsePaginationAward dto = new MerchantAwardDto.ApiResponsePaginationAward(
                List.of(mk(1)), "success", "ok");
        lenient().when(merchantAwardService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        MerchantAwardDto.ApiResponsePaginationAward dto = new MerchantAwardDto.ApiResponsePaginationAward(
                List.of(), "success", "ok");
        lenient().when(merchantAwardService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        MerchantAwardDto.ApiResponsePaginationAward dto = new MerchantAwardDto.ApiResponsePaginationAward(
                List.of(), "success", "ok");
        lenient().when(merchantAwardService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        MerchantAwardDto.ApiResponseAward dto = new MerchantAwardDto.ApiResponseAward(mk(1), "success", "ok");
        lenient().when(merchantAwardService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        MerchantAwardDto.ApiResponseAward dto = new MerchantAwardDto.ApiResponseAward(mk(1), "success", "ok");
        lenient().when(merchantAwardService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant_award.MerchantAwardCommand.CreateMerchantAwardRequest req = pb.merchant_award.MerchantAwardCommand.CreateMerchantAwardRequest.newBuilder().build();
        Response r = merchantAwardResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void update_Success() {
        MerchantAwardDto.ApiResponseAward dto = new MerchantAwardDto.ApiResponseAward(mk(1), "success", "ok");
        lenient().when(merchantAwardService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant_award.MerchantAwardCommand.UpdateMerchantAwardRequest req = pb.merchant_award.MerchantAwardCommand.UpdateMerchantAwardRequest.newBuilder().build();
        Response r = merchantAwardResource.update(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_Success() {
        MerchantAwardDto.ApiResponseAward dto = new MerchantAwardDto.ApiResponseAward(mk(1), "success", "ok");
        lenient().when(merchantAwardService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.delete(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restore_Success() {
        MerchantAwardDto.ApiResponseAward dto = new MerchantAwardDto.ApiResponseAward(mk(1), "success", "ok");
        lenient().when(merchantAwardService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.restore(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deletePermanent_Success() {
        MerchantAwardDto.SimpleResponse dto = new MerchantAwardDto.SimpleResponse("success", "ok");
        lenient().when(merchantAwardService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.deletePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAll_Success() {
        MerchantAwardDto.SimpleResponse dto = new MerchantAwardDto.SimpleResponse("success", "ok");
        lenient().when(merchantAwardService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.restoreAll().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllPermanent_Success() {
        MerchantAwardDto.SimpleResponse dto = new MerchantAwardDto.SimpleResponse("success", "ok");
        lenient().when(merchantAwardService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantAwardResource.deleteAllPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
