package com.sanedge.merchant_detail.service.impl;

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
import com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponseDeleteAt;
import com.sanedge.merchant_detail.entity.MerchantDetailsRelation;
import com.sanedge.merchant_detail.repository.MerchantDetailQueryRepository;
import com.sanedge.merchant_detail.service.MerchantDetailQueryService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MerchantDetailQueryServiceImpl implements MerchantDetailQueryService {
        private static final Logger logger = LoggerFactory.getLogger(MerchantDetailQueryServiceImpl.class);

        private final MerchantDetailQueryRepository merchantDetailQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public MerchantDetailQueryServiceImpl(MerchantDetailQueryRepository merchantDetailQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.merchantDetailQueryRepository = merchantDetailQueryRepository;
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
        public Uni<ApiResponsePagination<List<MerchantDetailRelationResponse>>> findAll(FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantdetail:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("detail.page", req.getPage())
                                .put("detail.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantDetailRelationResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantDetailRelationResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findAllMerchantDetails",
                                                        "find_all_details", attrs,
                                                        () -> merchantDetailQueryRepository.findAllWithSocialLinks(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantDetailRelationResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Merchant details retrieved successfully",
                                                                                                MerchantDetailRelationResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} merchant details",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>>> findByActive(
                        FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantdetail:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("detail.page", req.getPage())
                                .put("detail.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findActiveMerchantDetails",
                                                        "find_active_details", attrs,
                                                        () -> merchantDetailQueryRepository
                                                                        .findActiveWithSocialLinks(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Active merchant details retrieved successfully",
                                                                                                MerchantDetailRelationResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active merchant details",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>>> findByTrashed(
                        FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantdetail:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("detail.page", req.getPage())
                                .put("detail.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTrashedMerchantDetails",
                                                        "find_trashed_details", attrs,
                                                        () -> merchantDetailQueryRepository
                                                                        .findTrashedWithSocialLinks(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Trashed merchant details retrieved successfully",
                                                                                                MerchantDetailRelationResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed merchant details",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<MerchantDetailRelationResponse>> findById(Long merchantID) {
                String cacheKey = "merchantdetail:id:" + merchantID;

                Attributes attrs = Attributes.builder()
                                .put("merchant.id", merchantID)
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                MerchantDetailRelationResponse cachedDetail = fromJson(cachedJson,
                                                                MerchantDetailRelationResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Merchant Detail retrieved successfully",
                                                                cachedDetail));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findMerchantDetailById",
                                                        "find_detail_by_id", attrs,
                                                        () -> merchantDetailQueryRepository
                                                                        .findByIdWithSocialLinks(merchantID)
                                                                        .chain(optionalDetail -> {
                                                                                if (optionalDetail.isEmpty()) {
                                                                                        logger.warn("Merchant detail not found with ID: {}",
                                                                                                        merchantID);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Merchant detail not found with ID: "
                                                                                                                        + merchantID);
                                                                                }

                                                                                MerchantDetailsRelation relation = optionalDetail
                                                                                                .get();
                                                                                MerchantDetailRelationResponse detailResponse = MerchantDetailRelationResponse
                                                                                                .from(relation);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(detailResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached detail for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found merchant detail with ID: {} and displayName: {}",
                                                                                                                        merchantID,
                                                                                                                        relation.getDisplayName());
                                                                                                        return ApiResponse
                                                                                                                        .success("Merchant Detail retrieved successfully",
                                                                                                                                        detailResponse);
                                                                                                });
                                                                        }));
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        FindAllMerchantRequest request,
                        String successMessage,
                        Function<T, R> mapper) {

                List<R> data = pagedResult.getData().stream()
                                .map(mapper)
                                .collect(Collectors.toList());

                int totalRecords = pagedResult.getTotalRecords();
                int size = request.getPageSize() > 0 ? request.getPageSize() : 1;
                int totalPages = (int) Math.ceil((double) totalRecords / size);

                PaginationMeta pagination = new PaginationMeta(request.getPage(), size, totalPages, totalRecords);

                return new ApiResponsePagination<>("success", successMessage, data, pagination);
        }
}