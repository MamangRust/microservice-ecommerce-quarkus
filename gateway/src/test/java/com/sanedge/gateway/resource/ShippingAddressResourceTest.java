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

import com.sanedge.gateway.dto.ShippingAddressDto;
import com.sanedge.gateway.service.ShippingAddressService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class ShippingAddressResourceTest {
    @Mock
    ShippingAddressService shippingAddressService;
    private ShippingAddressResource shippingAddressResource;

    @BeforeEach
    void setUp() throws Exception {
        shippingAddressResource = new ShippingAddressResource();
        Field f = ShippingAddressResource.class.getDeclaredField("shippingAddressService");
        f.setAccessible(true);
        f.set(shippingAddressResource, shippingAddressService);
    }

    private ShippingAddressDto.ShippingResponse mk(int id) {
        return new ShippingAddressDto.ShippingResponse(id, 1, "addr", "prov", "country", "city", "JNE", 10000, "", "");
    }

    @Test
    void findAll_Success() {
        ShippingAddressDto.ApiResponsePaginationAddress dto = new ShippingAddressDto.ApiResponsePaginationAddress(
                List.of(mk(1)), "success", "ok");
        lenient().when(shippingAddressService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByOrder_Success() {
        ShippingAddressDto.ApiResponseAddress dto = new ShippingAddressDto.ApiResponseAddress(mk(1), "success", "ok");
        lenient().when(shippingAddressService.findByOrder(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.findByOrder(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        ShippingAddressDto.ApiResponsePaginationAddress dto = new ShippingAddressDto.ApiResponsePaginationAddress(
                List.of(), "success", "ok");
        lenient().when(shippingAddressService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        ShippingAddressDto.ApiResponsePaginationAddress dto = new ShippingAddressDto.ApiResponsePaginationAddress(
                List.of(), "success", "ok");
        lenient().when(shippingAddressService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        ShippingAddressDto.ApiResponseAddress dto = new ShippingAddressDto.ApiResponseAddress(mk(1), "success", "ok");
        lenient().when(shippingAddressService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        ShippingAddressDto.ApiResponseAddress dto = new ShippingAddressDto.ApiResponseAddress(mk(1), "success", "ok");
        lenient().when(shippingAddressService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest req = pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest.newBuilder().build();
        Response r = shippingAddressResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void update_Success() {
        ShippingAddressDto.ApiResponseAddress dto = new ShippingAddressDto.ApiResponseAddress(mk(1), "success", "ok");
        lenient().when(shippingAddressService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest req = pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest.newBuilder().build();
        Response r = shippingAddressResource.update(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_Success() {
        ShippingAddressDto.ApiResponseAddress dto = new ShippingAddressDto.ApiResponseAddress(mk(1), "success", "ok");
        lenient().when(shippingAddressService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.delete(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restore_Success() {
        ShippingAddressDto.ApiResponseAddress dto = new ShippingAddressDto.ApiResponseAddress(mk(1), "success", "ok");
        lenient().when(shippingAddressService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.restore(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deletePermanent_Success() {
        ShippingAddressDto.SimpleResponse dto = new ShippingAddressDto.SimpleResponse("success", "ok");
        lenient().when(shippingAddressService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.deletePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAll_Success() {
        ShippingAddressDto.SimpleResponse dto = new ShippingAddressDto.SimpleResponse("success", "ok");
        lenient().when(shippingAddressService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.restoreAll().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllPermanent_Success() {
        ShippingAddressDto.SimpleResponse dto = new ShippingAddressDto.SimpleResponse("success", "ok");
        lenient().when(shippingAddressService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = shippingAddressResource.deleteAllPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
