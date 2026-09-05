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
import pb.category.stats.MutinyCategoryTotalPriceByMerchantGrpc;

@GrpcService
@Singleton
public class CategoryTotalPriceByMerchantGrpcHandler
        extends MutinyCategoryTotalPriceByMerchantGrpc.CategoryTotalPriceByMerchantImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<CategoryCommon.ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPricesByMerchant(CategoryCommon.FindYearMonthTotalPriceByMerchant request) {
        return repo.findMonthlyCategoryTotalPriceByMerchant(request.getYear(), request.getMonth(), request.getMerchantId())
                .map(rows -> {
                    var b = CategoryCommon.ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly total category prices by merchant");
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
    public Uni<CategoryCommon.ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPricesByMerchant(CategoryCommon.FindYearTotalPriceByMerchant request) {
        return repo.findYearlyCategoryTotalPriceByMerchant(request.getYear(), request.getMerchantId())
                .map(rows -> {
                    var b = CategoryCommon.ApiResponseCategoryYearlyTotalPrice.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly total category prices by merchant");
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
