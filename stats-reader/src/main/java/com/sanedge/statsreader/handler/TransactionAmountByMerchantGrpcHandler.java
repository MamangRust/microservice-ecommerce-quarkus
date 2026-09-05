package com.sanedge.statsreader.handler;

import static com.sanedge.statsreader.handler.StatsRow.intOf;
import static com.sanedge.statsreader.handler.StatsRow.strOf;

import com.sanedge.statsreader.repository.ClickHouseStatsRepository;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.TransactionCommon;
import pb.transaction.stats.MutinyTransactionAmountByMerchantServiceGrpc;
import pb.transaction.stats.TransactionAmount;

@GrpcService
@Singleton
public class TransactionAmountByMerchantGrpcHandler
        extends MutinyTransactionAmountByMerchantServiceGrpc.TransactionAmountByMerchantServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionMonthAmountSuccess> getMonthlyAmountSuccessByMerchant(
            TransactionAmount.MonthAmountTransactionMerchantRequest request) {
        return repo.findMonthlyTransactionAmountByStatusMerchant(request.getYear(), "success", request.getMerchantId())
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionMonthAmountSuccess.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly successful transaction amounts by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionCommon.TransactionMonthlyAmountSuccess.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionYearAmountSuccess> getYearlyAmountSuccessByMerchant(
            TransactionAmount.YearAmountTransactionMerchantRequest request) {
        return repo.findYearlyTransactionAmountByStatusMerchant(request.getYear(), "success", request.getMerchantId())
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionYearAmountSuccess.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly successful transaction amounts by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionCommon.TransactionYearlyAmountSuccess.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionMonthAmountFailed> getMonthlyAmountFailedByMerchant(
            TransactionAmount.MonthAmountTransactionMerchantRequest request) {
        return repo.findMonthlyTransactionAmountByStatusMerchant(request.getYear(), "failed", request.getMerchantId())
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionMonthAmountFailed.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly failed transaction amounts by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionCommon.TransactionMonthlyAmountFailed.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionYearAmountFailed> getYearlyAmountFailedByMerchant(
            TransactionAmount.YearAmountTransactionMerchantRequest request) {
        return repo.findYearlyTransactionAmountByStatusMerchant(request.getYear(), "failed", request.getMerchantId())
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionYearAmountFailed.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly failed transaction amounts by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionCommon.TransactionYearlyAmountFailed.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
