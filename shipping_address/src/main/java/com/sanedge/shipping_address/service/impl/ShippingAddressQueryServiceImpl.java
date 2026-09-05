package com.sanedge.shipping_address.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.shipping_address.domain.requests.FindAllShippingAddress;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponseDeleteAt;
import com.sanedge.shipping_address.entity.ShippingAddress;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.shipping_address.repository.ShippingAddressQueryRepository;
import com.sanedge.shipping_address.service.ShippingAddressQueryService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShippingAddressQueryServiceImpl implements ShippingAddressQueryService {
        private static final Logger logger = LoggerFactory.getLogger(ShippingAddressQueryServiceImpl.class);

        ShippingAddressQueryRepository shippingAddressQueryRepository;
        RedisService redisService;
        TracingMetrics tracingMetrics;
        ObjectMapper objectMapper;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public ShippingAddressQueryServiceImpl(ShippingAddressQueryRepository shippingAddressQueryRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics,
                        ObjectMapper objectMapper) {
                this.shippingAddressQueryRepository = shippingAddressQueryRepository;
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
        public Uni<ApiResponsePagination<List<ShippingAddressResponse>>> findAll(FindAllShippingAddress req) {
                String cacheKey = String.format("shipping:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ShippingAddressResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ShippingAddressResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findAllShippingAddresses",
                                                        "find_all_shipping_addresses",
                                                        Attributes.builder()
                                                                        .put("address.page", req.getPage())
                                                                        .put("address.size", req.getPageSize())
                                                                        .build(),
                                                        () -> shippingAddressQueryRepository.findShippingAddresses(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ShippingAddressResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Shipping addresses retrieved successfully",
                                                                                                ShippingAddressResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} shipping addresses",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ShippingAddressResponseDeleteAt>>> findByActive(
                        FindAllShippingAddress req) {
                String cacheKey = String.format("shipping:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ShippingAddressResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ShippingAddressResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findActiveShippingAddresses",
                                                        "find_active_shipping_addresses",
                                                        Attributes.builder()
                                                                        .put("address.page", req.getPage())
                                                                        .put("address.size", req.getPageSize())
                                                                        .build(),
                                                        () -> shippingAddressQueryRepository
                                                                        .findActiveShippingAddresses(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ShippingAddressResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Active shipping addresses retrieved successfully",
                                                                                                ShippingAddressResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active shipping addresses",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<ShippingAddressResponseDeleteAt>>> findByTrashed(
                        FindAllShippingAddress req) {
                String cacheKey = String.format("shipping:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<ShippingAddressResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<ShippingAddressResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTrashedShippingAddresses",
                                                        "find_trashed_shipping_addresses",
                                                        Attributes.builder()
                                                                        .put("address.page", req.getPage())
                                                                        .put("address.size", req.getPageSize())
                                                                        .build(),
                                                        () -> shippingAddressQueryRepository
                                                                        .findTrashedShippingAddresses(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<ShippingAddressResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Trashed shipping addresses retrieved successfully",
                                                                                                ShippingAddressResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed shipping addresses",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<ShippingAddressResponse>> findById(Integer shippingId) {
                String cacheKey = "shipping:id:" + shippingId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ShippingAddressResponse cachedAddress = fromJson(cachedJson,
                                                                ShippingAddressResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Shipping address retrieved successfully",
                                                                cachedAddress));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findShippingAddressById",
                                                        "find_shipping_address_by_id",
                                                        Attributes.builder().put("shipping.id", shippingId.toString())
                                                                        .build(),
                                                        () -> shippingAddressQueryRepository
                                                                        .findByIdNative(shippingId.longValue())
                                                                        .chain(optionalAddress -> {
                                                                                if (optionalAddress.isEmpty()) {
                                                                                        logger.warn("Shipping address not found with ID: {}",
                                                                                                        shippingId);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Shipping address not found with ID: "
                                                                                                                        + shippingId);
                                                                                }

                                                                                ShippingAddress address = optionalAddress
                                                                                                .get();
                                                                                ShippingAddressResponse addressResponse = ShippingAddressResponse
                                                                                                .from(address);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(addressResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached shipping address for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found shipping address with city: {}",
                                                                                                                        address.getKota());
                                                                                                        return ApiResponse
                                                                                                                        .success("Shipping address retrieved successfully",
                                                                                                                                        addressResponse);
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<ShippingAddressResponse>> findByOrder(Integer orderId) {
                String cacheKey = "shipping:order:" + orderId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ShippingAddressResponse cachedAddress = fromJson(cachedJson,
                                                                ShippingAddressResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Shipping address retrieved successfully",
                                                                cachedAddress));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findShippingAddressByOrderId",
                                                        "find_shipping_address_by_order_id",
                                                        Attributes.builder().put("order.id", orderId.toString())
                                                                        .build(),
                                                        () -> shippingAddressQueryRepository.findByOrderId(orderId)
                                                                        .chain(optionalAddress -> {
                                                                                if (optionalAddress.isEmpty()) {
                                                                                        logger.warn("Shipping address not found for order ID: {}",
                                                                                                        orderId);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Shipping address not found for order ID: "
                                                                                                                        + orderId);
                                                                                }

                                                                                ShippingAddress address = optionalAddress
                                                                                                .get();
                                                                                ShippingAddressResponse addressResponse = ShippingAddressResponse
                                                                                                .from(address);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(addressResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached shipping address for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found shipping address with city: {}",
                                                                                                                        address.getKota());
                                                                                                        return ApiResponse
                                                                                                                        .success("Shipping address retrieved successfully",
                                                                                                                                        addressResponse);
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