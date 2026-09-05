package com.sanedge.merchant_business.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.merchant_business.domain.requests.FindAllMerchantRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;
import com.sanedge.merchant_business.repository.MerchantBusinessQueryRepository;
import com.sanedge.merchant_business.service.MerchantBusinessQueryService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class MerchantBusinessQueryServiceImpl implements MerchantBusinessQueryService {
        private static final Logger logger = LoggerFactory.getLogger(MerchantBusinessQueryServiceImpl.class);

        private final MerchantBusinessQueryRepository merchantBusinessQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public MerchantBusinessQueryServiceImpl(MerchantBusinessQueryRepository merchantBusinessQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.merchantBusinessQueryRepository = merchantBusinessQueryRepository;
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
        public Uni<ApiResponsePagination<List<MerchantBusinessResponse>>> findAll(FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantbusiness:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("business.page", req.getPage())
                                .put("business.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantBusinessResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantBusinessResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findAllMerchantBusiness",
                                                        "find_all_business_info", attrs,
                                                        () -> merchantBusinessQueryRepository
                                                                        .findMerchantBusinessInformation(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantBusinessResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Merchant business information retrieved successfully",
                                                                                                MerchantBusinessResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} business records",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>>> findByActive(
                        FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantbusiness:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("business.page", req.getPage())
                                .put("business.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findActiveMerchantBusiness",
                                                        "find_active_business_info", attrs,
                                                        () -> merchantBusinessQueryRepository
                                                                        .findActiveMerchantBusinessInformation(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Active merchant business information retrieved successfully",
                                                                                                MerchantBusinessResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active business records",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>>> findByTrashed(
                        FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantbusiness:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("business.page", req.getPage())
                                .put("business.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTrashedMerchantBusiness",
                                                        "find_trashed_business_info", attrs,
                                                        () -> merchantBusinessQueryRepository
                                                                        .findTrashedMerchantBusinessInformation(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Trashed merchant business information retrieved successfully",
                                                                                                MerchantBusinessResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed business records",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<MerchantBusinessResponse>> findById(Long merchantBusinessInfoId) {
                String cacheKey = "merchantbusiness:id:" + merchantBusinessInfoId;

                Attributes attrs = Attributes.builder()
                                .put("business.id", merchantBusinessInfoId)
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                MerchantBusinessResponse cachedInfo = fromJson(cachedJson,
                                                                MerchantBusinessResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Merchant business info retrieved successfully",
                                                                cachedInfo));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findMerchantBusinessById",
                                                        "find_business_info_by_id", attrs,
                                                        () -> merchantBusinessQueryRepository
                                                                        .findMerchantBusinessInformationById(
                                                                                        merchantBusinessInfoId)
                                                                        .chain(business -> {
                                                                                if (business == null) {
                                                                                        logger.warn("Merchant business info not found with id: {}",
                                                                                                        merchantBusinessInfoId);
                                                                                        throw new NotFoundException(
                                                                                                        "Merchant business info not found with id: "
                                                                                                                        + merchantBusinessInfoId);
                                                                                }

                                                                                MerchantBusinessResponse businessResponse = MerchantBusinessResponse
                                                                                                .from(business);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(businessResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached business info for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found merchant business info with id: {} and taxId: {}",
                                                                                                                        merchantBusinessInfoId,
                                                                                                                        business.getTaxId());
                                                                                                        return ApiResponse
                                                                                                                        .success("Merchant business info retrieved successfully",
                                                                                                                                        businessResponse);
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