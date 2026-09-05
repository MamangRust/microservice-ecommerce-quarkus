package com.sanedge.slider.service.impl;

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
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.slider.domain.requests.FindAllSliderRequest;
import com.sanedge.slider.domain.response.SliderResponse;
import com.sanedge.slider.domain.response.SliderResponseDeleteAt;
import com.sanedge.slider.repository.SliderQueryRepository;
import com.sanedge.slider.service.SliderQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SliderQueryServiceImpl implements SliderQueryService {
        private static final Logger logger = LoggerFactory.getLogger(SliderQueryServiceImpl.class);

        SliderQueryRepository sliderQueryRepository;
        RedisService redisService;
        TracingMetrics tracingMetrics;
        ObjectMapper objectMapper;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public SliderQueryServiceImpl(SliderQueryRepository sliderQueryRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics,
                        ObjectMapper objectMapper) {
                this.sliderQueryRepository = sliderQueryRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
                this.objectMapper = objectMapper;
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
        public Uni<ApiResponsePagination<List<SliderResponse>>> findAll(FindAllSliderRequest req) {
                String cacheKey = String.format("slider:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<SliderResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<SliderResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findAllSliders", "find_all_sliders",
                                                        Attributes.builder()
                                                                        .put("slider.page", req.getPage())
                                                                        .put("slider.size", req.getPageSize())
                                                                        .build(),
                                                        () -> sliderQueryRepository.findSliders(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<SliderResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Sliders retrieved successfully",
                                                                                                SliderResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} sliders",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<SliderResponseDeleteAt>>> findByActive(FindAllSliderRequest req) {
                String cacheKey = String.format("slider:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<SliderResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<SliderResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findActiveSliders",
                                                        "find_active_sliders",
                                                        Attributes.builder()
                                                                        .put("slider.page", req.getPage())
                                                                        .put("slider.size", req.getPageSize())
                                                                        .build(),
                                                        () -> sliderQueryRepository.findActiveSliders(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<SliderResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Active sliders retrieved successfully",
                                                                                                SliderResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active sliders",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<SliderResponseDeleteAt>>> findByTrashed(FindAllSliderRequest req) {
                String cacheKey = String.format("slider:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<SliderResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<SliderResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTrashedSliders",
                                                        "find_trashed_sliders",
                                                        Attributes.builder()
                                                                        .put("slider.page", req.getPage())
                                                                        .put("slider.size", req.getPageSize())
                                                                        .build(),
                                                        () -> sliderQueryRepository.findTrashedSliders(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<SliderResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Trashed sliders retrieved successfully",
                                                                                                SliderResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed sliders",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<SliderResponse>> findById(Integer id) {
                String cacheKey = "slider:" + id;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                SliderResponse cachedSlider = fromJson(cachedJson,
                                                                SliderResponse.class);
                                                return Uni.createFrom().item(
                                                                ApiResponse.success("Slider found", cachedSlider));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findSliderById", "find_slider_by_id",
                                                        Attributes.builder().put("slider.id", id.toString()).build(),
                                                        () -> sliderQueryRepository.findById(id.longValue())
                                                                        .chain(slider -> {
                                                                                if (slider == null) {
                                                                                        logger.warn("Slider not found with id: {}",
                                                                                                        id);
                                                                                        throw new com.sanedge.common.exception.ResourceNotFoundException(
                                                                                                        "Slider not found with id: "
                                                                                                                        + id);
                                                                                }

                                                                                SliderResponse sliderResponse = SliderResponse
                                                                                                .from(slider);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(sliderResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached slider for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found slider with id: {} and name: {}",
                                                                                                                        id,
                                                                                                                        slider.getName());
                                                                                                        return ApiResponse
                                                                                                                        .success("Slider found",
                                                                                                                                        sliderResponse);
                                                                                                });
                                                                        }));
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