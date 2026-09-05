package com.sanedge.category.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.category.domain.requests.CreateCategoryRequest;
import com.sanedge.category.domain.requests.UpdateCategoryRequest;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.service.CategoryCommandService;
import com.sanedge.common.domain.response.ApiResponse;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.category.CategoryCommand;
import pb.category.CategoryCommon;

@ExtendWith(MockitoExtension.class)
class CategoryCommandGrpcHandlerTest {

    @Mock
    private CategoryCommandService categoryCommandService;

    private CategoryCommandGrpcHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new CategoryCommandGrpcHandler();
        injectService(handler, categoryCommandService);
    }

    private void injectService(Object target, Object service) throws Exception {
        Field field = target.getClass().getDeclaredField("categoryCommandService");
        field.setAccessible(true);
        field.set(target, service);
    }

    @Test
    @DisplayName("create - should return success response when category created successfully")
    void create_Success() {
        CategoryCommand.CreateCategoryRequest request = CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName("Electronics")
                .setDescription("Electronic devices and gadgets")
                .setSlugCategory("electronics")
                .setImageCategory("electronics.jpg")
                .build();

        CategoryResponse mockResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .slugCategory("electronics")
                .imageCategory("electronics.jpg")
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();

        ApiResponse<CategoryResponse> apiResponse = ApiResponse.success("Category created successfully", mockResponse);

        when(categoryCommandService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategory response = handler.create(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Category created successfully");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getName()).isEqualTo("Electronics");
        assertThat(response.getData().getSlugCategory()).isEqualTo("electronics");
    }

    @Test
    @DisplayName("create - should return INTERNAL error when exception thrown")
    void create_InternalError() {
        CategoryCommand.CreateCategoryRequest request = CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName("Electronics")
                .setDescription("Electronic devices and gadgets")
                .setSlugCategory("electronics")
                .setImageCategory("electronics.jpg")
                .build();

        when(categoryCommandService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

        StatusRuntimeException exception = null;
        try {
            handler.create(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    @DisplayName("update - should return success response when category updated successfully")
    void update_Success() {
        CategoryCommand.UpdateCategoryRequest request = CategoryCommand.UpdateCategoryRequest.newBuilder()
                .setCategoryId(1)
                .setName("Updated Electronics")
                .setDescription("Updated description")
                .setSlugCategory("updated-electronics")
                .setImageCategory("updated.jpg")
                .build();

        CategoryResponse mockResponse = CategoryResponse.builder()
                .id(1L)
                .name("Updated Electronics")
                .description("Updated description")
                .slugCategory("updated-electronics")
                .imageCategory("updated.jpg")
                .createdAt("2024-01-01")
                .updatedAt("2024-01-02")
                .build();

        ApiResponse<CategoryResponse> apiResponse = ApiResponse.success("Category updated successfully", mockResponse);

        when(categoryCommandService.updateCategory(any(UpdateCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategory response = handler.update(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Category updated successfully");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("Updated Electronics");
        assertThat(response.getData().getSlugCategory()).isEqualTo("updated-electronics");
    }

    @Test
    @DisplayName("update - should return NOT_FOUND when category not found")
    void update_NotFound() {
        CategoryCommand.UpdateCategoryRequest request = CategoryCommand.UpdateCategoryRequest.newBuilder()
                .setCategoryId(999)
                .setName("Updated Electronics")
                .setDescription("Updated description")
                .setSlugCategory("updated-electronics")
                .setImageCategory("updated.jpg")
                .build();

        when(categoryCommandService.updateCategory(any(UpdateCategoryRequest.class)))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Category not found")));

        StatusRuntimeException exception = null;
        try {
            handler.update(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("trashedCategory - should return success response when category trashed successfully")
    void trashedCategory_Success() {
        CategoryCommon.FindByIdCategoryRequest request = CategoryCommon.FindByIdCategoryRequest.newBuilder()
                .setId(1)
                .build();

        CategoryResponseDeleteAt mockResponse = CategoryResponseDeleteAt.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .slugCategory("electronics")
                .imageCategory("electronics.jpg")
                .deletedAt("2024-01-02T10:00:00")
                .build();

        ApiResponse<CategoryResponseDeleteAt> apiResponse = ApiResponse.success("Category trashed successfully", mockResponse);

        when(categoryCommandService.trashedCategory(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategoryDeleteAt response = handler.trashedCategory(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Category trashed successfully");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getDeletedAt().getValue()).isEqualTo("2024-01-02T10:00:00");
    }

    @Test
    @DisplayName("trashedCategory - should return NOT_FOUND when category not found")
    void trashedCategory_NotFound() {
        CategoryCommon.FindByIdCategoryRequest request = CategoryCommon.FindByIdCategoryRequest.newBuilder()
                .setId(999)
                .build();

        when(categoryCommandService.trashedCategory(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Trashed category not found with id: 999")));

        StatusRuntimeException exception = null;
        try {
            handler.trashedCategory(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("restoreCategory - should return success response when category restored successfully")
    void restoreCategory_Success() {
        CategoryCommon.FindByIdCategoryRequest request = CategoryCommon.FindByIdCategoryRequest.newBuilder()
                .setId(1)
                .build();

        CategoryResponseDeleteAt mockResponse = CategoryResponseDeleteAt.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .slugCategory("electronics")
                .imageCategory("electronics.jpg")
                .build();

        ApiResponse<CategoryResponseDeleteAt> apiResponse = ApiResponse.success("Category restored successfully", mockResponse);

        when(categoryCommandService.restoreCategory(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategoryDeleteAt response = handler.restoreCategory(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Category restored successfully");
    }

    @Test
    @DisplayName("restoreCategory - should return NOT_FOUND when category not found")
    void restoreCategory_NotFound() {
        CategoryCommon.FindByIdCategoryRequest request = CategoryCommon.FindByIdCategoryRequest.newBuilder()
                .setId(999)
                .build();

        when(categoryCommandService.restoreCategory(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Restore category not found with id: 999")));

        StatusRuntimeException exception = null;
        try {
            handler.restoreCategory(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("deleteCategoryPermanent - should return success when category deleted permanently")
    void deleteCategoryPermanent_Success() {
        CategoryCommon.FindByIdCategoryRequest request = CategoryCommon.FindByIdCategoryRequest.newBuilder()
                .setId(1)
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.success("Category deleted permanently");

        when(categoryCommandService.deleteCategoryPermanent(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategoryDelete response = handler.deleteCategoryPermanent(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Category deleted permanently");
    }

    @Test
    @DisplayName("deleteCategoryPermanent - should return NOT_FOUND when category not found or not trashed")
    void deleteCategoryPermanent_NotFound() {
        CategoryCommon.FindByIdCategoryRequest request = CategoryCommon.FindByIdCategoryRequest.newBuilder()
                .setId(999)
                .build();

        when(categoryCommandService.deleteCategoryPermanent(999L))
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("Category not found or must be trashed before permanent deletion")));

        StatusRuntimeException exception = null;
        try {
            handler.deleteCategoryPermanent(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("restoreAllCategory - should return success when all categories restored")
    void restoreAllCategory_Success() {
        Empty request = Empty.getDefaultInstance();

        ApiResponse<Void> apiResponse = ApiResponse.success("All categories restored successfully");

        when(categoryCommandService.restoreAllCategories()).thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategoryAll response = handler.restoreAllCategory(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All categories restored successfully");
    }

    @Test
    @DisplayName("restoreAllCategory - should return INTERNAL error when no trashed categories found")
    void restoreAllCategory_NotFound() {
        Empty request = Empty.getDefaultInstance();

        when(categoryCommandService.restoreAllCategories())
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("No trashed categories found")));

        StatusRuntimeException exception = null;
        try {
            handler.restoreAllCategory(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("deleteAllCategoryPermanent - should return success when all trashed categories deleted")
    void deleteAllCategoryPermanent_Success() {
        Empty request = Empty.getDefaultInstance();

        ApiResponse<Void> apiResponse = ApiResponse.success("All categories permanently deleted");

        when(categoryCommandService.deleteAllCategoriesPermanent()).thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategoryAll response = handler.deleteAllCategoryPermanent(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All categories permanently deleted");
    }

    @Test
    @DisplayName("deleteAllCategoryPermanent - should return INTERNAL error when no trashed categories found")
    void deleteAllCategoryPermanent_NotFound() {
        Empty request = Empty.getDefaultInstance();

        when(categoryCommandService.deleteAllCategoriesPermanent())
                .thenReturn(Uni.createFrom().failure(
                        new com.sanedge.common.exception.ResourceNotFoundException("No trashed categories found")));

        StatusRuntimeException exception = null;
        try {
            handler.deleteAllCategoryPermanent(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("create - should correctly map request fields to domain object")
    void create_RequestMapping() {
        CategoryCommand.CreateCategoryRequest request = CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName("Test Category")
                .setDescription("Test description")
                .setSlugCategory("test-category")
                .setImageCategory("test.jpg")
                .build();

        CategoryResponse mockResponse = CategoryResponse.builder()
                .id(1L)
                .name("Test Category")
                .description("Test description")
                .slugCategory("test-category")
                .imageCategory("test.jpg")
                .build();

        ApiResponse<CategoryResponse> apiResponse = ApiResponse.success("Category created", mockResponse);

        when(categoryCommandService.createCategory(any(CreateCategoryRequest.class)))
                .thenAnswer(invocation -> {
                    CreateCategoryRequest domainReq = invocation.getArgument(0);

                    assertThat(domainReq.getName()).isEqualTo("Test Category");
                    assertThat(domainReq.getDescription()).isEqualTo("Test description");
                    assertThat(domainReq.getSlugCategory()).isEqualTo("test-category");
                    assertThat(domainReq.getImageCategory()).isEqualTo("test.jpg");
                    return Uni.createFrom().item(apiResponse);
                });

        CategoryCommon.ApiResponseCategory response = handler.create(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("update - should correctly map request fields to domain object")
    void update_RequestMapping() {
        CategoryCommand.UpdateCategoryRequest request = CategoryCommand.UpdateCategoryRequest.newBuilder()
                .setCategoryId(5)
                .setName("Updated Category")
                .setDescription("Updated description")
                .setSlugCategory("updated-category")
                .setImageCategory("updated.jpg")
                .build();

        CategoryResponse mockResponse = CategoryResponse.builder()
                .id(5L)
                .name("Updated Category")
                .description("Updated description")
                .slugCategory("updated-category")
                .imageCategory("updated.jpg")
                .build();

        ApiResponse<CategoryResponse> apiResponse = ApiResponse.success("Category updated", mockResponse);

        when(categoryCommandService.updateCategory(any(UpdateCategoryRequest.class)))
                .thenAnswer(invocation -> {
                    UpdateCategoryRequest domainReq = invocation.getArgument(0);

                    assertThat(domainReq.getCategoryId()).isEqualTo(5);
                    assertThat(domainReq.getName()).isEqualTo("Updated Category");
                    assertThat(domainReq.getDescription()).isEqualTo("Updated description");
                    assertThat(domainReq.getSlugCategory()).isEqualTo("updated-category");
                    assertThat(domainReq.getImageCategory()).isEqualTo("updated.jpg");
                    return Uni.createFrom().item(apiResponse);
                });

        CategoryCommon.ApiResponseCategory response = handler.update(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    @DisplayName("create - should correctly map null data in response")
    void create_NullData() {
        CategoryCommand.CreateCategoryRequest request = CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName("Electronics")
                .setDescription("Electronic devices and gadgets")
                .setSlugCategory("electronics")
                .setImageCategory("electronics.jpg")
                .build();

        ApiResponse<CategoryResponse> apiResponse = ApiResponse.<CategoryResponse>success("Category created", null);

        when(categoryCommandService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategory response = handler.create(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.hasData()).isFalse();
    }

    @Test
    @DisplayName("trashedCategory - should correctly map null deletedAt in response")
    void trashedCategory_NullDeletedAt() {
        CategoryCommon.FindByIdCategoryRequest request = CategoryCommon.FindByIdCategoryRequest.newBuilder()
                .setId(1)
                .build();

        CategoryResponseDeleteAt mockResponse = CategoryResponseDeleteAt.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .slugCategory("electronics")
                .imageCategory("electronics.jpg")
                .deletedAt(null)
                .build();

        ApiResponse<CategoryResponseDeleteAt> apiResponse = ApiResponse.success("Category restored", mockResponse);

        when(categoryCommandService.restoreCategory(1L)).thenReturn(Uni.createFrom().item(apiResponse));

        CategoryCommon.ApiResponseCategoryDeleteAt response = handler.restoreCategory(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.hasData()).isTrue();
        assertThat(response.getData().hasDeletedAt()).isFalse();
    }
}
