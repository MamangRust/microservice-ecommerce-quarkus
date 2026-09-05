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
import pb.category.CategoryCommon;
import pb.category.stats.MutinyCategoryTotalPriceServiceGrpc;

@GrpcService
@Singleton
public class CategoryTotalPriceGrpcHandler
        extends MutinyCategoryTotalPriceServiceGrpc.CategoryTotalPriceServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<CategoryCommon.ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPrices(CategoryCommon.FindYearMonthTotalPrices request) {
        return repo.findMonthlyCategoryTotalPrice(request.getYear(), request.getMonth())
                .map(rows -> {
                    var b = CategoryCommon.ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly total category prices");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CategoryCommon.CategoriesMonthlyTotalPriceResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setMonth(strOf(r, "month"))
                                .setTotalRevenue(intOf(r, "total_revenue"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<CategoryCommon.ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPrices(CategoryCommon.FindYearTotalPrices request) {
        return repo.findYearlyCategoryTotalPrice(request.getYear())
                .map(rows -> {
                    var b = CategoryCommon.ApiResponseCategoryYearlyTotalPrice.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly total category prices");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CategoryCommon.CategoriesYearlyTotalPriceResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalRevenue(intOf(r, "total_revenue"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
