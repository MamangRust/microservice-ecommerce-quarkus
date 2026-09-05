package com.sanedge.category.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.category.domain.requests.CreateCategoryRequest;
import com.sanedge.category.domain.requests.UpdateCategoryRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.entity.Category;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.category.repository.CategoryCommandRepository;
import com.sanedge.category.repository.CategoryQueryRepository;
import com.sanedge.category.service.CategoryCommandService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CategoryCommandServiceImpl implements CategoryCommandService {
        private static final Logger logger = LoggerFactory.getLogger(CategoryCommandServiceImpl.class);

        private final CategoryCommandRepository categoryCommandRepository;
        private final CategoryQueryRepository categoryQueryRepository;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;

        @Inject
        public CategoryCommandServiceImpl(CategoryCommandRepository categoryCommandRepository,
                        CategoryQueryRepository categoryQueryRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics) {
                this.categoryCommandRepository = categoryCommandRepository;
                this.categoryQueryRepository = categoryQueryRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CategoryResponse>> createCategory(CreateCategoryRequest request) {
                Attributes attrs = Attributes.builder()
                                .put("category.name", request.getName())
                                .build();

                logger.info("Creating new category with name: {}", request.getName());

                return tracingMetrics.traceAndMeasure("createCategory", "create_category", attrs, () -> {
                        Category category = new Category();
                        category.setName(request.getName());
                        category.setDescription(request.getDescription());
                        category.setSlugCategory(request.getSlugCategory());
                        category.setImageCategory(request.getImageCategory());
                        category.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        category.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                        return categoryCommandRepository.persist(category)
                                        .map(v -> {
                                                CategoryResponse categoryResponse = CategoryResponse.from(category);
                                                logger.info("Successfully created category with id: {} and name: {}",
                                                                category.id, category.getName());
                                                return ApiResponse.success("Category created successfully",
                                                                categoryResponse);
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CategoryResponse>> updateCategory(UpdateCategoryRequest request) {
                if (request.getCategoryId() == null) {
                        throw new ResourceNotFoundException("category_id is required");
                }

                Attributes attrs = Attributes.builder()
                                .put("category.id", request.getCategoryId())
                                .build();

                logger.info("Updating category with id: {}", request.getCategoryId());

                return tracingMetrics.traceAndMeasure("updateCategory", "update_category", attrs,
                                () -> categoryQueryRepository.findById(request.getCategoryId().longValue())
                                                .chain(existingCategory -> {
                                                        if (existingCategory == null) {
                                                                logger.warn("Category update failed - category not found with id: {}",
                                                                                request.getCategoryId());
                                                                throw new ResourceNotFoundException(
                                                                                "Category not found");
                                                        }

                                                        existingCategory.setName(request.getName());
                                                        existingCategory.setDescription(request.getDescription());
                                                        existingCategory.setSlugCategory(request.getSlugCategory());
                                                        if (request.getImageCategory() != null) {
                                                                existingCategory.setImageCategory(
                                                                                request.getImageCategory());
                                                        }
                                                        existingCategory.setUpdatedAt(java.sql.Timestamp
                                                                        .valueOf(java.time.LocalDateTime.now()));

                                                        return categoryCommandRepository.persist(existingCategory)
                                                                        .chain(v -> {
                                                                                CategoryResponse categoryResponse = CategoryResponse
                                                                                                .from(existingCategory);
                                                                                String cacheKey = "categories:id:"
                                                                                                + request.getCategoryId();

                                                                                return redisService.deleteReactive(
                                                                                                cacheKey)
                                                                                                .map(v2 -> {
                                                                                                        logger.info("Invalidated cache for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully updated category with id: {}",
                                                                                                                        request.getCategoryId());
                                                                                                        return ApiResponse
                                                                                                                        .success("Category updated successfully",
                                                                                                                                        categoryResponse);
                                                                                                });
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CategoryResponseDeleteAt>> trashedCategory(Long categoryId) {
                Attributes attrs = Attributes.builder()
                                .put("category.id", categoryId)
                                .build();

                logger.info("Trashing category with id: {}", categoryId);

                return tracingMetrics.traceAndMeasure("trashCategory", "trash_category", attrs,
                                () -> categoryCommandRepository.trash(categoryId)
                                                .chain(trashedCategory -> {
                                                        if (trashedCategory == null) {
                                                                logger.warn("Category trash failed - category not found with id: {}",
                                                                                categoryId);
                                                                throw new ResourceNotFoundException(
                                                                                "Trashed category not found with id: "
                                                                                                + categoryId);
                                                        }

                                                        CategoryResponseDeleteAt response = CategoryResponseDeleteAt
                                                                        .from(trashedCategory);
                                                        String cacheKey = "categories:id:" + categoryId;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Invalidated cache for key: {}",
                                                                                                cacheKey);
                                                                                logger.info("Successfully trashed category with id: {}",
                                                                                                categoryId);
                                                                                return ApiResponse.success(
                                                                                                "Category trashed successfully",
                                                                                                response);
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CategoryResponseDeleteAt>> restoreCategory(Long categoryId) {
                Attributes attrs = Attributes.builder()
                                .put("category.id", categoryId)
                                .build();

                logger.info("Restoring category with id: {}", categoryId);

                return tracingMetrics.traceAndMeasure("restoreCategory", "restore_category", attrs,
                                () -> categoryCommandRepository.restore(categoryId)
                                                .chain(restoredCategory -> {
                                                        if (restoredCategory == null) {
                                                                logger.warn("Category restore failed - category not found with id: {}",
                                                                                categoryId);
                                                                throw new ResourceNotFoundException(
                                                                                "Restore category not found with id: "
                                                                                                + categoryId);
                                                        }

                                                        CategoryResponseDeleteAt response = CategoryResponseDeleteAt
                                                                        .from(restoredCategory);
                                                        String cacheKey = "categories:id:" + categoryId;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Invalidated cache for key: {}",
                                                                                                cacheKey);
                                                                                logger.info("Successfully restored category with id: {}",
                                                                                                categoryId);
                                                                                return ApiResponse.success(
                                                                                                "Category restored successfully",
                                                                                                response);
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteCategoryPermanent(Long categoryId) {
                Attributes attrs = Attributes.builder()
                                .put("category.id", categoryId)
                                .build();

                logger.info("Permanently deleting category with id: {}", categoryId);

                return tracingMetrics.traceAndMeasure("deleteCategoryPermanent", "delete_category_permanent", attrs,
                                () -> categoryCommandRepository.deletePermanent(categoryId)
                                                .chain(deletedCategory -> {
                                                        if (deletedCategory == null) {
                                                                logger.warn("Permanent delete failed - category not found or must be trashed before permanent deletion with id: {}",
                                                                                categoryId);
                                                                throw new InvalidRequestException(
                                                                                "Category not found or must be trashed before permanent deletion");
                                                        }

                                                        String cacheKey = "categories:id:" + categoryId;
                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v2 -> {
                                                                                logger.info("Invalidated cache for key: {}",
                                                                                                cacheKey);
                                                                                logger.info("Successfully permanently deleted category with id: {}",
                                                                                                categoryId);
                                                                                return ApiResponse.<Void>success("Category deleted permanently");
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> restoreAllCategories() {
                logger.info("Restoring all trashed categories");

                return tracingMetrics.traceAndMeasure("restoreAllCategories", "restore_all_categories",
                                () -> categoryCommandRepository.restoreAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException("No trashed categories found");
                                                        }
                                                        logger.info("Successfully restored all trashed categories");
                                                        return ApiResponse.<Void>success(
                                                                        "All categories restored successfully");
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteAllCategoriesPermanent() {
                logger.info("Permanently deleting all trashed categories");

                return tracingMetrics.traceAndMeasure("deleteAllCategoriesPermanent", "delete_all_categories_permanent",
                                () -> categoryCommandRepository.deleteAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException("No trashed categories found");
                                                        }
                                                        logger.info("Successfully permanently deleted all trashed categories");
                                                        return ApiResponse.<Void>success(
                                                                        "All categories permanently deleted");
                                                }));
        }
}