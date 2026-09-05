package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.CartDto;
import com.sanedge.gateway.dto.PaginationMeta;
import com.sanedge.gateway.service.CartService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class CartResourceTest {
    @Mock
    CartService cartService;
    private CartResource cartResource;

    @BeforeEach
    void setUp() throws Exception {
        cartResource = new CartResource();
        Field f = CartResource.class.getDeclaredField("cartService");
        f.setAccessible(true);
        f.set(cartResource, cartService);
    }

    private CartDto.CartItemResponse mk(int id) {
        return new CartDto.CartItemResponse(id, 1, 1, 9.99, "", "");
    }

    private CartDto.ApiResponsePaginationCart mkPaginated() {
        return new CartDto.ApiResponsePaginationCart("success", "ok", List.of(mk(1)), null);
    }

    private CartDto.ApiResponseCart mkSingle() {
        return new CartDto.ApiResponseCart("success", "ok", mk(1));
    }

    @Test
    void findAll_Success() {
        CartDto.ApiResponsePaginationCart dto = mkPaginated();

        lenient().when(cartService.findAll(anyInt(), anyInt(), anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = cartResource.findAll(1, 1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        CartDto.ApiResponseCart dto = mkSingle();
        lenient().when(cartService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.cart.CartCommand.CreateCartRequest req = pb.cart.CartCommand.CreateCartRequest.newBuilder().build();
        Response r = cartResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void delete_Success() {
        CartDto.SimpleResponse dto = new CartDto.SimpleResponse("success", "ok");
        lenient().when(cartService.delete(anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = cartResource.delete(1, 1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAll_Success() {
        CartDto.SimpleResponse dto = new CartDto.SimpleResponse("success", "ok");
        lenient().when(cartService.deleteAll(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        Response r = cartResource.deleteAll(1, List.of(1, 2, 3)).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
