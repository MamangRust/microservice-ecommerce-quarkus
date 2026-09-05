package com.sanedge.product.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;
import com.sanedge.product.service.ProductCommandService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.product.ProductCommand;
import pb.product.ProductCommon;

@ExtendWith(MockitoExtension.class)
class ProductCommandGrpcHandlerTest {

    @Mock
    private ProductCommandService productCommandService;

    @Mock
    private ProductResponse productResponse;

    @Mock
    private ProductResponseDeleteAt productResponseDeleteAt;

    @Mock
    private ApiResponse<ProductResponse> apiResponseSuccess;

    @Mock
    private ApiResponse<ProductResponseDeleteAt> apiResponseDeleteAtSuccess;

    @Mock
    private ApiResponse<Void> apiResponseEmptySuccess;

    private ProductCommandGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductCommandGrpcHandler();
        handler.productCommandService = productCommandService;
    }

    @Test
    @DisplayName("create - should return ApiResponseProduct on success")
    void create_Success() {
        setupProductResponse();
        when(apiResponseSuccess.status()).thenReturn("success");
        when(apiResponseSuccess.message()).thenReturn("Product created successfully");
        when(apiResponseSuccess.data()).thenReturn(productResponse);

        ProductCommand.CreateProductRequest request = ProductCommand.CreateProductRequest.newBuilder()
                .setMerchantId(1).setCategoryId(1).setName("Laptop").setPrice(15000000).build();

        when(productCommandService.createProduct(any())).thenReturn(Uni.createFrom().item(apiResponseSuccess));

        ProductCommon.ApiResponseProduct response = handler.create(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("create - should return ALREADY_EXISTS on conflict")
    void create_AlreadyExists() {
        ProductCommand.CreateProductRequest request = ProductCommand.CreateProductRequest.newBuilder()
                .setMerchantId(1).setCategoryId(1).setName("Dup Laptop").build();

        when(productCommandService.createProduct(any()))
                .thenReturn(Uni.createFrom().failure(new ResourceAlreadyExistsException("Product already exists")));

        StatusRuntimeException ex = null;
        try {
            handler.create(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.ALREADY_EXISTS.getCode());
    }

    @Test
    @DisplayName("create - should return INTERNAL on failure")
    void create_InternalError() {
        ProductCommand.CreateProductRequest request = ProductCommand.CreateProductRequest.newBuilder()
                .setMerchantId(1).setCategoryId(1).setName("Laptop").build();

        when(productCommandService.createProduct(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        StatusRuntimeException ex = null;
        try {
            handler.create(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    @DisplayName("update - should return ApiResponseProduct on success")
    void update_Success() {
        setupProductResponse();
        when(apiResponseSuccess.status()).thenReturn("success");
        when(apiResponseSuccess.message()).thenReturn("Product updated successfully");
        when(apiResponseSuccess.data()).thenReturn(productResponse);

        ProductCommand.UpdateProductRequest request = ProductCommand.UpdateProductRequest.newBuilder()
                .setProductId(1).setMerchantId(1).setCategoryId(1).setName("Laptop Updated").build();

        when(productCommandService.updateProduct(any())).thenReturn(Uni.createFrom().item(apiResponseSuccess));

        ProductCommon.ApiResponseProduct response = handler.update(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("update - should return NOT_FOUND when not found")
    void update_NotFound() {
        ProductCommand.UpdateProductRequest request = ProductCommand.UpdateProductRequest.newBuilder()
                .setProductId(999).setMerchantId(1).setCategoryId(1).setName("Missing").build();

        when(productCommandService.updateProduct(any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Product not found")));

        StatusRuntimeException ex = null;
        try {
            handler.update(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("updateProductCountStock - should return ApiResponseProduct on success")
    void updateProductCountStock_Success() {
        setupProductResponse();
        when(apiResponseSuccess.status()).thenReturn("success");
        when(apiResponseSuccess.message()).thenReturn("Stock updated successfully");
        when(apiResponseSuccess.data()).thenReturn(productResponse);

        ProductCommand.UpdateProductCountStockRequest request = ProductCommand.UpdateProductCountStockRequest
                .newBuilder()
                .setProductId(1).setStock(50).build();

        when(productCommandService.updateProductCountStock(anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(apiResponseSuccess));

        ProductCommon.ApiResponseProduct response = handler.updateProductCountStock(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("updateProductCountStock - should return NOT_FOUND when not found")
    void updateProductCountStock_NotFound() {
        ProductCommand.UpdateProductCountStockRequest request = ProductCommand.UpdateProductCountStockRequest
                .newBuilder().setProductId(999).setStock(0).build();

        when(productCommandService.updateProductCountStock(anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Product not found")));

        StatusRuntimeException ex = null;
        try {
            handler.updateProductCountStock(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("trashedProduct - should return ApiResponseProductDeleteAt on success")
    void trashedProduct_Success() {
        setupProductResponseDeleteAt();
        when(apiResponseDeleteAtSuccess.status()).thenReturn("success");
        when(apiResponseDeleteAtSuccess.message()).thenReturn("Product trashed successfully");
        when(apiResponseDeleteAtSuccess.data()).thenReturn(productResponseDeleteAt);

        ProductCommon.FindByIdProductRequest request = ProductCommon.FindByIdProductRequest.newBuilder().setId(1)
                .build();

        when(productCommandService.trashedProduct(any())).thenReturn(Uni.createFrom().item(apiResponseDeleteAtSuccess));

        ProductCommon.ApiResponseProductDeleteAt response = handler.trashedProduct(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getDeletedAt().getValue()).isEqualTo("2024-01-02 00:00:00.0");
    }

    @Test
    @DisplayName("restoreProduct - should return ApiResponseProductDeleteAt on success")
    void restoreProduct_Success() {
        setupProductResponseDeleteAt();
        when(apiResponseDeleteAtSuccess.status()).thenReturn("success");
        when(apiResponseDeleteAtSuccess.message()).thenReturn("Product restored successfully");
        when(apiResponseDeleteAtSuccess.data()).thenReturn(productResponseDeleteAt);

        ProductCommon.FindByIdProductRequest request = ProductCommon.FindByIdProductRequest.newBuilder().setId(1)
                .build();

        when(productCommandService.restoreProduct(any())).thenReturn(Uni.createFrom().item(apiResponseDeleteAtSuccess));

        ProductCommon.ApiResponseProductDeleteAt response = handler.restoreProduct(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("deleteProductPermanent - should return ApiResponseProductDelete on success")
    void deleteProductPermanent_Success() {
        when(apiResponseEmptySuccess.status()).thenReturn("success");
        when(apiResponseEmptySuccess.message()).thenReturn("Product deleted permanently");

        ProductCommon.FindByIdProductRequest request = ProductCommon.FindByIdProductRequest.newBuilder().setId(1)
                .build();

        when(productCommandService.deleteProductPermanent(any()))
                .thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

        ProductCommon.ApiResponseProductDelete response = handler.deleteProductPermanent(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Product deleted permanently");
    }

    @Test
    @DisplayName("restoreAllProduct - should return ApiResponseProductAll on success")
    void restoreAllProduct_Success() {
        when(apiResponseEmptySuccess.status()).thenReturn("success");
        when(apiResponseEmptySuccess.message()).thenReturn("All products restored successfully");

        when(productCommandService.restoreAllProducts()).thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

        ProductCommon.ApiResponseProductAll response = handler
                .restoreAllProduct(com.google.protobuf.Empty.getDefaultInstance()).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("deleteAllProductPermanent - should return ApiResponseProductAll on success")
    void deleteAllProductPermanent_Success() {
        when(apiResponseEmptySuccess.status()).thenReturn("success");
        when(apiResponseEmptySuccess.message()).thenReturn("All products deleted permanently");

        when(productCommandService.deleteAllProductsPermanent())
                .thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

        ProductCommon.ApiResponseProductAll response = handler
                .deleteAllProductPermanent(com.google.protobuf.Empty.getDefaultInstance()).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
    }

    private void setupProductResponse() {
        when(productResponse.getId()).thenReturn(1L);
        when(productResponse.getMerchantId()).thenReturn(1);
        when(productResponse.getCategoryId()).thenReturn(1);
        when(productResponse.getName()).thenReturn("Laptop");
        when(productResponse.getDescription()).thenReturn("High-end laptop");
        when(productResponse.getPrice()).thenReturn(15000000);
        when(productResponse.getCountInStock()).thenReturn(10);
        when(productResponse.getBrand()).thenReturn("BrandX");
        when(productResponse.getWeight()).thenReturn(1500);
        when(productResponse.getRating()).thenReturn(4.5f);
        when(productResponse.getSlugProduct()).thenReturn("laptop");
        when(productResponse.getImageProduct()).thenReturn("image.jpg");
        when(productResponse.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
        when(productResponse.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");
    }

    private void setupProductResponseDeleteAt() {
        when(productResponseDeleteAt.getId()).thenReturn(1L);
        when(productResponseDeleteAt.getName()).thenReturn("Laptop");
        when(productResponseDeleteAt.getDeletedAt()).thenReturn("2024-01-02 00:00:00.0");

        when(productResponseDeleteAt.getMerchantId()).thenReturn(1);
        when(productResponseDeleteAt.getCategoryId()).thenReturn(1);
        when(productResponseDeleteAt.getDescription()).thenReturn("desc");
        when(productResponseDeleteAt.getPrice()).thenReturn("15000");
        when(productResponseDeleteAt.getCountInStock()).thenReturn("10");
        when(productResponseDeleteAt.getBrand()).thenReturn("BrandX");
        when(productResponseDeleteAt.getWeight()).thenReturn("100");
        when(productResponseDeleteAt.getRating()).thenReturn("4.5");
        when(productResponseDeleteAt.getSlugProduct()).thenReturn("laptop");
        when(productResponseDeleteAt.getImageProduct()).thenReturn("image.jpg");
        when(productResponseDeleteAt.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
        when(productResponseDeleteAt.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");
    }
}
