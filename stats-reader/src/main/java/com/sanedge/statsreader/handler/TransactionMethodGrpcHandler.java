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
import pb.transaction.stats.MutinyTransactionMethodServiceGrpc;
import pb.transaction.stats.TransactionMethod;

@GrpcService
@Singleton
public class TransactionMethodGrpcHandler
        extends MutinyTransactionMethodServiceGrpc.TransactionMethodServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodSuccess(
            TransactionMethod.MonthMethodTransactionRequest request) {
        return repo.findMonthlyTransactionMethodByStatus(request.getYear(), "success")
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionMonthPaymentMethod.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly successful transaction methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionCommon.TransactionMonthlyMethod.newBuilder()
                                .setMonth(strOf(r, "month"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setTotalTransactions(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodSuccess(
            TransactionMethod.YearMethodTransactionRequest request) {
        return repo.findYearlyTransactionMethodByStatus(request.getYear(), "success")
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionYearPaymentmethod.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly successful transaction methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionCommon.TransactionYearlyMethod.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setTotalTransactions(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionMonthPaymentMethod> getMonthlyTransactionMethodFailed(
            TransactionMethod.MonthMethodTransactionRequest request) {
        return repo.findMonthlyTransactionMethodByStatus(request.getYear(), "failed")
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionMonthPaymentMethod.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly failed transaction methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionCommon.TransactionMonthlyMethod.newBuilder()
                                .setMonth(strOf(r, "month"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setTotalTransactions(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<TransactionCommon.ApiResponseTransactionYearPaymentmethod> getYearlyTransactionMethodFailed(
            TransactionMethod.YearMethodTransactionRequest request) {
        return repo.findYearlyTransactionMethodByStatus(request.getYear(), "failed")
                .map(rows -> {
                    var b = TransactionCommon.ApiResponseTransactionYearPaymentmethod.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly failed transaction methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionCommon.TransactionYearlyMethod.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setTotalTransactions(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
