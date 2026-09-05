package com.sanedge.category.service;

import com.sanedge.category.domain.requests.CreateCategoryRequest;
import com.sanedge.category.domain.requests.UpdateCategoryRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface CategoryCommandService {
    Uni<ApiResponse<CategoryResponse>> createCategory(CreateCategoryRequest req);
    Uni<ApiResponse<CategoryResponse>> updateCategory(UpdateCategoryRequest req);
    Uni<ApiResponse<CategoryResponseDeleteAt>> trashedCategory(Long categoryId);
    Uni<ApiResponse<CategoryResponseDeleteAt>> restoreCategory(Long categoryId);
    Uni<ApiResponse<Void>> deleteCategoryPermanent(Long categoryId);
    Uni<ApiResponse<Void>> restoreAllCategories();
    Uni<ApiResponse<Void>> deleteAllCategoriesPermanent();
}
