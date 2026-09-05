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

import com.sanedge.gateway.dto.MerchantBusinessDto;
import com.sanedge.gateway.service.MerchantBusinessService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class MerchantBusinessResourceTest {
    @Mock
    MerchantBusinessService merchantBusinessService;
    private MerchantBusinessResource merchantBusinessResource;

    @BeforeEach
    void setUp() throws Exception {
        merchantBusinessResource = new MerchantBusinessResource();
        Field f = MerchantBusinessResource.class.getDeclaredField("merchantBusinessService");
        f.setAccessible(true);
        f.set(merchantBusinessResource, merchantBusinessService);
    }

    private MerchantBusinessDto.MerchantBusinessResponse mk(int id) {
        return new MerchantBusinessDto.MerchantBusinessResponse(id, 1, "LLC", "TX123", 2020, 10, "https://x", "M", "", "");
    }

    @Test
    void findAll_Success() {
        MerchantBusinessDto.ApiResponsePaginationBusiness dto = new MerchantBusinessDto.ApiResponsePaginationBusiness(
                List.of(mk(1)), "success", "ok");
        lenient().when(merchantBusinessService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        MerchantBusinessDto.ApiResponsePaginationBusiness dto = new MerchantBusinessDto.ApiResponsePaginationBusiness(
                List.of(), "success", "ok");
        lenient().when(merchantBusinessService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        MerchantBusinessDto.ApiResponsePaginationBusiness dto = new MerchantBusinessDto.ApiResponsePaginationBusiness(
                List.of(), "success", "ok");
        lenient().when(merchantBusinessService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        MerchantBusinessDto.ApiResponseBusiness dto = new MerchantBusinessDto.ApiResponseBusiness(mk(1), "success", "ok");
        lenient().when(merchantBusinessService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        MerchantBusinessDto.ApiResponseBusiness dto = new MerchantBusinessDto.ApiResponseBusiness(mk(1), "success", "ok");
        lenient().when(merchantBusinessService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest req = pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest.newBuilder().build();
        Response r = merchantBusinessResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void update_Success() {
        MerchantBusinessDto.ApiResponseBusiness dto = new MerchantBusinessDto.ApiResponseBusiness(mk(1), "success", "ok");
        lenient().when(merchantBusinessService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest req = pb.merchant_business.MerchantBusinessCommand.UpdateMerchantBusinessRequest.newBuilder().build();
        Response r = merchantBusinessResource.update(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_Success() {
        MerchantBusinessDto.ApiResponseBusiness dto = new MerchantBusinessDto.ApiResponseBusiness(mk(1), "success", "ok");
        lenient().when(merchantBusinessService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.delete(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restore_Success() {
        MerchantBusinessDto.ApiResponseBusiness dto = new MerchantBusinessDto.ApiResponseBusiness(mk(1), "success", "ok");
        lenient().when(merchantBusinessService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.restore(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deletePermanent_Success() {
        MerchantBusinessDto.SimpleResponse dto = new MerchantBusinessDto.SimpleResponse("success", "ok");
        lenient().when(merchantBusinessService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.deletePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAll_Success() {
        MerchantBusinessDto.SimpleResponse dto = new MerchantBusinessDto.SimpleResponse("success", "ok");
        lenient().when(merchantBusinessService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.restoreAll().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllPermanent_Success() {
        MerchantBusinessDto.SimpleResponse dto = new MerchantBusinessDto.SimpleResponse("success", "ok");
        lenient().when(merchantBusinessService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantBusinessResource.deleteAllPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
