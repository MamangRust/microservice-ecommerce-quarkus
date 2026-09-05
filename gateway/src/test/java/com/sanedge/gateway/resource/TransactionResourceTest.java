package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.TransactionDto;
import com.sanedge.gateway.service.TransactionService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class TransactionResourceTest {
    @Mock
    TransactionService transactionService;
    private TransactionResource transactionResource;

    @BeforeEach
    void setUp() throws Exception {
        transactionResource = new TransactionResource();
        Field f = TransactionResource.class.getDeclaredField("transactionService");
        f.setAccessible(true);
        f.set(transactionResource, transactionService);
    }

    private TransactionDto.TransactionResponse mk(int id) {
        return new TransactionDto.TransactionResponse(id, 1, 1, "credit_card", 1000, "success", "", "");
    }

    @Test
    void findAll_Success() {
        TransactionDto.ApiResponsePaginationTransaction dto = new TransactionDto.ApiResponsePaginationTransaction(
                List.of(mk(1)), "success", "ok");
        lenient().when(transactionService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.findAll(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByMerchant_Success() {
        TransactionDto.ApiResponsePaginationTransaction dto = new TransactionDto.ApiResponsePaginationTransaction(
                List.of(), "success", "ok");
        lenient().when(transactionService.findByMerchant(anyInt(), anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.findByMerchant(1, 1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findById_Success() {
        TransactionDto.ApiResponseTransaction dto = new TransactionDto.ApiResponseTransaction(mk(1), "success", "ok");
        lenient().when(transactionService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.findById(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByOrderId_Success() {
        TransactionDto.ApiResponseTransaction dto = new TransactionDto.ApiResponseTransaction(mk(1), "success", "ok");
        lenient().when(transactionService.findByOrderId(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.findByOrderId(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByActive_Success() {
        TransactionDto.ApiResponsePaginationTransaction dto = new TransactionDto.ApiResponsePaginationTransaction(
                List.of(), "success", "ok");
        lenient().when(transactionService.findByActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.findByActive(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void findByTrashed_Success() {
        TransactionDto.ApiResponsePaginationTransactionDeleteAt dto = new TransactionDto.ApiResponsePaginationTransactionDeleteAt(
                List.of(), "success", "ok");
        lenient().when(transactionService.findByTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.findByTrashed(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void create_Success_Returns201() {
        TransactionDto.ApiResponseTransaction dto = new TransactionDto.ApiResponseTransaction(mk(1), "success", "ok");
        lenient().when(transactionService.create(any())).thenReturn(Uni.createFrom().item(dto));
        pb.transaction.TransactionCommand.CreateTransactionRequest req = pb.transaction.TransactionCommand.CreateTransactionRequest.newBuilder().build();
        Response r = transactionResource.create(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void update_Success() {
        TransactionDto.ApiResponseTransaction dto = new TransactionDto.ApiResponseTransaction(mk(1), "success", "ok");
        lenient().when(transactionService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        pb.transaction.TransactionCommand.UpdateTransactionRequest req = pb.transaction.TransactionCommand.UpdateTransactionRequest.newBuilder().build();
        Response r = transactionResource.update(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void delete_Success() {
        TransactionDto.ApiResponseTransactionDeleteAt dto = new TransactionDto.ApiResponseTransactionDeleteAt(mk(1), "success", "ok");
        lenient().when(transactionService.delete(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.delete(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restore_Success() {
        TransactionDto.ApiResponseTransactionDeleteAt dto = new TransactionDto.ApiResponseTransactionDeleteAt(mk(1), "success", "ok");
        lenient().when(transactionService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.restore(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deletePermanent_Success() {
        TransactionDto.SimpleResponse dto = new TransactionDto.SimpleResponse("success", "ok");
        lenient().when(transactionService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.deletePermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAll_Success() {
        TransactionDto.SimpleResponse dto = new TransactionDto.SimpleResponse("success", "ok");
        lenient().when(transactionService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.restoreAll().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteTransactionByOrderPermanent_Success() {
        TransactionDto.SimpleResponse dto = new TransactionDto.SimpleResponse("success", "ok");
        lenient().when(transactionService.deleteTransactionByOrderPermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.deleteTransactionByOrderPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllPermanent_Success() {
        TransactionDto.SimpleResponse dto = new TransactionDto.SimpleResponse("success", "ok");
        lenient().when(transactionService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.deleteAllPermanent().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getMonthlyAmountSuccess_Success() {
        TransactionDto.ApiResponseTransactionMonthAmountSuccess dto = new TransactionDto.ApiResponseTransactionMonthAmountSuccess(
                List.of(), "success", "ok");
        lenient().when(transactionService.getMonthlyAmountSuccess(anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.getMonthlyAmountSuccess(2024, 1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getYearlyAmountSuccess_Success() {
        TransactionDto.ApiResponseTransactionYearAmountSuccess dto = new TransactionDto.ApiResponseTransactionYearAmountSuccess(
                List.of(), "success", "ok");
        lenient().when(transactionService.getYearlyAmountSuccess(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.getYearlyAmountSuccess(2024).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getMonthlyTransactionMethodByMerchantFailed_Success() {
        TransactionDto.ApiResponseTransactionMonthPaymentMethod dto = new TransactionDto.ApiResponseTransactionMonthPaymentMethod(
                List.of(), "success", "ok");
        lenient().when(transactionService.getMonthlyTransactionMethodByMerchantFailed(anyInt(), anyInt(), anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = transactionResource.getMonthlyTransactionMethodByMerchantFailed(1, 2024, 1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
