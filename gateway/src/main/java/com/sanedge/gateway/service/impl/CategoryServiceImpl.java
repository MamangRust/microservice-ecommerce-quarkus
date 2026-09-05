package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.CategoryDto;
import com.sanedge.gateway.service.CategoryService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CategoryServiceImpl implements CategoryService {

    private static final Logger LOG = Logger.getLogger(CategoryServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("category")
    pb.category.MutinyCategoryQueryServiceGrpc.MutinyCategoryQueryServiceStub categoryQueryService;

    @GrpcClient("category")
    pb.category.MutinyCategoryCommandServiceGrpc.MutinyCategoryCommandServiceStub categoryCommandService;

    @GrpcClient("statsreader")
    pb.category.stats.MutinyCategoryPriceServiceGrpc.MutinyCategoryPriceServiceStub categoryPriceService;

    @GrpcClient("statsreader")
    pb.category.stats.MutinyCategoryPriceByIdServiceGrpc.MutinyCategoryPriceByIdServiceStub categoryPriceByIdService;

    @GrpcClient("statsreader")
    pb.category.stats.MutinyCategoryPriceByMerchantGrpc.MutinyCategoryPriceByMerchantStub categoryPriceByMerchantService;

    @GrpcClient("statsreader")
    pb.category.stats.MutinyCategoryTotalPriceServiceGrpc.MutinyCategoryTotalPriceServiceStub categoryTotalPriceService;

    @GrpcClient("statsreader")
    pb.category.stats.MutinyCategoryTotalPriceByIdGrpc.MutinyCategoryTotalPriceByIdStub categoryTotalPriceByIdService;

    @GrpcClient("statsreader")
    pb.category.stats.MutinyCategoryTotalPriceByMerchantGrpc.MutinyCategoryTotalPriceByMerchantStub categoryTotalPriceByMerchantService;

    @Override
    public Uni<CategoryDto.ApiResponsePaginationCategory> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("category.findAll", () -> categoryQueryService.findAll(
                pb.category.CategoryQuery.FindAllCategoryRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(CategoryDto.ApiResponsePaginationCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponsePaginationCategory> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("category.findByActive", () -> categoryQueryService.findByActive(
                pb.category.CategoryQuery.FindAllCategoryRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(CategoryDto.ApiResponsePaginationCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponsePaginationCategory> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("category.findByTrashed", () -> categoryQueryService.findByTrashed(
                pb.category.CategoryQuery.FindAllCategoryRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(CategoryDto.ApiResponsePaginationCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategory> findById(int id) {
        return telemetryHelper.traceAndMetric("category.findById", () -> categoryQueryService.findById(
                pb.category.CategoryCommon.FindByIdCategoryRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(CategoryDto.ApiResponseCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find category by id " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseMonthlyTotalPrice> findMonthTotalPrice(int year, int month) {
        return telemetryHelper.traceAndMetric("category.findMonthTotalPrice", () -> categoryTotalPriceService.findMonthlyTotalPrices(
                pb.category.CategoryCommon.FindYearMonthTotalPrices.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(CategoryDto.ApiResponseMonthlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total price stats: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseYearlyTotalPrice> findYearTotalPrice(int year) {
        return telemetryHelper.traceAndMetric("category.findYearTotalPrice", () -> categoryTotalPriceService.findYearlyTotalPrices(
                pb.category.CategoryCommon.FindYearTotalPrices.newBuilder()
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseYearlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total price stats: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseMonthlyTotalPrice> findMonthTotalPriceByMerchant(int merchantId, int year, int month) {
        return telemetryHelper.traceAndMetric("category.findMonthTotalPriceByMerchant", () -> categoryTotalPriceByMerchantService.findMonthlyTotalPricesByMerchant(
                pb.category.CategoryCommon.FindYearMonthTotalPriceByMerchant.newBuilder()
                        .setMerchantId(merchantId)
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(CategoryDto.ApiResponseMonthlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total price by merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseYearlyTotalPrice> findYearlyTotalPricesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("category.findYearTotalPriceByMerchant", () -> categoryTotalPriceByMerchantService.findYearlyTotalPricesByMerchant(
                pb.category.CategoryCommon.FindYearTotalPriceByMerchant.newBuilder()
                        .setMerchantId(merchantId)
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseYearlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total price by merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseMonthlyTotalPrice> findMonthlyTotalPricesById(int categoryId, int year, int month) {
        return telemetryHelper.traceAndMetric("category.findMonthTotalPriceById", () -> categoryTotalPriceByIdService.findMonthlyTotalPricesById(
                pb.category.CategoryCommon.FindYearMonthTotalPriceById.newBuilder()
                        .setCategoryId(categoryId)
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(CategoryDto.ApiResponseMonthlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total price by ID: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseYearlyTotalPrice> findYearlyTotalPricesById(int categoryId, int year) {
        return telemetryHelper.traceAndMetric("category.findYearTotalPriceById", () -> categoryTotalPriceByIdService.findYearlyTotalPricesById(
                pb.category.CategoryCommon.FindYearTotalPriceById.newBuilder()
                        .setCategoryId(categoryId)
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseYearlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total price by ID: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseMonthlyPrice> findMonthPrice(int year) {
        return telemetryHelper.traceAndMetric("category.findMonthPrice", () -> categoryPriceService.findMonthPrice(
                pb.category.CategoryCommon.FindYearCategory.newBuilder()
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseMonthlyPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly price stats: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseYearlyPrice> findYearPrice(int year) {
        return telemetryHelper.traceAndMetric("category.findYearPrice", () -> categoryPriceService.findYearPrice(
                pb.category.CategoryCommon.FindYearCategory.newBuilder()
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseYearlyPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly price stats: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseMonthlyPrice> findMonthPriceByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("category.findMonthPriceByMerchant", () -> categoryPriceByMerchantService.findMonthPriceByMerchant(
                pb.category.CategoryCommon.FindYearCategoryByMerchant.newBuilder()
                        .setMerchantId(merchantId)
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseMonthlyPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly price by merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseYearlyPrice> findYearPriceByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("category.findYearPriceByMerchant", () -> categoryPriceByMerchantService.findYearPriceByMerchant(
                pb.category.CategoryCommon.FindYearCategoryByMerchant.newBuilder()
                        .setMerchantId(merchantId)
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseYearlyPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly price by merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseMonthlyPrice> findMonthPriceById(int categoryId, int year) {
        return telemetryHelper.traceAndMetric("category.findMonthPriceById", () -> categoryPriceByIdService.findMonthPriceById(
                pb.category.CategoryCommon.FindYearCategoryById.newBuilder()
                        .setCategoryId(categoryId)
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseMonthlyPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly price by ID: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseYearlyPrice> findYearPriceById(int categoryId, int year) {
        return telemetryHelper.traceAndMetric("category.findYearPriceById", () -> categoryPriceByIdService.findYearPriceById(
                pb.category.CategoryCommon.FindYearCategoryById.newBuilder()
                        .setCategoryId(categoryId)
                        .setYear(year)
                        .build())
                .map(CategoryDto.ApiResponseYearlyPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly price by ID: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategory> create(CategoryDto.CreateCategoryRequest body) {
        pb.category.CategoryCommand.CreateCategoryRequest req = pb.category.CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName(body.name() == null ? "" : body.name())
                .setDescription(body.description() == null ? "" : body.description())
                .setSlugCategory(body.slugCategory() == null ? "" : body.slugCategory())
                .setImageCategory(body.imageCategory() == null ? "" : body.imageCategory())
                .build();
        return telemetryHelper.traceAndMetric("category.create", () -> categoryCommandService.create(req)
                .map(CategoryDto.ApiResponseCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create category: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategory> update(int id, CategoryDto.UpdateCategoryRequest body) {
        pb.category.CategoryCommand.UpdateCategoryRequest req = pb.category.CategoryCommand.UpdateCategoryRequest.newBuilder()
                .setCategoryId(id)
                .setName(body.name() == null ? "" : body.name())
                .setDescription(body.description() == null ? "" : body.description())
                .setSlugCategory(body.slugCategory() == null ? "" : body.slugCategory())
                .setImageCategory(body.imageCategory() == null ? "" : body.imageCategory())
                .build();
        return telemetryHelper.traceAndMetric("category.update", () -> categoryCommandService.update(req)
                .map(CategoryDto.ApiResponseCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategory> trash(int id) {
        return telemetryHelper.traceAndMetric("category.trash", () -> categoryCommandService.trashedCategory(
                pb.category.CategoryCommon.FindByIdCategoryRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(CategoryDto.ApiResponseCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategory> restore(int id) {
        return telemetryHelper.traceAndMetric("category.restore", () -> categoryCommandService.restoreCategory(
                pb.category.CategoryCommon.FindByIdCategoryRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(CategoryDto.ApiResponseCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.SimpleResponse> deleteCategoryPermanent(int id) {
        return telemetryHelper.traceAndMetric("category.deletePermanent", () -> categoryCommandService.deleteCategoryPermanent(
                pb.category.CategoryCommon.FindByIdCategoryRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(CategoryDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.SimpleResponse> restoreAllCategories() {
        return telemetryHelper.traceAndMetric("category.restoreAll", () -> categoryCommandService.restoreAllCategory(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(CategoryDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.SimpleResponse> deleteAllCategoriesPermanent() {
        return telemetryHelper.traceAndMetric("category.deleteAllPermanent", () -> categoryCommandService.deleteAllCategoryPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(CategoryDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all categories: " + throwable.getMessage(), throwable)));
    }
}
