package com.sanedge.product.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.product.domain.requests.FindAllProductByCategoryRequest;
import com.sanedge.product.domain.requests.FindAllProductByMerchantRequest;
import com.sanedge.product.domain.requests.FindAllProductRequest;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;
import com.sanedge.product.entity.Product;
import com.sanedge.product.repository.ProductQueryRepository;
import com.sanedge.product.service.ProductQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProductQueryServiceImpl implements ProductQueryService {
        private static final Logger logger = LoggerFactory.getLogger(ProductQueryServiceImpl.class);

        private final ProductQueryRepository productQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public ProductQueryServiceImpl(ProductQueryRepository productQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.productQueryRepository = productQueryRepository;
                this.redisService = redisService;
                this.objectMapper = objectMapper;
                this.tracingMetrics = tracingMetrics;
        }

        private String toJson(Object obj) {
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                        logger.error("Error serializing object to JSON", e);
                        throw new RuntimeException("Failed to serialize object", e);
                }
        }

        private <T> T fromJson(String json, Class<T> clazz) {
                try {
                        return objectMapper.readValue(json, clazz);
                } catch (JsonProcessingException e) {
                        logger.error("Error deserializing JSON to object", e);
                        throw new RuntimeException("Failed to deserialize JSON", e);
                }
        }

        private <T> T fromJson(String json, TypeReference<T> typeReference) {
                try {
                        return objectMapper.readValue(json, typeReference);
                } catch (JsonProcessingException e) {
                        logger.error("Error deserializing JSON to object with TypeReference", e);
                        throw new RuntimeException("Failed to deserialize JSON", e);
                }
        }

        @Override
        public Uni<ApiResponsePagination<List<ProductResponse>>> findAll(FindAllProductRequest req) {
                String cacheKey = String.format("product:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ProductResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ProductResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findAllProducts", "find_all_products",
                                                        () -> productQueryRepository.findProducts(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ProductResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Products retrieved successfully",
                                                                                                ProductResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding all products",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ProductResponseDeleteAt>>> findActiveProducts(FindAllProductRequest req) {
                String cacheKey = String.format("product:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ProductResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ProductResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findActiveProducts",
                                                        "find_active_products",
                                                        () -> productQueryRepository.findActiveProducts(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ProductResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Active products retrieved successfully",
                                                                                                ProductResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding active products",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ProductResponseDeleteAt>>> findTrashedProducts(
                        FindAllProductRequest req) {
                String cacheKey = String.format("product:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ProductResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ProductResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findTrashedProducts",
                                                        "find_trashed_products",
                                                        () -> productQueryRepository.findTrashedProducts(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ProductResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Trashed products retrieved successfully",
                                                                                                ProductResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding trashed products",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ProductResponse>>> findByMerchant(FindAllProductByMerchantRequest req) {
                String cacheKey = String.format("product:merchant:%d:%d:%d:%d:%d:%d:%s",
                                req.getMerchantId(),
                                req.getCategoryId() != null ? req.getCategoryId() : 0,
                                req.getMinPrice() != null ? req.getMinPrice() : 0,
                                req.getMaxPrice() != null ? req.getMaxPrice() : 0,
                                req.getPage(),
                                req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ProductResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ProductResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findProductsByMerchant",
                                                        "find_products_by_merchant",
                                                        Attributes.builder()
                                                                        .put("merchant.id", req.getMerchantId() != null
                                                                                        ? req.getMerchantId().toString()
                                                                                        : "null")
                                                                        .build(),
                                                        () -> productQueryRepository.findProductsByMerchantNative(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ProductResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Products by merchant retrieved successfully",
                                                                                                ProductResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding products for merchant: {}",
                                                                                        req.getMerchantId(), e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ProductResponse>>> findByCategoryName(
                        FindAllProductByCategoryRequest req) {
                String cacheKey = String.format("product:categoryName:%s:%d:%d:%d:%d:%s",
                                req.getCategoryName() != null ? req.getCategoryName() : "",
                                req.getMinPrice() != null ? req.getMinPrice() : 0,
                                req.getMaxPrice() != null ? req.getMaxPrice() : 0,
                                req.getPage(),
                                req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ProductResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ProductResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findProductsByCategoryName",
                                                        "find_products_by_category_name",
                                                        Attributes.builder().put("category.name", req.getCategoryName())
                                                                        .build(),
                                                        () -> productQueryRepository
                                                                        .findProductsByCategoryNameNative(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ProductResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Products by category retrieved successfully",
                                                                                                ProductResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding products for category: {}",
                                                                                        req.getCategoryName(), e)));
                                });
        }

        @Override
        public Uni<ApiResponse<ProductResponse>> findById(Long productId) {
                String cacheKey = "product:id:" + productId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ProductResponse cachedProduct = fromJson(cachedJson,
                                                                ProductResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Product retrieved successfully", cachedProduct));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findProductById", "find_product_by_id",
                                                        Attributes.builder().put("product.id", productId.toString())
                                                                        .build(),
                                                        () -> productQueryRepository.findProductById(productId)
                                                                        .chain(optionalProduct -> {
                                                                                if (optionalProduct.isEmpty()) {
                                                                                        logger.warn("Product not found with ID: {}",
                                                                                                        productId);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Product not found with ID: "
                                                                                                                        + productId);
                                                                                }

                                                                                Product product = optionalProduct.get();
                                                                                ProductResponse productResponse = ProductResponse
                                                                                                .from(product);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(productResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached product for key: {}",
                                                                                                                        cacheKey);
                                                                                                        return ApiResponse
                                                                                                                        .success("Product retrieved successfully",
                                                                                                                                        productResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding product by ID: {}",
                                                                                        productId, e)));
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        int pageParam,
                        int sizeParam,
                        String successMessage,
                        Function<T, R> mapper) {

                List<R> data = pagedResult.getData().stream()
                                .map(mapper)
                                .collect(Collectors.toList());

                int totalRecords = pagedResult.getTotalRecords();
                int size = sizeParam > 0 ? sizeParam : 1;
                int totalPages = (int) Math.ceil((double) totalRecords / size);

                PaginationMeta pagination = new PaginationMeta(pageParam, size, totalPages, totalRecords);

                return new ApiResponsePagination<>("success", successMessage, data, pagination);
        }
}