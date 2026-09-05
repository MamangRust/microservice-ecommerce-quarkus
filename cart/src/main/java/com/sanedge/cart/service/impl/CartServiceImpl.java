package com.sanedge.cart.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.cart.domain.requests.CreateCartRequest;
import com.sanedge.cart.domain.requests.DeleteCartRequest;
import com.sanedge.cart.domain.requests.FindAllCartsRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.cart.domain.response.CartResponse;
import com.sanedge.cart.entity.Cart;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.cart.repository.CartCommandRepository;
import com.sanedge.cart.repository.CartQueryRepository;
import com.sanedge.cart.service.CartService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CartServiceImpl implements CartService {
        private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

        private final CartQueryRepository cartQueryRepository;
        private final CartCommandRepository cartCommandRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public CartServiceImpl(CartQueryRepository cartQueryRepository,
                        CartCommandRepository cartCommandRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.cartQueryRepository = cartQueryRepository;
                this.cartCommandRepository = cartCommandRepository;
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

        @Override
        public Uni<ApiResponsePagination<List<CartResponse>>> findAll(FindAllCartsRequest req) {
                String cacheKey = String.format("carts:user:%d:%d:%d:%s", req.getUserId(), req.getPage(),
                                req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                Attributes attrs = Attributes.builder()
                                .put("cart.userId", req.getUserId())
                                .put("cart.page", req.getPage())
                                .put("cart.size", req.getPageSize())
                                .build();

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CartResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CartResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findAllCarts", "find_all_carts", attrs,
                                                        () -> cartQueryRepository.findCartsByUser(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<CartResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Cart data fetched successfully",
                                                                                                CartResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} carts",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CartResponse>> createCart(CreateCartRequest request) {
                Attributes attrs = Attributes.builder()
                                .put("cart.userId",
                                                request.getUserId() != null ? request.getUserId().toString() : "null")
                                .put("cart.productId",
                                                request.getProductId() != null ? request.getProductId().toString()
                                                                : "null")
                                .build();

                logger.info("Creating new cart for userId={} | productId={} | quantity={}",
                                request.getUserId(), request.getProductId(), request.getQuantity());

                return tracingMetrics.traceAndMeasure("createCart", "create_cart", attrs, () -> {
                        Cart cart = new Cart();
                        cart.setUserId(request.getUserId());
                        cart.setProductId(request.getProductId());
                        cart.setQuantity(request.getQuantity());
                        cart.setName("Product " + request.getProductId());
                        cart.setPrice(100);
                        cart.setImage("default.png");
                        cart.setWeight(100);
                        cart.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                        return cartCommandRepository.persist(cart)
                                        .map(v -> {
                                                CartResponse cartResponse = CartResponse.from(cart);
                                                logger.info("Successfully created cart with id: {} for userId: {}",
                                                                cart.id, cart.getUserId());
                                                return ApiResponse.success("Cart created successfully", cartResponse);
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deletePermanent(Long cartId) {
                Attributes attrs = Attributes.builder()
                                .put("cart.id", cartId)
                                .build();

                logger.info("Permanently deleting cart with id: {}", cartId);

                return tracingMetrics.traceAndMeasure("deleteCartPermanent", "delete_cart_permanent", attrs,
                                () -> cartCommandRepository.findById(cartId)
                                                .chain(cartToDelete -> {
                                                        if (cartToDelete == null) {
                                                                logger.warn("Permanent delete failed - cart not found with id: {}",
                                                                                cartId);
                                                                throw new ResourceNotFoundException(
                                                                                "Cart not found with id: " + cartId);
                                                        }

                                                        return cartCommandRepository.deleteCartById(cartId)
                                                                        .map(v -> {
                                                                                logger.info("Successfully permanently deleted cart with id: {}",
                                                                                                cartId);
                                                                                return ApiResponse.<Void>success(
                                                                                                "Cart deleted permanently");
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteAllPermanently(DeleteCartRequest req) {
                logger.info("Permanently deleting carts with ids: {}", req.getCartIds());

                return tracingMetrics.traceAndMeasure("deleteCartsPermanently", "delete_carts_permanently", () -> {
                        List<Long> ids = req.getCartIds().stream()
                                        .map(Integer::longValue)
                                        .toList();

                        return cartCommandRepository.deleteCartsByIds(ids)
                                        .map(v -> {
                                                logger.info("Successfully permanently deleted carts with ids: {}", ids);
                                                return ApiResponse.<Void>success("Carts deleted permanently");
                                        });
                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        FindAllCartsRequest request,
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