package com.sanedge.review.service.impl;

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
import com.sanedge.review.domain.requests.FindAllReview;
import com.sanedge.review.domain.requests.FindAllReviewByMerchant;
import com.sanedge.review.domain.requests.FindAllReviewByProduct;
import com.sanedge.review.domain.response.ReviewRelationsDetailResponse;
import com.sanedge.review.domain.response.ReviewResponse;
import com.sanedge.review.domain.response.ReviewResponseDeleteAt;
import com.sanedge.review.entity.Review;
import com.sanedge.review.repository.ReviewQueryRepository;
import com.sanedge.review.service.ReviewQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReviewQueryServiceImpl implements ReviewQueryService {
        private static final Logger logger = LoggerFactory.getLogger(ReviewQueryServiceImpl.class);

        private final ReviewQueryRepository reviewQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public ReviewQueryServiceImpl(ReviewQueryRepository reviewQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.reviewQueryRepository = reviewQueryRepository;
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
        public Uni<ApiResponsePagination<List<ReviewResponse>>> findAll(FindAllReview req) {
                String cacheKey = String.format("review:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ReviewResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ReviewResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findAllReviews", "find_all_reviews",
                                                        () -> reviewQueryRepository.findReviews(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ReviewResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Reviews retrieved successfully",
                                                                                                ReviewResponse::from);

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
                                                                                        "Error finding all reviews",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ReviewResponseDeleteAt>>> findActive(FindAllReview req) {
                String cacheKey = String.format("review:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ReviewResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ReviewResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findActiveReviews",
                                                        "find_active_reviews",
                                                        () -> reviewQueryRepository.findActiveReviews(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ReviewResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Active reviews retrieved successfully",
                                                                                                ReviewResponseDeleteAt::from);

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
                                                                                        "Error finding active reviews",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ReviewResponseDeleteAt>>> findTrashed(FindAllReview req) {
                String cacheKey = String.format("review:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ReviewResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ReviewResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findTrashedReviews",
                                                        "find_trashed_reviews",
                                                        () -> reviewQueryRepository.findTrashedReviews(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ReviewResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Trashed reviews retrieved successfully",
                                                                                                ReviewResponseDeleteAt::from);

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
                                                                                        "Error finding trashed reviews",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ReviewRelationsDetailResponse>>> findByMerchant(
                        FindAllReviewByMerchant req) {
                String cacheKey = String.format("review:merchant:%d:%d:%s:%d:%d",
                                req.getMerchantId(),
                                req.getRating() != null ? req.getRating() : 0,
                                req.getSearch() != null ? req.getSearch() : "",
                                req.getPage(),
                                req.getPageSize());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ReviewRelationsDetailResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ReviewRelationsDetailResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findReviewsByMerchant",
                                                        "find_reviews_by_merchant",
                                                        Attributes.builder()
                                                                        .put("merchant.id", req.getMerchantId() != null
                                                                                        ? req.getMerchantId().toString()
                                                                                        : "null")
                                                                        .build(),
                                                        () -> reviewQueryRepository.findByMerchantId(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ReviewRelationsDetailResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Merchant reviews retrieved successfully",
                                                                                                ReviewRelationsDetailResponse::from);

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
                                                                                        "Error finding reviews for merchant: {}",
                                                                                        req.getMerchantId(), e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ReviewRelationsDetailResponse>>> findByProduct(
                        FindAllReviewByProduct req) {
                String cacheKey = String.format("review:product:%d:%d:%s:%d:%d",
                                req.getProductId(),
                                req.getRating() != null ? req.getRating() : 0,
                                req.getSearch() != null ? req.getSearch() : "",
                                req.getPage(),
                                req.getPageSize());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ReviewRelationsDetailResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ReviewRelationsDetailResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findReviewsByProduct",
                                                        "find_reviews_by_product",
                                                        Attributes.builder()
                                                                        .put("product.id", req.getProductId() != null
                                                                                        ? req.getProductId().toString()
                                                                                        : "null")
                                                                        .build(),
                                                        () -> reviewQueryRepository.findByProductId(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ReviewRelationsDetailResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Product reviews retrieved successfully",
                                                                                                ReviewRelationsDetailResponse::from);

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
                                                                                        "Error finding reviews for product: {}",
                                                                                        req.getProductId(), e)));
                                });
        }

        @Override
        public Uni<ApiResponse<ReviewResponse>> findById(Integer reviewId) {
                String cacheKey = "review:id:" + reviewId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ReviewResponse cachedReview = fromJson(cachedJson,
                                                                ReviewResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Review retrieved successfully", cachedReview));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findReviewById", "find_review_by_id",
                                                        Attributes.builder().put("review.id", reviewId.toString())
                                                                        .build(),
                                                        () -> reviewQueryRepository.findReviewById(reviewId.longValue())
                                                                        .chain(optionalReview -> {
                                                                                if (optionalReview.isEmpty()) {
                                                                                        logger.warn("Review not found with ID: {}",
                                                                                                        reviewId);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Review not found with ID: "
                                                                                                                        + reviewId);
                                                                                }

                                                                                Review review = optionalReview.get();
                                                                                ReviewResponse reviewResponse = ReviewResponse
                                                                                                .from(review);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(reviewResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached review for key: {}",
                                                                                                                        cacheKey);
                                                                                                        return ApiResponse
                                                                                                                        .success("Review retrieved successfully",
                                                                                                                                        reviewResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding review by ID: {}",
                                                                                        reviewId, e)));
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