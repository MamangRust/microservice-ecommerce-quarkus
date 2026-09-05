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
import pb.transaction.stats.MutinyTransactionAmountServiceGrpc;
import pb.transaction.stats.TransactionAmount;

@GrpcService
@Singleton
public class TransactionAmountGrpcHandler
        extends MutinyTransactionAmountServiceGrpc.TransactionAmountServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionMonthAmountSuccess> getMonthlyAmountSuccess(
            TransactionAmount.MonthAmountTransactionRequest request) {
        return repo.findMonthlyTransactionAmountByStatus(request.getYear(), "success")
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionMonthAmountSuccess.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly successful transaction amounts");
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
    public Uni<TransactionCommon.ApiResponseTransactionYearAmountSuccess> getYearlyAmountSuccess(
            TransactionAmount.YearAmountTransactionRequest request) {
        return repo.findYearlyTransactionAmountByStatus(request.getYear(), "success")
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionYearAmountSuccess.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly successful transaction amounts");
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
    public Uni<TransactionCommon.ApiResponseTransactionMonthAmountFailed> getMonthlyAmountFailed(
            TransactionAmount.MonthAmountTransactionRequest request) {
        return repo.findMonthlyTransactionAmountByStatus(request.getYear(), "failed")
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionMonthAmountFailed.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly failed transaction amounts");
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
    public Uni<TransactionCommon.ApiResponseTransactionYearAmountFailed> getYearlyAmountFailed(
            TransactionAmount.YearAmountTransactionRequest request) {
        return repo.findYearlyTransactionAmountByStatus(request.getYear(), "failed")
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionYearAmountFailed.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly failed transaction amounts");
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
