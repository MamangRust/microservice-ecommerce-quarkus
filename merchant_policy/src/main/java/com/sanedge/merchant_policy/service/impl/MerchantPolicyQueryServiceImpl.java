package com.sanedge.merchant_policy.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant_policy.domain.requests.FindAllMerchantRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.merchant_policy.repository.MerchantPolicyQueryRepository;
import com.sanedge.merchant_policy.service.MerchantPolicyQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MerchantPolicyQueryServiceImpl implements MerchantPolicyQueryService {
        private static final Logger logger = LoggerFactory.getLogger(MerchantPolicyQueryServiceImpl.class);

        private final MerchantPolicyQueryRepository merchantPolicyQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public MerchantPolicyQueryServiceImpl(MerchantPolicyQueryRepository merchantPolicyQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.merchantPolicyQueryRepository = merchantPolicyQueryRepository;
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

        private <T> T fromJson(String json, TypeReference<T> typeReference) {
                try {
                        return objectMapper.readValue(json, typeReference);
                } catch (JsonProcessingException e) {
                        logger.error("Error deserializing JSON to object with TypeReference", e);
                        throw new RuntimeException("Failed to deserialize JSON", e);
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

        @Override
        public Uni<ApiResponsePagination<List<MerchantPoliciesResponse>>> findAll(FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantpolicy:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantPoliciesResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantPoliciesResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findAllMerchantPolicies",
                                                        "find_all_policies",
                                                        () -> merchantPolicyQueryRepository.findMerchantPolicies(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantPoliciesResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Merchant policies retrieved successfully",
                                                                                                MerchantPoliciesResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} policy records",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding all policies",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>>> findByActive(
                        FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantpolicy:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findActiveMerchantPolicies",
                                                        "find_active_policies",
                                                        () -> merchantPolicyQueryRepository
                                                                        .findActiveMerchantPolicies(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Active merchant policies retrieved successfully",
                                                                                                MerchantPoliciesResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active policy records",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding active policies",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>>> findByTrashed(
                        FindAllMerchantRequest req) {
                String cacheKey = String.format("merchantpolicy:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findTrashedMerchantPolicies",
                                                        "find_trashed_policies",
                                                        () -> merchantPolicyQueryRepository
                                                                        .findTrashedMerchantPolicies(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<MerchantPoliciesResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Trashed merchant policies retrieved successfully",
                                                                                                MerchantPoliciesResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed policy records",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding trashed policies",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponse<MerchantPoliciesResponse>> findById(Long id) {
                String cacheKey = "merchantpolicy:id:" + id;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                MerchantPoliciesResponse cachedPolicy = fromJson(cachedJson,
                                                                MerchantPoliciesResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Merchant policy retrieved successfully",
                                                                cachedPolicy));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findMerchantPolicyById",
                                                        "find_policy_by_id",
                                                        Attributes.builder().put("policy.id", id).build(),
                                                        () -> merchantPolicyQueryRepository.findById(id)
                                                                        .chain(policy -> {
                                                                                if (policy == null) {
                                                                                        logger.warn("Merchant policy not found with id: {}",
                                                                                                        id);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Merchant policy not found with id="
                                                                                                                        + id);
                                                                                }

                                                                                MerchantPoliciesResponse policyResponse = MerchantPoliciesResponse
                                                                                                .from(policy);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(policyResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached policy for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found merchant policy with id: {} and title: {}",
                                                                                                                        id,
                                                                                                                        policy.getTitle());
                                                                                                        return ApiResponse
                                                                                                                        .success("Merchant policy retrieved successfully",
                                                                                                                                        policyResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding merchant policy by id: {}",
                                                                                        id, e)));
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