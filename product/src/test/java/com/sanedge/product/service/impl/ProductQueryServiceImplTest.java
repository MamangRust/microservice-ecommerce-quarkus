package com.sanedge.product.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.product.domain.requests.FindAllProductRequest;
import com.sanedge.product.entity.Product;
import com.sanedge.product.repository.ProductQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceImplTest {

    @Mock
    private ProductQueryRepository productQueryRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private TracingMetrics tracingMetrics;

    private ProductQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                        .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        service = new ProductQueryServiceImpl(
                productQueryRepository,
                redisService,
                objectMapper,
                tracingMetrics);
    }

    private Product mkProduct(Long id) {
        Product p = new Product();
        try {
            Field idField = p.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        p.setName("Phone");
        p.setPrice(10000);
        p.setCountInStock(5);
        p.setMerchantId(1);
        p.setCategoryId(1);
        p.setDescription("Test phone");
        p.setBrand("TestBrand");
        p.setWeight(500);
        p.setRating(4.5f);
        p.setSlugProduct("phone");
        p.setImageProduct("http://example.com/phone.jpg");
        p.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        p.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return p;
    }

    @Test
    void findById_Success() {
        lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(productQueryRepository.findProductById(anyLong()))
                .thenReturn(Uni.createFrom().item(Optional.of(mkProduct(1L))));
        ApiResponse<?> result = service.findById(1L).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Product retrieved successfully");
    }

    @Test
    void findById_NotFound_ThrowsResourceNotFound() {
        lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(productQueryRepository.findProductById(anyLong())).thenReturn(Uni.createFrom().item(Optional.empty()));
        org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.findById(999L).await().indefinitely());
    }

    @Test
    void findAll_Success() {
        FindAllProductRequest req = new FindAllProductRequest();
        req.setPage(1);
        req.setPageSize(10);
        lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(productQueryRepository.findProducts(any(FindAllProductRequest.class)))
                .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(), 0)));
        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());
        ApiResponsePagination<List<com.sanedge.product.domain.response.ProductResponse>> result = service.findAll(req)
                .await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    /**
     * Finds the Supplier argument in the invocation regardless of whether it was
     * passed positionally in the 3-arg overload (arg index 2) or 4-arg overload
     * (arg index 3), then invokes it and returns the resulting Uni. This lets
     * a single Answer<?> body serve both traceAndMeasure overloads.
     */
    private Answer<Uni<?>> invokeSupplier() {
        return invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? (Uni<?>) supplier.get() : null;
        };
    }
}
