package com.sanedge.transaction.service.impl;

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
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.repository.TransactionQueryRepository;
import com.sanedge.transaction.service.TransactionQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransactionQueryServiceImpl implements TransactionQueryService {
        private static final Logger logger = LoggerFactory.getLogger(TransactionQueryServiceImpl.class);

        private final TransactionQueryRepository transactionQueryRepository;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;
        private final ObjectMapper objectMapper;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public TransactionQueryServiceImpl(TransactionQueryRepository transactionQueryRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics,
                        ObjectMapper objectMapper) {
                this.transactionQueryRepository = transactionQueryRepository;
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
        public Uni<ApiResponsePagination<List<TransactionResponse>>> findAllTransactions(
                        FindAllTransactionRequest req) {
                String cacheKey = String.format("transaction:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TransactionResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TransactionResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findAllTransactions",
                                                        "find_all_transactions",
                                                        Attributes.builder()
                                                                        .put("transaction.page", req.getPage())
                                                                        .put("transaction.size", req.getPageSize())
                                                                        .build(),
                                                        () -> transactionQueryRepository.findTransactions(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<TransactionResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Transactions retrieved successfully",
                                                                                                TransactionResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} transactions",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByActive(
                        FindAllTransactionRequest req) {
                String cacheKey = String.format("transaction:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TransactionResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findActiveTransactions",
                                                        "find_active_transactions",
                                                        Attributes.builder()
                                                                        .put("transaction.page", req.getPage())
                                                                        .put("transaction.size", req.getPageSize())
                                                                        .build(),
                                                        () -> transactionQueryRepository.findActiveTransactions(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Active transactions retrieved successfully",
                                                                                                TransactionResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active transactions",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByTrashed(
                        FindAllTransactionRequest req) {
                String cacheKey = String.format("transaction:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TransactionResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTrashedTransactions",
                                                        "find_trashed_transactions",
                                                        Attributes.builder()
                                                                        .put("transaction.page", req.getPage())
                                                                        .put("transaction.size", req.getPageSize())
                                                                        .build(),
                                                        () -> transactionQueryRepository.findTrashedTransactions(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Trashed transactions retrieved successfully",
                                                                                                TransactionResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed transactions",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<TransactionResponse>>> findByMerchant(
                        FindAllTransactionByMerchantRequest req) {
                String cacheKey = String.format("transaction:merchant:%d:%d:%d:%s",
                                req.getMerchantId(), req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TransactionResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TransactionResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTransactionsByMerchant",
                                                        "find_transactions_by_merchant",
                                                        Attributes.builder()
                                                                        .put("merchant.id", req.getMerchantId() != null
                                                                                        ? req.getMerchantId().toString()
                                                                                        : "null")
                                                                        .put("transaction.page", req.getPage())
                                                                        .put("transaction.size", req.getPageSize())
                                                                        .build(),
                                                        () -> transactionQueryRepository.findTransactionsByMerchant(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<TransactionResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Merchant transactions retrieved successfully",
                                                                                                TransactionResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} merchant transactions",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<TransactionResponse>> findById(Integer id) {
                String cacheKey = "transaction:id:" + id;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                TransactionResponse cachedTx = fromJson(cachedJson,
                                                                TransactionResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Transaction retrieved successfully", cachedTx));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTransactionById",
                                                        "find_transaction_by_id",
                                                        Attributes.builder().put("transaction.id", id.toString())
                                                                        .build(),
                                                        () -> transactionQueryRepository
                                                                        .findTransactionById(id.longValue())
                                                                        .chain(optionalTx -> {
                                                                                if (optionalTx.isEmpty()) {
                                                                                        logger.warn("Transaction not found with ID: {}",
                                                                                                        id);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Transaction not found with ID: "
                                                                                                                        + id);
                                                                                }

                                                                                Transaction tx = optionalTx.get();
                                                                                TransactionResponse txResponse = TransactionResponse
                                                                                                .from(tx);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(txResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached transaction for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found transaction with ID: {}",
                                                                                                                        id);
                                                                                                        return ApiResponse
                                                                                                                        .success("Transaction retrieved successfully",
                                                                                                                                        txResponse);
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        public Uni<ApiResponse<TransactionResponse>> findByOrderId(Integer id) {
                String cacheKey = "transaction:order:" + id;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                TransactionResponse cachedTx = fromJson(cachedJson,
                                                                TransactionResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Transaction retrieved successfully", cachedTx));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return tracingMetrics.traceAndMeasure("findTransactionByOrderId",
                                                        "find_transaction_by_order_id",
                                                        Attributes.builder().put("order.id", id.toString()).build(),
                                                        () -> transactionQueryRepository.findByOrderId(id)
                                                                        .chain(optionalTx -> {
                                                                                if (optionalTx.isEmpty()) {
                                                                                        logger.warn("Transaction not found for order ID: {}",
                                                                                                        id);
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Transaction not found for order ID: "
                                                                                                                        + id);
                                                                                }

                                                                                Transaction tx = optionalTx.get();
                                                                                TransactionResponse txResponse = TransactionResponse
                                                                                                .from(tx);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(txResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached transaction for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found transaction for order ID: {}",
                                                                                                                        id);
                                                                                                        return ApiResponse
                                                                                                                        .success("Transaction retrieved successfully",
                                                                                                                                        txResponse);
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