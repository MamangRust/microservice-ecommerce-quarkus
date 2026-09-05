package com.sanedge.merchant_award.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.merchant_award.domain.requests.FindAllMerchantRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponseDeleteAt;
import com.sanedge.merchant_award.repository.MerchantAwardQueryRepository;
import com.sanedge.merchant_award.service.MerchantAwardQueryService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class MerchantAwardQueryServiceImpl implements MerchantAwardQueryService {
        private static final Logger logger = LoggerFactory.getLogger(MerchantAwardQueryServiceImpl.class);

        private final MerchantAwardQueryRepository merchantAwardQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public MerchantAwardQueryServiceImpl(MerchantAwardQueryRepository merchantAwardQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.merchantAwardQueryRepository = merchantAwardQueryRepository;
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
        public Uni<ApiResponsePagination<List<MerchantAwardResponse>>> findAll(FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantawards:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("award.page", req.getPage())
                                .put("award.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantAwardResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantAwardResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findAllMerchantAwards",
                                                        "find_all_awards", attrs,
                                                        () -> merchantAwardQueryRepository.findMerchantAwards(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantAwardResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Merchant awards retrieved successfully",
                                                                                                MerchantAwardResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} merchant awards",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<MerchantAwardResponseDeleteAt>>> findByActive(
                        FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantawards:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("award.page", req.getPage())
                                .put("award.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantAwardResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantAwardResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findActiveMerchantAwards",
                                                        "find_active_awards", attrs,
                                                        () -> merchantAwardQueryRepository.findActiveMerchantAwards(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantAwardResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Active merchant awards retrieved successfully",
                                                                                                MerchantAwardResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active merchant awards",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<MerchantAwardResponseDeleteAt>>> findByTrashed(
                        FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantawards:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("award.page", req.getPage())
                                .put("award.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantAwardResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantAwardResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTrashedMerchantAwards",
                                                        "find_trashed_awards", attrs,
                                                        () -> merchantAwardQueryRepository
                                                                        .findTrashedMerchantAwards(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantAwardResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Trashed merchant awards retrieved successfully",
                                                                                                MerchantAwardResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed merchant awards",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<MerchantAwardResponse>> findById(Long merchantAwardId) {
                String cacheKey = "merchantawards:id:" + merchantAwardId;

                Attributes attrs = Attributes.builder()
                                .put("award.id", merchantAwardId)
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                MerchantAwardResponse cachedAward = fromJson(cachedJson,
                                                                MerchantAwardResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Merchant award retrieved successfully", cachedAward));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findMerchantAwardById",
                                                        "find_award_by_id", attrs,
                                                        () -> merchantAwardQueryRepository
                                                                        .findMerchantAwardById(merchantAwardId)
                                                                        .chain(award -> {
                                                                                if (award == null) {
                                                                                        logger.warn("Merchant award not found with id: {}",
                                                                                                        merchantAwardId);
                                                                                        throw new NotFoundException(
                                                                                                        "Merchant award not found with id: "
                                                                                                                        + merchantAwardId);
                                                                                }

                                                                                MerchantAwardResponse awardResponse = MerchantAwardResponse
                                                                                                .from(award);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(awardResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached award for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found merchant award with id: {} and title: {}",
                                                                                                                        merchantAwardId,
                                                                                                                        award.getTitle());
                                                                                                        return ApiResponse
                                                                                                                        .success("Merchant award retrieved successfully",
                                                                                                                                        awardResponse);
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