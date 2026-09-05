package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.CategoryDto;
import io.smallrye.mutiny.Uni;

public interface CategoryService {
    Uni<CategoryDto.ApiResponsePaginationCategory> findAll(int page, int size, String search);
    Uni<CategoryDto.ApiResponsePaginationCategory> findByActive(int page, int size, String search);
    Uni<CategoryDto.ApiResponsePaginationCategory> findByTrashed(int page, int size, String search);
    Uni<CategoryDto.ApiResponseCategory> findById(int id);
    Uni<CategoryDto.ApiResponseMonthlyTotalPrice> findMonthTotalPrice(int year, int month);
    Uni<CategoryDto.ApiResponseYearlyTotalPrice> findYearTotalPrice(int year);
    Uni<CategoryDto.ApiResponseMonthlyTotalPrice> findMonthTotalPriceByMerchant(int merchantId, int year, int month);
    Uni<CategoryDto.ApiResponseYearlyTotalPrice> findYearlyTotalPricesByMerchant(int merchantId, int year);
    Uni<CategoryDto.ApiResponseMonthlyTotalPrice> findMonthlyTotalPricesById(int categoryId, int year, int month);
    Uni<CategoryDto.ApiResponseYearlyTotalPrice> findYearlyTotalPricesById(int categoryId, int year);
    Uni<CategoryDto.ApiResponseMonthlyPrice> findMonthPrice(int year);
    Uni<CategoryDto.ApiResponseYearlyPrice> findYearPrice(int year);
    Uni<CategoryDto.ApiResponseMonthlyPrice> findMonthPriceByMerchant(int merchantId, int year);
    Uni<CategoryDto.ApiResponseYearlyPrice> findYearPriceByMerchant(int merchantId, int year);
    Uni<CategoryDto.ApiResponseMonthlyPrice> findMonthPriceById(int categoryId, int year);
    Uni<CategoryDto.ApiResponseYearlyPrice> findYearPriceById(int categoryId, int year);
    Uni<CategoryDto.ApiResponseCategory> create(CategoryDto.CreateCategoryRequest body);
    Uni<CategoryDto.ApiResponseCategory> update(int id, CategoryDto.UpdateCategoryRequest body);
    Uni<CategoryDto.ApiResponseCategory> trash(int id);
    Uni<CategoryDto.ApiResponseCategory> restore(int id);
    Uni<CategoryDto.SimpleResponse> deleteCategoryPermanent(int id);
    Uni<CategoryDto.SimpleResponse> restoreAllCategories();
    Uni<CategoryDto.SimpleResponse> deleteAllCategoriesPermanent();
}
