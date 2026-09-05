package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.TransactionDto;
import com.sanedge.gateway.service.TransactionService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransactionServiceImpl implements TransactionService {

        private static final Logger LOG = Logger.getLogger(TransactionServiceImpl.class);

        @Inject
        TelemetryHelper telemetryHelper;

        @GrpcClient("transaction")
        pb.transaction.MutinyTransactionQueryServiceGrpc.MutinyTransactionQueryServiceStub transactionQueryService;

        @GrpcClient("transaction")
        pb.transaction.MutinyTransactionCommandServiceGrpc.MutinyTransactionCommandServiceStub transactionCommandService;

        @GrpcClient("statsreader")
        pb.transaction.stats.MutinyTransactionAmountServiceGrpc.MutinyTransactionAmountServiceStub transactionAmountService;

        @GrpcClient("statsreader")
        pb.transaction.stats.MutinyTransactionMethodServiceGrpc.MutinyTransactionMethodServiceStub transactionMethodService;

        @GrpcClient("statsreader")
        pb.transaction.stats.MutinyTransactionAmountByMerchantServiceGrpc.MutinyTransactionAmountByMerchantServiceStub transactionAmountByMerchantService;

        @GrpcClient("statsreader")
        pb.transaction.stats.MutinyTransactionMethodByMerchantServiceGrpc.MutinyTransactionMethodByMerchantServiceStub transactionMethodByMerchantService;

        @Override
        public Uni<TransactionDto.ApiResponsePaginationTransaction> findAll(int page, int size, String search) {
                return telemetryHelper.traceAndMetric("transaction.findAll", () -> transactionQueryService
                                .findAllTransactions(
                                                pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                                                                .setPage(page)
                                                                .setPageSize(size)
                                                                .setSearch(search == null ? "" : search)
                                                                .build())
                                .map(TransactionDto.ApiResponsePaginationTransaction::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                                "Failed to find all transactions: " + throwable.getMessage(),
                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponsePaginationTransaction> findByMerchant(int merchantId, int page, int size,
                        String search) {
                return telemetryHelper.traceAndMetric("transaction.findByMerchant", () -> transactionQueryService
                                .findByMerchant(
                                                pb.transaction.TransactionQuery.FindAllTransactionByMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setPage(page)
                                                                .setPageSize(size)
                                                                .setSearch(search == null ? "" : search)
                                                                .build())
                                .map(TransactionDto.ApiResponsePaginationTransaction::from)
                                .onFailure().invoke(
                                                throwable -> LOG.error(
                                                                "Failed to find transactions by merchant " + merchantId
                                                                                + ": " + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransaction> findById(int id) {
                return telemetryHelper.traceAndMetric("transaction.findById", () -> transactionQueryService.findById(
                                pb.transaction.TransactionCommon.FindByIdTransactionRequest.newBuilder()
                                                .setId(id)
                                                .build())
                                .map(TransactionDto.ApiResponseTransaction::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                                "Failed to find transaction " + id + ": " + throwable.getMessage(),
                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransaction> findByOrderId(int orderId) {
                return telemetryHelper.traceAndMetric("transaction.findByOrderId", () -> transactionQueryService
                                .findByOrderId(
                                                pb.transaction.TransactionQuery.FindByOrderIdTransactionRequest
                                                                .newBuilder()
                                                                .setOrderId(orderId)
                                                                .build())
                                .map(TransactionDto.ApiResponseTransaction::from)
                                .onFailure().invoke(throwable -> LOG.error("Failed to find transaction by order "
                                                + orderId + ": " + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponsePaginationTransaction> findByActive(int page, int size, String search) {
                return telemetryHelper.traceAndMetric("transaction.findByActive", () -> transactionQueryService
                                .findByActive(
                                                pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                                                                .setPage(page)
                                                                .setPageSize(size)
                                                                .setSearch(search == null ? "" : search)
                                                                .build())
                                .map(TransactionDto.ApiResponsePaginationTransaction::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                                "Failed to find active transactions: " + throwable.getMessage(),
                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponsePaginationTransactionDeleteAt> findByTrashed(int page, int size,
                        String search) {
                return telemetryHelper.traceAndMetric("transaction.findByTrashed", () -> transactionQueryService
                                .findByTrashed(
                                                pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                                                                .setPage(page)
                                                                .setPageSize(size)
                                                                .setSearch(search == null ? "" : search)
                                                                .build())
                                .map(TransactionDto.ApiResponsePaginationTransactionDeleteAt::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                                "Failed to find trashed transactions: " + throwable.getMessage(),
                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransaction> create(
                        pb.transaction.TransactionCommand.CreateTransactionRequest body) {
                return telemetryHelper.traceAndMetric("transaction.create", () -> transactionCommandService.create(body)
                                .map(TransactionDto.ApiResponseTransaction::from)
                                .onFailure().invoke(throwable -> LOG.error(
                                                "Failed to create transaction: " + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransaction> update(int id,
                        pb.transaction.TransactionCommand.UpdateTransactionRequest body) {
                pb.transaction.TransactionCommand.UpdateTransactionRequest req = pb.transaction.TransactionCommand.UpdateTransactionRequest
                                .newBuilder(body)
                                .setTransactionId(id)
                                .build();
                return telemetryHelper.traceAndMetric("transaction.update", () -> transactionCommandService.update(req)
                                .map(TransactionDto.ApiResponseTransaction::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                                "Failed to update transaction " + id + ": " + throwable.getMessage(),
                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionDeleteAt> delete(int id) {
                return telemetryHelper.traceAndMetric("transaction.delete", () -> transactionCommandService
                                .trashedTransaction(
                                                pb.transaction.TransactionCommon.FindByIdTransactionRequest.newBuilder()
                                                                .setId(id)
                                                                .build())
                                .map(TransactionDto.ApiResponseTransactionDeleteAt::from)
                                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete transaction " + id
                                                + ": " + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionDeleteAt> restore(int id) {
                return telemetryHelper.traceAndMetric("transaction.restore", () -> transactionCommandService
                                .restoreTransaction(
                                                pb.transaction.TransactionCommon.FindByIdTransactionRequest.newBuilder()
                                                                .setId(id)
                                                                .build())
                                .map(TransactionDto.ApiResponseTransactionDeleteAt::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                                "Failed to restore transaction " + id + ": " + throwable.getMessage(),
                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.SimpleResponse> deletePermanent(int id) {
                return telemetryHelper.traceAndMetric("transaction.deletePermanent", () -> transactionCommandService
                                .deleteTransactionPermanent(
                                                pb.transaction.TransactionCommon.FindByIdTransactionRequest.newBuilder()
                                                                .setId(id)
                                                                .build())
                                .map(TransactionDto.SimpleResponse::from)
                                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete transaction "
                                                + id + ": " + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.SimpleResponse> restoreAll() {
                return telemetryHelper.traceAndMetric("transaction.restoreAll", () -> transactionCommandService
                                .restoreAllTransaction(
                                                com.google.protobuf.Empty.getDefaultInstance())
                                .map(TransactionDto.SimpleResponse::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error(
                                                "Failed to restore all transactions: " + throwable.getMessage(),
                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.SimpleResponse> deleteTransactionByOrderPermanent(int orderId) {
                return telemetryHelper.traceAndMetric("transaction.deleteByOrderPermanent",
                                () -> transactionCommandService.deleteTransactionByOrderPermanent(
                                                pb.transaction.TransactionCommon.FindByIdTransactionRequest.newBuilder()
                                                                .setId(orderId)
                                                                .build())
                                                .map(TransactionDto.SimpleResponse::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to delete transactions for order " + orderId
                                                                                + ": " + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.SimpleResponse> deleteAllPermanent() {
                return telemetryHelper.traceAndMetric("transaction.deleteAllPermanent", () -> transactionCommandService
                                .deleteAllTransactionPermanent(
                                                com.google.protobuf.Empty.getDefaultInstance())
                                .map(TransactionDto.SimpleResponse::from)
                                .onFailure()
                                .invoke(throwable -> LOG.error("Failed to permanently delete all transactions: "
                                                + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionMonthAmountSuccess> getMonthlyAmountSuccess(int year,
                        int month) {
                return telemetryHelper.traceAndMetric("transaction.getMonthlyAmountSuccess",
                                () -> transactionAmountService.getMonthlyAmountSuccess(
                                                pb.transaction.stats.TransactionAmount.MonthAmountTransactionRequest
                                                                .newBuilder()
                                                                .setYear(year)
                                                                .setMonth(month)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionMonthAmountSuccess::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error("Failed to get monthly amount success: "
                                                                + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionYearAmountSuccess> getYearlyAmountSuccess(int year) {
                return telemetryHelper.traceAndMetric("transaction.getYearlyAmountSuccess",
                                () -> transactionAmountService.getYearlyAmountSuccess(
                                                pb.transaction.stats.TransactionAmount.YearAmountTransactionRequest
                                                                .newBuilder()
                                                                .setYear(year)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionYearAmountSuccess::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error("Failed to get yearly amount success: "
                                                                + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionMonthAmountFailed> getMonthlyAmountFailed(int year, int month) {
                return telemetryHelper.traceAndMetric("transaction.getMonthlyAmountFailed",
                                () -> transactionAmountService.getMonthlyAmountFailed(
                                                pb.transaction.stats.TransactionAmount.MonthAmountTransactionRequest
                                                                .newBuilder()
                                                                .setYear(year)
                                                                .setMonth(month)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionMonthAmountFailed::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error("Failed to get monthly amount failed: "
                                                                + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionYearAmountFailed> getYearlyAmountFailed(int year) {
                return telemetryHelper.traceAndMetric("transaction.getYearlyAmountFailed",
                                () -> transactionAmountService.getYearlyAmountFailed(
                                                pb.transaction.stats.TransactionAmount.YearAmountTransactionRequest
                                                                .newBuilder()
                                                                .setYear(year)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionYearAmountFailed::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error("Failed to get yearly amount failed: "
                                                                + throwable.getMessage(), throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodSuccess(int year,
                        int month) {
                return telemetryHelper.traceAndMetric("transaction.getMonthlyTransactionMethodSuccess",
                                () -> transactionMethodService.getMonthlyTransactionMethodSuccess(
                                                pb.transaction.stats.TransactionMethod.MonthMethodTransactionRequest
                                                                .newBuilder()
                                                                .setYear(year)
                                                                .setMonth(month)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionMonthPaymentMethod::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get monthly transaction method success: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodSuccess(int year) {
                return telemetryHelper.traceAndMetric("transaction.getYearlyTransactionMethodSuccess",
                                () -> transactionMethodService.getYearlyTransactionMethodSuccess(
                                                pb.transaction.stats.TransactionMethod.YearMethodTransactionRequest
                                                                .newBuilder()
                                                                .setYear(year)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionYearPaymentmethod::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get yearly transaction method success: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodFailed(int year,
                        int month) {
                return telemetryHelper.traceAndMetric("transaction.getMonthlyTransactionMethodFailed",
                                () -> transactionMethodService.getMonthlyTransactionMethodFailed(
                                                pb.transaction.stats.TransactionMethod.MonthMethodTransactionRequest
                                                                .newBuilder()
                                                                .setYear(year)
                                                                .setMonth(month)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionMonthPaymentMethod::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get monthly transaction method failed: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodFailed(int year) {
                return telemetryHelper.traceAndMetric("transaction.getYearlyTransactionMethodFailed",
                                () -> transactionMethodService.getYearlyTransactionMethodFailed(
                                                pb.transaction.stats.TransactionMethod.YearMethodTransactionRequest
                                                                .newBuilder()
                                                                .setYear(year)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionYearPaymentmethod::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get yearly transaction method failed: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionMonthAmountSuccess> getMonthlyAmountSuccessByMerchant(
                        int merchantId, int year, int month) {
                return telemetryHelper.traceAndMetric("transaction.getMonthlyAmountSuccessByMerchant",
                                () -> transactionAmountByMerchantService.getMonthlyAmountSuccessByMerchant(
                                                pb.transaction.stats.TransactionAmount.MonthAmountTransactionMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setYear(year)
                                                                .setMonth(month)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionMonthAmountSuccess::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get monthly amount success by merchant: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionYearAmountSuccess> getYearlyAmountSuccessByMerchant(
                        int merchantId, int year) {
                return telemetryHelper.traceAndMetric("transaction.getYearlyAmountSuccessByMerchant",
                                () -> transactionAmountByMerchantService.getYearlyAmountSuccessByMerchant(
                                                pb.transaction.stats.TransactionAmount.YearAmountTransactionMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setYear(year)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionYearAmountSuccess::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get yearly amount success by merchant: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionMonthAmountFailed> getMonthlyAmountFailedByMerchant(
                        int merchantId, int year, int month) {
                return telemetryHelper.traceAndMetric("transaction.getMonthlyAmountFailedByMerchant",
                                () -> transactionAmountByMerchantService.getMonthlyAmountFailedByMerchant(
                                                pb.transaction.stats.TransactionAmount.MonthAmountTransactionMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setYear(year)
                                                                .setMonth(month)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionMonthAmountFailed::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get monthly amount failed by merchant: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionYearAmountFailed> getYearlyAmountFailedByMerchant(
                        int merchantId, int year) {
                return telemetryHelper.traceAndMetric("transaction.getYearlyAmountFailedByMerchant",
                                () -> transactionAmountByMerchantService.getYearlyAmountFailedByMerchant(
                                                pb.transaction.stats.TransactionAmount.YearAmountTransactionMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setYear(year)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionYearAmountFailed::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get yearly amount failed by merchant: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodByMerchantSuccess(
                        int merchantId, int year, int month) {
                return telemetryHelper.traceAndMetric("transaction.getMonthlyTransactionMethodByMerchantSuccess",
                                () -> transactionMethodByMerchantService.getMonthlyTransactionMethodByMerchantSuccess(
                                                pb.transaction.stats.TransactionMethod.MonthMethodTransactionMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setYear(year)
                                                                .setMonth(month)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionMonthPaymentMethod::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get monthly method success by merchant: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodByMerchantSuccess(
                        int merchantId, int year) {
                return telemetryHelper.traceAndMetric("transaction.getYearlyTransactionMethodByMerchantSuccess",
                                () -> transactionMethodByMerchantService.getYearlyTransactionMethodByMerchantSuccess(
                                                pb.transaction.stats.TransactionMethod.YearMethodTransactionMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setYear(year)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionYearPaymentmethod::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get yearly method success by merchant: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodByMerchantFailed(
                        int merchantId, int year, int month) {
                return telemetryHelper.traceAndMetric("transaction.getMonthlyTransactionMethodByMerchantFailed",
                                () -> transactionMethodByMerchantService.getMonthlyTransactionMethodByMerchantFailed(
                                                pb.transaction.stats.TransactionMethod.MonthMethodTransactionMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setYear(year)
                                                                .setMonth(month)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionMonthPaymentMethod::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get monthly method failed by merchant: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }

        @Override
        public Uni<TransactionDto.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodByMerchantFailed(
                        int merchantId, int year) {
                return telemetryHelper.traceAndMetric("transaction.getYearlyTransactionMethodByMerchantFailed",
                                () -> transactionMethodByMerchantService.getYearlyTransactionMethodByMerchantFailed(
                                                pb.transaction.stats.TransactionMethod.YearMethodTransactionMerchantRequest
                                                                .newBuilder()
                                                                .setMerchantId(merchantId)
                                                                .setYear(year)
                                                                .build())
                                                .map(TransactionDto.ApiResponseTransactionYearPaymentmethod::from)
                                                .onFailure()
                                                .invoke(throwable -> LOG.error(
                                                                "Failed to get yearly method failed by merchant: "
                                                                                + throwable.getMessage(),
                                                                throwable)));
        }
}
