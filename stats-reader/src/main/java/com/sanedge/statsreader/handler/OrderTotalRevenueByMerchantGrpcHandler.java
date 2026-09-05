package com.sanedge.statsreader.handler;

import static com.sanedge.statsreader.handler.StatsRow.intOf;
import static com.sanedge.statsreader.handler.StatsRow.longOf;
import static com.sanedge.statsreader.handler.StatsRow.strOf;

import com.sanedge.statsreader.repository.ClickHouseStatsRepository;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.order.OrderCommon;
import pb.order.stats.MutinyOrderTotalRevenueByMerchantGrpc;
import pb.order.stats.OrderTotalRevenue;

@GrpcService
@Singleton
public class OrderTotalRevenueByMerchantGrpcHandler
        extends MutinyOrderTotalRevenueByMerchantGrpc.OrderTotalRevenueByMerchantImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<OrderCommon.ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenueByMerchant(OrderTotalRevenue.FindYearMonthTotalRevenueByMerchant request) {
        return repo.findMonthlyTotalRevenueByMerchant(request.getYear(), request.getMonth(), request.getMerchantId())
                .map(rows -> {
                    var b = OrderCommon.ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly total revenue by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(OrderCommon.OrderMonthlyTotalRevenueResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setMonth(strOf(r, "month"))
                                .setOrderCount(intOf(r, "order_count"))
                                .setTotalRevenue(longOf(r, "total_revenue"))
                                .setTotalItemsSold(intOf(r, "total_items_sold"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<OrderCommon.ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenueByMerchant(OrderTotalRevenue.FindYearTotalRevenueByMerchant request) {
        return repo.findYearlyTotalRevenueByMerchant(request.getYear(), request.getMerchantId())
                .map(rows -> {
                    var b = OrderCommon.ApiResponseOrderYearlyTotalRevenue.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly total revenue by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(OrderCommon.OrderYearlyTotalRevenueResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setOrderCount(intOf(r, "order_count"))
                                .setTotalRevenue(longOf(r, "total_revenue"))
                                .setTotalItemsSold(intOf(r, "total_items_sold"))
                                .setActiveCashiers(intOf(r, "active_cashiers"))
                                .setUniqueProductsSold(intOf(r, "unique_products_sold"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
