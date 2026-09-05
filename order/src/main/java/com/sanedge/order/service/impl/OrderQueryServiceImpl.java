package com.sanedge.order.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.cache.CacheKeys;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.order.domain.requests.FindAllOrderByMerchantRequest;
import com.sanedge.order.domain.requests.FindAllOrderRequest;
import com.sanedge.order.domain.response.OrderItemResponse;
import com.sanedge.order.domain.response.OrderRelationResponse;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.entity.Order;
import com.sanedge.order.repository.OrderQueryRepository;
import com.sanedge.order.service.OrderQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderQueryServiceImpl implements OrderQueryService {
        private static final Logger logger = LoggerFactory.getLogger(OrderQueryServiceImpl.class);

        private final OrderQueryRepository orderQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        @Inject
        @io.quarkus.grpc.GrpcClient("order_item")
        pb.order_item.OrderItemQueryService orderItemQueryService;

        private static final long LIST_CACHE_TTL_SECONDS = CacheKeys.TTL_DEFAULT_SECONDS;

        @Inject
        public OrderQueryServiceImpl(OrderQueryRepository orderQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.orderQueryRepository = orderQueryRepository;
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
        public Uni<ApiResponsePagination<List<OrderResponse>>> findAll(FindAllOrderRequest req) {
                String cacheKey = CacheKeys.forList("order", "all", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<OrderResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<OrderResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findAllOrders", "find_all_orders",
                                                        () -> orderQueryRepository.findOrders(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<OrderResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Orders retrieved successfully",
                                                                                                OrderResponse::from);

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
                                                                                        "Error finding all orders",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<OrderResponseDeleteAt>>> findByActive(FindAllOrderRequest req) {
                String cacheKey = CacheKeys.forList("order", "active", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<OrderResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<OrderResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findActiveOrders", "find_active_orders",
                                                        () -> orderQueryRepository.findActiveOrders(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<OrderResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Active orders retrieved successfully",
                                                                                                OrderResponseDeleteAt::from);

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
                                                                                        "Error finding active orders",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<OrderResponseDeleteAt>>> findByTrashed(FindAllOrderRequest req) {
                String cacheKey = CacheKeys.forList("order", "trashed", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<OrderResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<OrderResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findTrashedOrders",
                                                        "find_trashed_orders",
                                                        () -> orderQueryRepository.findTrashedOrders(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<OrderResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Trashed orders retrieved successfully",
                                                                                                OrderResponseDeleteAt::from);

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
                                                                                        "Error finding trashed orders",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<OrderResponse>>> findByMerchantId(FindAllOrderByMerchantRequest req) {
                String cacheKey = CacheKeys.forList("order", "merchant", req.getMerchantId(), req.getPage(),
                                req.getPageSize(), req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<OrderResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<OrderResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findOrdersByMerchant",
                                                        "find_orders_by_merchant",
                                                        Attributes.builder()
                                                                        .put("merchant.id", req.getMerchantId() != null
                                                                                        ? req.getMerchantId().toString()
                                                                                        : "null")
                                                                        .build(),
                                                        () -> orderQueryRepository.findOrdersByMerchant(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<OrderResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Orders retrieved successfully",
                                                                                                OrderResponse::from);

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
                                                                                        "Error finding merchant orders",
                                                                                        e)));
                                });
        }

        @Override
        public Uni<ApiResponse<OrderResponse>> findById(Long id) {
                String cacheKey = CacheKeys.forEntity("order", id);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                OrderResponse cachedOrder = fromJson(cachedJson, OrderResponse.class);
                                                return Uni.createFrom().item(ApiResponse
                                                                .success("Order retrieved successfully", cachedOrder));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findOrderById", "find_order_by_id",
                                                        Attributes.builder().put("order.id", id.toString()).build(),
                                                        () -> orderQueryRepository.findOrderById(id)
                                                                        .chain(optionalOrder -> {
                                                                                if (optionalOrder.isEmpty()) {
                                                                                        logger.warn("Order not found with ID: {}",
                                                                                                        id);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Order not found with ID: "
                                                                                                                        + id);
                                                                                }

                                                                                Order order = optionalOrder.get();
                                                                                OrderResponse orderResponse = OrderResponse
                                                                                                .from(order);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(orderResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached order for key: {}",
                                                                                                                        cacheKey);
                                                                                                        return ApiResponse
                                                                                                                        .success("Order retrieved successfully",
                                                                                                                                        orderResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding order by ID: {}",
                                                                                        id, e)));
                                });
        }

        @Override
        public Uni<ApiResponse<OrderRelationResponse>> findOrderRelation(Long id) {
                String cacheKey = CacheKeys.forList("order", "relation", id);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                OrderRelationResponse cachedRelation = fromJson(cachedJson,
                                                                OrderRelationResponse.class);
                                                return Uni.createFrom()
                                                                .item(ApiResponse.success(
                                                                                "Order relation retrieved successfully",
                                                                                cachedRelation));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB and gRPC.", cacheKey);

                                        return tracingMetrics.traceAndMeasure("findOrderRelation",
                                                        "find_order_relation",
                                                        Attributes.builder().put("order.id", id.toString()).build(),
                                                        () -> orderQueryRepository.findOrderById(id)
                                                                        .chain(optionalOrder -> {
                                                                                if (optionalOrder.isEmpty()) {
                                                                                        logger.warn("Order relation not found with ID: {}",
                                                                                                        id);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Order relation not found with ID: "
                                                                                                                        + id);
                                                                                }

                                                                                Order order = optionalOrder.get();

                                                                                pb.order_item.OrderItemCommon.FindByIdOrderItemRequest grpcReq = pb.order_item.OrderItemCommon.FindByIdOrderItemRequest
                                                                                                .newBuilder()
                                                                                                .setId(id.intValue())
                                                                                                .build();

                                                                                return orderItemQueryService
                                                                                                .findOrderItemByOrder(
                                                                                                                grpcReq)
                                                                                                .chain(grpcResp -> {
                                                                                                        List<OrderItemResponse> items = grpcResp
                                                                                                                        .getDataList()
                                                                                                                        .stream()
                                                                                                                        .map(OrderItemResponse::from)
                                                                                                                        .toList();

                                                                                                        OrderRelationResponse relationResponse = OrderRelationResponse
                                                                                                                        .from(order, items);

                                                                                                        return redisService
                                                                                                                        .setReactive(cacheKey,
                                                                                                                                        toJson(relationResponse))
                                                                                                                        .map(v -> {
                                                                                                                                logger.info("Cached order relation for key: {}",
                                                                                                                                                cacheKey);
                                                                                                                                return ApiResponse
                                                                                                                                                .success("Order relation retrieved successfully",
                                                                                                                                                                relationResponse);
                                                                                                                        });
                                                                                                });
                                                                        })
                                                                        .onFailure()
                                                                        .invoke(e -> logger.error(
                                                                                        "Error finding order relation by ID: {}",
                                                                                        id, e)));
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