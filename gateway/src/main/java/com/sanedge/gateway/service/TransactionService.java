package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.TransactionDto;
import io.smallrye.mutiny.Uni;

public interface TransactionService {
    Uni<TransactionDto.ApiResponsePaginationTransaction> findAll(int page, int size, String search);
    Uni<TransactionDto.ApiResponsePaginationTransaction> findByMerchant(int merchantId, int page, int size, String search);
    Uni<TransactionDto.ApiResponseTransaction> findById(int id);
    Uni<TransactionDto.ApiResponseTransaction> findByOrderId(int orderId);
    Uni<TransactionDto.ApiResponsePaginationTransaction> findByActive(int page, int size, String search);
    Uni<TransactionDto.ApiResponsePaginationTransactionDeleteAt> findByTrashed(int page, int size, String search);

    Uni<TransactionDto.ApiResponseTransaction> create(pb.transaction.TransactionCommand.CreateTransactionRequest body);
    Uni<TransactionDto.ApiResponseTransaction> update(int id, pb.transaction.TransactionCommand.UpdateTransactionRequest body);
    Uni<TransactionDto.ApiResponseTransactionDeleteAt> delete(int id);
    Uni<TransactionDto.ApiResponseTransactionDeleteAt> restore(int id);
    Uni<TransactionDto.SimpleResponse> deletePermanent(int id);
    Uni<TransactionDto.SimpleResponse> restoreAll();
    Uni<TransactionDto.SimpleResponse> deleteTransactionByOrderPermanent(int orderId);
    Uni<TransactionDto.SimpleResponse> deleteAllPermanent();

    Uni<TransactionDto.ApiResponseTransactionMonthAmountSuccess> getMonthlyAmountSuccess(int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearAmountSuccess> getYearlyAmountSuccess(int year);
    Uni<TransactionDto.ApiResponseTransactionMonthAmountFailed> getMonthlyAmountFailed(int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearAmountFailed> getYearlyAmountFailed(int year);

    Uni<TransactionDto.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodSuccess(int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodSuccess(int year);
    Uni<TransactionDto.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodFailed(int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodFailed(int year);

    Uni<TransactionDto.ApiResponseTransactionMonthAmountSuccess> getMonthlyAmountSuccessByMerchant(int merchantId, int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearAmountSuccess> getYearlyAmountSuccessByMerchant(int merchantId, int year);
    Uni<TransactionDto.ApiResponseTransactionMonthAmountFailed> getMonthlyAmountFailedByMerchant(int merchantId, int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearAmountFailed> getYearlyAmountFailedByMerchant(int merchantId, int year);

    Uni<TransactionDto.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodByMerchantSuccess(int merchantId, int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodByMerchantSuccess(int merchantId, int year);
    Uni<TransactionDto.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodByMerchantFailed(int merchantId, int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodByMerchantFailed(int merchantId, int year);
}
