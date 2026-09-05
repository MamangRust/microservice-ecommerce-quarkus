package com.sanedge.category.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.category.domain.requests.FindAllCategoryRequest;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.entity.Category;
import com.sanedge.category.repository.CategoryQueryRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceImplTest {

    @Mock
    private CategoryQueryRepository categoryQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private CategoryQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new CategoryQueryServiceImpl(
                categoryQueryRepository,
                redisService,
                objectMapper,
                tracingMetrics);

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
    }

    private Category createMockCategory(Long id) {
        Category c = new Category();
        c.id = id;
        c.setName("Cat-" + id);
        c.setDescription("Description " + id);
        c.setSlugCategory("slug-" + id);
        c.setImageCategory("img-" + id + ".jpg");
        c.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        c.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return c;
    }

    @Test
    void findAll_cacheMiss_returnsFromDb() {
        FindAllCategoryRequest req = new FindAllCategoryRequest();
        req.setPage(1);
        req.setPageSize(10);

        PagedResult<Category> paged = new PagedResult<>(List.of(createMockCategory(1L)), 1);
        when(categoryQueryRepository.findCategories(any())).thenReturn(Uni.createFrom().item(paged));
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<CategoryResponse>> result = service.findAll(req).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Categories retrieved successfully");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).getName()).isEqualTo("Cat-1");
    }

    @Test
    void findAll_cacheHit_returnsCached() {
        FindAllCategoryRequest req = new FindAllCategoryRequest();
        req.setPage(1);
        req.setPageSize(10);

        CategoryResponse cached = CategoryResponse.from(createMockCategory(1L));
        ApiResponsePagination<List<CategoryResponse>> cachedResponse = new ApiResponsePagination<>(
                "success", "Categories retrieved successfully", List.of(cached), null);
        String json = toJson(cachedResponse);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(json));

        ApiResponsePagination<List<CategoryResponse>> result = service.findAll(req).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void findByActive_cacheMiss_returnsFromDb() {
        FindAllCategoryRequest req = new FindAllCategoryRequest();
        req.setPage(1);
        req.setPageSize(10);

        PagedResult<Category> paged = new PagedResult<>(List.of(createMockCategory(2L)), 1);
        when(categoryQueryRepository.findActiveCategories(any())).thenReturn(Uni.createFrom().item(paged));
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<CategoryResponseDeleteAt>> result = service.findByActive(req).await()
                .indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Active categories retrieved successfully");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void findByActive_cacheHit_returnsCached() {
        FindAllCategoryRequest req = new FindAllCategoryRequest();
        req.setPage(1);
        req.setPageSize(10);

        CategoryResponseDeleteAt cached = CategoryResponseDeleteAt.from(createMockCategory(2L));
        ApiResponsePagination<List<CategoryResponseDeleteAt>> cachedResponse = new ApiResponsePagination<>(
                "success", "Active categories retrieved successfully", List.of(cached), null);
        String json = toJson(cachedResponse);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(json));

        ApiResponsePagination<List<CategoryResponseDeleteAt>> result = service.findByActive(req).await()
                .indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void findByTrashed_cacheMiss_returnsFromDb() {
        FindAllCategoryRequest req = new FindAllCategoryRequest();
        req.setPage(1);
        req.setPageSize(10);

        PagedResult<Category> paged = new PagedResult<>(List.of(createMockCategory(3L)), 1);
        when(categoryQueryRepository.findTrashedCategories(any())).thenReturn(Uni.createFrom().item(paged));
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<CategoryResponseDeleteAt>> result = service.findByTrashed(req).await()
                .indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Trashed categories retrieved successfully");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void findById_cacheMiss_returnsFromDb() {
        Category mock = createMockCategory(1L);
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(categoryQueryRepository.findCategoryById(anyLong())).thenReturn(Uni.createFrom().item(mock));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<CategoryResponse> result = service.findById(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Category retrieved successfully");
        assertThat(result.data().getId()).isEqualTo(1L);
        assertThat(result.data().getName()).isEqualTo("Cat-1");
    }

    @Test
    void findById_cacheHit_returnsCached() {
        CategoryResponse cached = CategoryResponse.from(createMockCategory(1L));
        String json = toJson(cached);
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(json));

        ApiResponse<CategoryResponse> result = service.findById(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().getId()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(categoryQueryRepository.findCategoryById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        try {
            service.findById(999L).await().indefinitely();
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Category not found");
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize", e);
        }
    }
}
