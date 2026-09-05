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
import pb.order.stats.MutinyOrderRevenueServiceGrpc;
import pb.order.stats.OrderRevenue;

@GrpcService
@Singleton
public class OrderRevenueGrpcHandler
        extends MutinyOrderRevenueServiceGrpc.OrderRevenueServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<OrderCommon.ApiResponseOrderMonthly> findMonthlyRevenue(OrderRevenue.FindYearOrder request) {
        return repo.findMonthlyOrderRevenue(request.getYear(), request.getMonth())
                .map(rows -> {
                    var b = OrderCommon.ApiResponseOrderMonthly.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly order revenue");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(OrderCommon.OrderMonthlyResponse.newBuilder()
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
    public Uni<OrderCommon.ApiResponseOrderYearly> findYearlyRevenue(OrderRevenue.FindYearOrder request) {
        return repo.findYearlyOrderRevenue(request.getYear())
                .map(rows -> {
                    var b = OrderCommon.ApiResponseOrderYearly.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly order revenue");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(OrderCommon.OrderYearlyResponse.newBuilder()
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
