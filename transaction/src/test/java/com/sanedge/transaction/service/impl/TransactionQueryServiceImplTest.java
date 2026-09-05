package com.sanedge.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.enums.PaymentStatus;
import com.sanedge.transaction.repository.TransactionQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceImplTest {

        @Mock
        private TransactionQueryRepository transactionQueryRepository;

        @Mock
        private RedisService redisService;

        @Mock
        private TracingMetrics tracingMetrics;

        private TransactionQueryServiceImpl service;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                service = new TransactionQueryServiceImpl(
                                transactionQueryRepository,
                                redisService,
                                tracingMetrics,
                                objectMapper);

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(2);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any());
        }

        private Transaction createMockTransaction(Long id) {
                Transaction t = new Transaction();
                t.setId(id);
                t.setOrderId(1);
                t.setMerchantId(1);
                t.setAmount(150000);
                t.setPaymentMethod("CREDIT");
                t.setStatus(PaymentStatus.PENDING);
                t.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                t.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return t;
        }

        private String toJson(Object obj) {
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize in test helper", e);
                }
        }

        @Nested
        @DisplayName("findAllTransactions tests")
        class FindAllTests {

                @Test
                @DisplayName("cache miss - fetch from DB and cache result")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransactionRequest req = new FindAllTransactionRequest();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTransactions(any(FindAllTransactionRequest.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponse>> result = service.findAllTransactions(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.message()).isEqualTo("Transactions retrieved successfully");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - return cached response without DB call")
                void cacheHit_returnsCached() {
                        FindAllTransactionRequest req = new FindAllTransactionRequest();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        TransactionResponse cachedData = TransactionResponse.from(createMockTransaction(1L));
                        ApiResponsePagination<List<TransactionResponse>> cachedResponse = new ApiResponsePagination<>(
                                        "success", "Transactions retrieved successfully", List.of(cachedData), null);

                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                        ApiResponsePagination<List<TransactionResponse>> result = service.findAllTransactions(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache miss with search parameter")
                void cacheMiss_withSearch() {
                        FindAllTransactionRequest req = new FindAllTransactionRequest();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("CREDIT");

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTransactions(any(FindAllTransactionRequest.class)))
                                        .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(), 0)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponse>> result = service.findAllTransactions(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).isEmpty();
                }
        }

        @Nested
        @DisplayName("findByActive tests")
        class FindByActiveTests {

                @Test
                @DisplayName("cache miss - fetch from DB and cache result")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransactionRequest req = new FindAllTransactionRequest();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findActiveTransactions(any(FindAllTransactionRequest.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponseDeleteAt>> result = service.findByActive(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.message()).isEqualTo("Active transactions retrieved successfully");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - return cached response")
                void cacheHit_returnsCached() {
                        FindAllTransactionRequest req = new FindAllTransactionRequest();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        TransactionResponseDeleteAt cachedData = TransactionResponseDeleteAt
                                        .from(createMockTransaction(1L));
                        ApiResponsePagination<List<TransactionResponseDeleteAt>> cachedResponse = new ApiResponsePagination<>(
                                        "success", "Active transactions retrieved successfully", List.of(cachedData),
                                        null);

                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                        ApiResponsePagination<List<TransactionResponseDeleteAt>> result = service.findByActive(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findByTrashed tests")
        class FindByTrashedTests {

                @Test
                @DisplayName("cache miss - fetch from DB and cache result")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransactionRequest req = new FindAllTransactionRequest();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTrashedTransactions(any(FindAllTransactionRequest.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponseDeleteAt>> result = service.findByTrashed(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.message()).isEqualTo("Trashed transactions retrieved successfully");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - return cached response")
                void cacheHit_returnsCached() {
                        FindAllTransactionRequest req = new FindAllTransactionRequest();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        TransactionResponseDeleteAt cachedData = TransactionResponseDeleteAt
                                        .from(createMockTransaction(1L));
                        ApiResponsePagination<List<TransactionResponseDeleteAt>> cachedResponse = new ApiResponsePagination<>(
                                        "success", "Trashed transactions retrieved successfully", List.of(cachedData),
                                        null);

                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                        ApiResponsePagination<List<TransactionResponseDeleteAt>> result = service.findByTrashed(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findById tests")
        class FindByIdTests {

                @Test
                @DisplayName("cache miss - fetch from DB and cache result")
                void cacheMiss_fetchesFromDb() {
                        Integer transactionId = 1;

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTransactionById(anyLong()))
                                        .thenReturn(Uni.createFrom().item(Optional.of(createMockTransaction(1L))));
                        when(redisService.setReactive(anyString(), anyString()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<TransactionResponse> result = service.findById(transactionId).await()
                                        .indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.message()).isEqualTo("Transaction retrieved successfully");
                        assertThat(result.data().getId().longValue()).isEqualTo(transactionId.longValue());
                }

                @Test
                @DisplayName("cache hit - return cached transaction")
                void cacheHit_returnsCached() {
                        Integer transactionId = 1;
                        TransactionResponse cached = TransactionResponse.from(createMockTransaction(1L));

                        when(redisService.getReactive("transaction:id:" + transactionId))
                                        .thenReturn(Uni.createFrom().item(toJson(cached)));

                        ApiResponse<TransactionResponse> result = service.findById(transactionId).await()
                                        .indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data().getId().longValue()).isEqualTo(transactionId.longValue());
                }

                @Test
                @DisplayName("should fail when transaction not found")
                void notFound_returnsError() {
                        Integer transactionId = 999;

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTransactionById(anyLong()))
                                        .thenReturn(Uni.createFrom().item(Optional.empty()));

                        assertThatThrownBy(() -> service.findById(transactionId).await().indefinitely())
                                        .isInstanceOf(com.sanedge.common.exception.ResourceNotFoundException.class)
                                        .hasMessageContaining("Transaction not found with ID: 999");
                }
        }

        @Nested
        @DisplayName("findByMerchant tests")
        class FindByMerchantTests {

                @Test
                @DisplayName("cache miss - fetch from DB and cache result")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransactionByMerchantRequest req = new FindAllTransactionByMerchantRequest();
                        req.setMerchantId(1);
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository
                                        .findTransactionsByMerchant(any(FindAllTransactionByMerchantRequest.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponse>> result = service.findByMerchant(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.message()).isEqualTo("Merchant transactions retrieved successfully");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - return cached list")
                void cacheHit_returnsCached() {
                        FindAllTransactionByMerchantRequest req = new FindAllTransactionByMerchantRequest();
                        req.setMerchantId(1);
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        TransactionResponse cachedData = TransactionResponse.from(createMockTransaction(1L));
                        ApiResponsePagination<List<TransactionResponse>> cachedResponse = new ApiResponsePagination<>(
                                        "success", "Merchant transactions retrieved successfully", List.of(cachedData),
                                        null);

                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                        ApiResponsePagination<List<TransactionResponse>> result = service.findByMerchant(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("should return empty list if no transactions found")
                void emptyList_whenNotFound() {
                        FindAllTransactionByMerchantRequest req = new FindAllTransactionByMerchantRequest();
                        req.setMerchantId(999);
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("");

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository
                                        .findTransactionsByMerchant(any(FindAllTransactionByMerchantRequest.class)))
                                        .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(), 0)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponse>> result = service.findByMerchant(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).isEmpty();
                }
        }
}
