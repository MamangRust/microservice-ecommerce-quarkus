package com.sanedge.category.service;

import java.util.List;

import com.sanedge.category.domain.requests.FindAllCategoryRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface CategoryQueryService {
    Uni<ApiResponsePagination<List<CategoryResponse>>> findAll(FindAllCategoryRequest req);
    Uni<ApiResponsePagination<List<CategoryResponseDeleteAt>>> findByActive(FindAllCategoryRequest req);
    Uni<ApiResponsePagination<List<CategoryResponseDeleteAt>>> findByTrashed(FindAllCategoryRequest req);
    Uni<ApiResponse<CategoryResponse>> findById(Long categoryId);
}
