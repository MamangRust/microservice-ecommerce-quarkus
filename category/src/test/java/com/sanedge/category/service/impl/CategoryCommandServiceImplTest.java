package com.sanedge.category.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.category.domain.requests.CreateCategoryRequest;
import com.sanedge.category.domain.requests.UpdateCategoryRequest;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.entity.Category;
import com.sanedge.category.repository.CategoryCommandRepository;
import com.sanedge.category.repository.CategoryQueryRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceImplTest {

    @Mock
    private CategoryCommandRepository categoryCommandRepository;

    @Mock
    private CategoryQueryRepository categoryQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private CategoryCommandServiceImpl service;

    private Answer<Uni<?>> invokeSupplier() {
        return invocation -> {
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier) {
                    return ((Supplier<Uni<?>>) arg).get();
                }
            }
            return Uni.createFrom().nullItem();
        };
    }

    @BeforeEach
    void setUp() {
        service = new CategoryCommandServiceImpl(
                categoryCommandRepository,
                categoryQueryRepository,
                redisService,
                tracingMetrics);

        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(
                        anyString(),
                        anyString(),
                        any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());

        lenient()
                .when(redisService.deleteReactive(anyString()))
                .thenReturn(Uni.createFrom().voidItem());
    }

    private Category createMockCategory(Long id, String name) {
        Category c = new Category();
        c.id = id;
        c.setName(name);
        c.setDescription("Description " + name);
        c.setSlugCategory("slug-" + name);
        c.setImageCategory(name + ".jpg");
        c.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        c.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return c;
    }

    @Test
    void createCategory_success_returnsNewCategory() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Electronics");
        req.setDescription("Electronic devices");
        req.setSlugCategory("electronics");
        req.setImageCategory("electronics.jpg");

        lenient().when(categoryCommandRepository.persist(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category c = invocation.getArgument(0);
                    c.id = 1L;
                    return Uni.createFrom().item(c);
                });

        ApiResponse<CategoryResponse> result = service.createCategory(req).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Category created successfully");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().getName()).isEqualTo("Electronics");
    }

    @Test
    void updateCategory_success_returnsUpdatedCategory() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setCategoryId(1);
        req.setName("Updated");
        req.setDescription("Updated desc");
        req.setSlugCategory("updated");
        req.setImageCategory("updated.jpg");

        Category existing = createMockCategory(1L, "Old");
        when(categoryQueryRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(existing));
        when(categoryCommandRepository.persist(any(Category.class)))
                .thenReturn(Uni.createFrom().item(existing));

        ApiResponse<CategoryResponse> result = service.updateCategory(req).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Category updated successfully");
        assertThat(result.data().getName()).isEqualTo("Updated");
    }

    @Test
    void updateCategory_notFound_throwsResourceNotFoundException() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setCategoryId(999);
        req.setName("Test");
        req.setDescription("Test");
        req.setSlugCategory("test");

        when(categoryQueryRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        try {
            service.updateCategory(req).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Category not found");
        }
    }

    @Test
    void updateCategory_nullCategoryId_throwsResourceNotFoundException() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setCategoryId(null);

        try {
            service.updateCategory(req).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("category_id is required");
        }
    }

    @Test
    void trashedCategory_success_returnsTrashedCategory() {
        Category trashed = createMockCategory(1L, "Electronics");
        trashed.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        when(categoryCommandRepository.trash(anyLong())).thenReturn(Uni.createFrom().item(trashed));

        ApiResponse<CategoryResponseDeleteAt> result = service.trashedCategory(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Category trashed successfully");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void trashedCategory_notFound_throwsResourceNotFoundException() {
        when(categoryCommandRepository.trash(anyLong())).thenReturn(Uni.createFrom().nullItem());

        try {
            service.trashedCategory(999L).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Trashed category not found");
        }
    }

    @Test
    void restoreCategory_success_returnsRestoredCategory() {
        Category restored = createMockCategory(1L, "Electronics");
        when(categoryCommandRepository.restore(anyLong())).thenReturn(Uni.createFrom().item(restored));

        ApiResponse<CategoryResponseDeleteAt> result = service.restoreCategory(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Category restored successfully");
    }

    @Test
    void restoreCategory_notFound_throwsResourceNotFoundException() {
        when(categoryCommandRepository.restore(anyLong())).thenReturn(Uni.createFrom().nullItem());

        try {
            service.restoreCategory(999L).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Restore category not found");
        }
    }

    @Test
    void deleteCategoryPermanent_success_returnsSuccess() {
        Category deleted = createMockCategory(1L, "Electronics");
        deleted.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        when(categoryCommandRepository.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(deleted));

        ApiResponse<Void> result = service.deleteCategoryPermanent(1L).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Category deleted permanently");
    }

    @Test
    void deleteCategoryPermanent_notFound_throwsInvalidRequestException() {
        when(categoryCommandRepository.deletePermanent(anyLong())).thenReturn(Uni.createFrom().nullItem());

        try {
            service.deleteCategoryPermanent(999L).await().indefinitely();
            Assertions.fail("Expected InvalidRequestException");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage()).contains("must be trashed before permanent deletion");
        }
    }

    @Test
    void restoreAllCategories_success_returnsSuccess() {
        when(categoryCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));

        ApiResponse<Void> result = service.restoreAllCategories().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All categories restored successfully");
    }

    @Test
    void restoreAllCategories_noneFound_throwsResourceNotFoundException() {
        when(categoryCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));

        try {
            service.restoreAllCategories().await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No trashed categories found");
        }
    }

    @Test
    void deleteAllCategoriesPermanent_success_returnsSuccess() {
        when(categoryCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));

        ApiResponse<Void> result = service.deleteAllCategoriesPermanent().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All categories permanently deleted");
    }

    @Test
    void deleteAllCategoriesPermanent_noneFound_throwsResourceNotFoundException() {
        when(categoryCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));

        try {
            service.deleteAllCategoriesPermanent().await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No trashed categories found");
        }
    }
}
