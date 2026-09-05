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

import com.sanedge.gateway.dto.CategoryDto;
import com.sanedge.gateway.service.CategoryService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class CategoryResourceTest {
    @Mock
    CategoryService categoryService;
    private CategoryResource categoryResource;

    @BeforeEach
    void setUp() throws Exception {
        categoryResource = new CategoryResource();
        Field f = CategoryResource.class.getDeclaredField("categoryService");
        f.setAccessible(true);
        f.set(categoryResource, categoryService);
    }

    private CategoryDto.CategoryResponse mk(int id) {
        return new CategoryDto.CategoryResponse(id, "name", "desc", "slug", "", "", "");
    }

    @Test
    void findAll_Success() {

        CategoryDto.ApiResponsePaginationCategory dto = new CategoryDto.ApiResponsePaginationCategory(
                List.of(mk(1)), "success", "ok");
        lenient().when(categoryService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        CategoryDto.ApiResponsePaginationCategory dto = new CategoryDto.ApiResponsePaginationCategory(
                List.of(), "success", "ok");
        lenient().when(categoryService.findByActive(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        CategoryDto.ApiResponsePaginationCategory dto = new CategoryDto.ApiResponsePaginationCategory(
                List.of(), "success", "ok");
        lenient().when(categoryService.findByTrashed(anyInt(), anyInt(), anyString()))
                .thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        CategoryDto.ApiResponseCategory dto = new CategoryDto.ApiResponseCategory(mk(1), "success", "ok");
        lenient().when(categoryService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createCategory_Success_Returns201() {
        CategoryDto.ApiResponseCategory dto = new CategoryDto.ApiResponseCategory(mk(1), "success", "ok");
        lenient().when(categoryService.create(any())).thenReturn(Uni.createFrom().item(dto));
        CategoryDto.CreateCategoryRequest req = new CategoryDto.CreateCategoryRequest("name", "desc", "slug", "image");
        Response r = categoryResource.createCategory(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateCategory_Success() {
        CategoryDto.ApiResponseCategory dto = new CategoryDto.ApiResponseCategory(mk(1), "success", "ok");
        lenient().when(categoryService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        CategoryDto.UpdateCategoryRequest req = new CategoryDto.UpdateCategoryRequest("name", "desc", "slug", "image");
        Response r = categoryResource.updateCategory(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void trashedCategory_Success() {

        CategoryDto.ApiResponseCategory dto = new CategoryDto.ApiResponseCategory(mk(1), "success", "ok");
        lenient().when(categoryService.trash(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.trashedCategory(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreCategory_Success() {
        CategoryDto.ApiResponseCategory dto = new CategoryDto.ApiResponseCategory(mk(1), "success", "ok");
        lenient().when(categoryService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.restoreCategory(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteCategoryPermanent_Success() {
        CategoryDto.SimpleResponse dto = new CategoryDto.SimpleResponse("success", "ok");
        lenient().when(categoryService.deleteCategoryPermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.deleteCategoryPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllCategories_Success() {
        CategoryDto.SimpleResponse dto = new CategoryDto.SimpleResponse("success", "ok");
        lenient().when(categoryService.restoreAllCategories()).thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.restoreAllCategories().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllCategoriesPermanent_Success() {
        CategoryDto.SimpleResponse dto = new CategoryDto.SimpleResponse("success", "ok");
        lenient().when(categoryService.deleteAllCategoriesPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = categoryResource.deleteAllCategoriesPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
