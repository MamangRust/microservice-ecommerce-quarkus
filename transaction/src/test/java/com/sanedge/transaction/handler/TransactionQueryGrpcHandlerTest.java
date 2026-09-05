package com.sanedge.transaction.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.service.TransactionQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.transaction.TransactionCommon;
import pb.transaction.TransactionQuery;

@ExtendWith(MockitoExtension.class)
class TransactionQueryGrpcHandlerTest {

        @Mock
        private TransactionQueryService transactionQueryService;

        private TransactionQueryGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new TransactionQueryGrpcHandler();
                handler.transactionQueryService = transactionQueryService;
        }

        private TransactionResponse createTransactionResponse(Long id) {
                return TransactionResponse.builder()
                                .id(id)
                                .orderId(100)
                                .merchantId(200)
                                .paymentMethod("Credit Card")
                                .amount(110000)
                                .paymentStatus("SUCCESS")
                                .createdAt("2024-01-01 00:00:00.0")
                                .updatedAt("2024-01-01 00:00:00.0")
                                .build();
        }

        @Test
        @DisplayName("findAllTransactions - should return ApiResponsePaginationTransaction on success")
        void findAllTransactions_Success() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).setPageSize(10).build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponsePagination<java.util.List<TransactionResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Transactions retrieved successfully", java.util.List.of(data), null);

                when(transactionQueryService.findAllTransactions(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommon.ApiResponsePaginationTransaction response = handler.findAllTransactions(request)
                                .await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findAllTransactions - should return INTERNAL on failure")
        void findAllTransactions_InternalError() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).setPageSize(10).build();

                when(transactionQueryService.findAllTransactions(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findAllTransactions(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("findByMerchant - should return ApiResponsePaginationTransaction on success")
        void findByMerchant_Success() {
                TransactionQuery.FindAllTransactionByMerchantRequest request = TransactionQuery.FindAllTransactionByMerchantRequest
                                .newBuilder().setMerchantId(200).setPage(1).setPageSize(10).build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponsePagination<java.util.List<TransactionResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Merchant transactions retrieved successfully", java.util.List.of(data),
                                null);

                when(transactionQueryService.findByMerchant(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommon.ApiResponsePaginationTransaction response = handler.findByMerchant(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("findById - should return ApiResponseTransaction on success")
        void findById_Success() {
                TransactionCommon.FindByIdTransactionRequest request = TransactionCommon.FindByIdTransactionRequest
                                .newBuilder()
                                .setId(1).build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Transaction retrieved successfully",
                                data);

                when(transactionQueryService.findById(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommon.ApiResponseTransaction response = handler.findById(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("findById - should return NOT_FOUND when transaction not found")
        void findById_NotFound() {
                TransactionCommon.FindByIdTransactionRequest request = TransactionCommon.FindByIdTransactionRequest
                                .newBuilder()
                                .setId(999).build();

                when(transactionQueryService.findById(anyInt()))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Transaction not found")));

                StatusRuntimeException ex = null;
                try {
                        handler.findById(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("findById - should return INTERNAL on generic exception")
        void findById_InternalError() {
                TransactionCommon.FindByIdTransactionRequest request = TransactionCommon.FindByIdTransactionRequest
                                .newBuilder()
                                .setId(1).build();

                when(transactionQueryService.findById(anyInt()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findById(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("findByOrderId - should return ApiResponseTransaction on success")
        void findByOrderId_Success() {
                TransactionQuery.FindByOrderIdTransactionRequest request = TransactionQuery.FindByOrderIdTransactionRequest
                                .newBuilder().setOrderId(100).build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Transaction retrieved successfully",
                                data);

                when(transactionQueryService.findByOrderId(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommon.ApiResponseTransaction response = handler.findByOrderId(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("findByOrderId - should return NOT_FOUND when transaction not found")
        void findByOrderId_NotFound() {
                TransactionQuery.FindByOrderIdTransactionRequest request = TransactionQuery.FindByOrderIdTransactionRequest
                                .newBuilder().setOrderId(999).build();

                when(transactionQueryService.findByOrderId(anyInt()))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Transaction not found")));

                StatusRuntimeException ex = null;
                try {
                        handler.findByOrderId(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("findByActive - should return ApiResponsePaginationTransaction on success")
        void findByActive_Success() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).setPageSize(10).build();

                ApiResponsePagination<java.util.List<com.sanedge.transaction.domain.response.TransactionResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Active transactions retrieved successfully", java.util.List.of(), null);

                when(transactionQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommon.ApiResponsePaginationTransaction response = handler.findByActive(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("findByTrashed - should return ApiResponsePaginationTransactionDeleteAt on success")
        void findByTrashed_Success() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).setPageSize(10).build();

                createTransactionResponse(1L);
                ApiResponsePagination<java.util.List<com.sanedge.transaction.domain.response.TransactionResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Trashed transactions retrieved successfully", java.util.List.of(), null);

                when(transactionQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommon.ApiResponsePaginationTransactionDeleteAt response = handler.findByTrashed(request)
                                .await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }
}
