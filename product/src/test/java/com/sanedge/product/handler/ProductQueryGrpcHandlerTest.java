package com.sanedge.product.handler;

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
import com.sanedge.product.domain.requests.FindAllProductRequest;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.service.ProductQueryService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class ProductQueryGrpcHandlerTest {
    @Mock ProductQueryService productQueryService;
    private ProductQueryGrpcHandler productQueryGrpcHandler;

    @BeforeEach
    void setUp() throws Exception {
        productQueryGrpcHandler = new ProductQueryGrpcHandler();
        Field f = ProductQueryGrpcHandler.class.getDeclaredField("productQueryService");
        f.setAccessible(true);
        f.set(productQueryGrpcHandler, productQueryService);
    }

    @Test
    void findById_Success() {
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success("Product retrieved successfully", null);
        lenient().when(productQueryService.findById(any(Long.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));
        var result = productQueryGrpcHandler.findById(
                pb.product.ProductCommon.FindByIdProductRequest.newBuilder().setId(1).build()
        ).await().indefinitely();
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");
    }

    @Test
    void findAll_Success() {
        ApiResponsePagination<List<ProductResponse>> apiResp = new ApiResponsePagination<>(
                "success", "msg", List.of(), null);
        lenient().when(productQueryService.findAll(any(FindAllProductRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResp));
        var result = productQueryGrpcHandler.findAll(
                pb.product.ProductQuery.FindAllProductRequest.newBuilder()
                        .setPage(1).setPageSize(10).build()
        ).await().indefinitely();
        assertThat(result).isNotNull();
    }
}
