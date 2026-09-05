package com.sanedge.banner.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.banner.domain.requests.FindAllBannerRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;
import com.sanedge.banner.repository.BannerQueryRepository;
import com.sanedge.banner.service.BannerQueryService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class BannerQueryServiceImpl implements BannerQueryService {
        private static final Logger logger = LoggerFactory.getLogger(BannerQueryServiceImpl.class);

        private final BannerQueryRepository bannerQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public BannerQueryServiceImpl(BannerQueryRepository bannerQueryRepository, RedisService redisService,
                        ObjectMapper objectMapper, TracingMetrics tracingMetrics) {
                this.bannerQueryRepository = bannerQueryRepository;
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
        public Uni<ApiResponsePagination<List<BannerResponse>>> findAllPaginated(FindAllBannerRequest request) {
                String cacheKey = String.format("banners:all:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());

                Attributes attrs = Attributes.builder()
                                .put("banner.page", request.getPage())
                                .put("banner.size", request.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<BannerResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<BannerResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findAllBanners", "find_all_banners",
                                                        attrs, () -> bannerQueryRepository.findBanners(request)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<BannerResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, request,
                                                                                                "Banners retrieved successfully",
                                                                                                BannerResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} banners",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<BannerResponseDeleteAt>>> findActivePaginated(
                        FindAllBannerRequest request) {
                String cacheKey = String.format("banners:active:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());

                Attributes attrs = Attributes.builder()
                                .put("banner.page", request.getPage())
                                .put("banner.size", request.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<BannerResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<BannerResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findActiveBanners",
                                                        "find_active_banners", attrs,
                                                        () -> bannerQueryRepository.findActiveBanners(request)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<BannerResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, request,
                                                                                                "Active banners retrieved successfully",
                                                                                                BannerResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active banners",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<BannerResponseDeleteAt>>> findTrashedPaginated(
                        FindAllBannerRequest request) {
                String cacheKey = String.format("banners:trashed:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());

                Attributes attrs = Attributes.builder()
                                .put("banner.page", request.getPage())
                                .put("banner.size", request.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<BannerResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<BannerResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTrashedBanners",
                                                        "find_trashed_banners", attrs,
                                                        () -> bannerQueryRepository.findTrashedBanners(request)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<BannerResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, request,
                                                                                                "Trashed banners retrieved successfully",
                                                                                                BannerResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed banners",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<BannerResponse>> findById(Long id) {
                String cacheKey = "banner:" + id;

                Attributes attrs = Attributes.builder()
                                .put("banner.id", id)
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                BannerResponse cachedBanner = fromJson(cachedJson,
                                                                BannerResponse.class);
                                                return Uni.createFrom().item(
                                                                ApiResponse.success("Banner found", cachedBanner));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findBannerById", "find_banner_by_id",
                                                        attrs, () -> bannerQueryRepository.findById(id)
                                                                        .chain(banner -> {
                                                                                if (banner == null) {
                                                                                        logger.warn("Banner not found with id: {}",
                                                                                                        id);
                                                                                        throw new NotFoundException(
                                                                                                        "Banner not found with id: "
                                                                                                                        + id);
                                                                                }

                                                                                BannerResponse bannerResponse = BannerResponse
                                                                                                .from(banner);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(bannerResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached banner for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found banner with id: {} and name: {}",
                                                                                                                        id,
                                                                                                                        banner.getName());
                                                                                                        return ApiResponse
                                                                                                                        .success("Banner found",
                                                                                                                                        bannerResponse);
                                                                                                });
                                                                        }));
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        FindAllBannerRequest request,
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