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

import com.sanedge.gateway.dto.ProductDto;
import com.sanedge.gateway.service.ProductService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class ProductResourceTest {
    @Mock
    ProductService productService;
    private ProductResource productResource;

    @BeforeEach
    void setUp() throws Exception {
        productResource = new ProductResource();
        Field f = ProductResource.class.getDeclaredField("productService");
        f.setAccessible(true);
        f.set(productResource, productService);
    }

    private ProductDto.ProductResponse mk(int id) {
        return new ProductDto.ProductResponse(id, 1, 1, "name", "desc", 100, 5, "brand", 1, 0.0f, "slug", "", "", "");
    }

    @Test
    void findAll_Success() {
        ProductDto.ApiResponsePaginationProduct dto = new ProductDto.ApiResponsePaginationProduct(
                List.of(mk(1)), "success", "ok");
        lenient().when(productService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        ProductDto.ApiResponsePaginationProduct dto = new ProductDto.ApiResponsePaginationProduct(
                List.of(), "success", "ok");
        lenient().when(productService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        ProductDto.ApiResponsePaginationProduct dto = new ProductDto.ApiResponsePaginationProduct(
                List.of(), "success", "ok");
        lenient().when(productService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        ProductDto.ApiResponseProduct dto = new ProductDto.ApiResponseProduct(mk(1), "success", "ok");
        lenient().when(productService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createProduct_Success_Returns201() {
        ProductDto.ApiResponseProduct dto = new ProductDto.ApiResponseProduct(mk(1), "success", "ok");
        lenient().when(productService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.product.ProductCommand.CreateProductRequest req = pb.product.ProductCommand.CreateProductRequest.newBuilder().build();
        Response r = productResource.createProduct(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateProduct_Success() {
        ProductDto.ApiResponseProduct dto = new ProductDto.ApiResponseProduct(mk(1), "success", "ok");
        lenient().when(productService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.product.ProductCommand.UpdateProductRequest req = pb.product.ProductCommand.UpdateProductRequest.newBuilder().build();
        Response r = productResource.updateProduct(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteProduct_Success() {
        ProductDto.ApiResponseProduct dto = new ProductDto.ApiResponseProduct(mk(1), "success", "ok");
        lenient().when(productService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.deleteProduct(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreProduct_Success() {
        ProductDto.ApiResponseProduct dto = new ProductDto.ApiResponseProduct(mk(1), "success", "ok");
        lenient().when(productService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.restoreProduct(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteProductPermanent_Success() {
        ProductDto.SimpleResponse dto = new ProductDto.SimpleResponse("success", "ok");
        lenient().when(productService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.deleteProductPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllProducts_Success() {
        ProductDto.SimpleResponse dto = new ProductDto.SimpleResponse("success", "ok");
        lenient().when(productService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.restoreAllProducts().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllProductsPermanent_Success() {
        ProductDto.SimpleResponse dto = new ProductDto.SimpleResponse("success", "ok");
        lenient().when(productService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = productResource.deleteAllProductsPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
