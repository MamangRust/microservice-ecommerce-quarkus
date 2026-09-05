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
import pb.category.stats.MutinyCategoryPriceByMerchantGrpc;

@GrpcService
@Singleton
public class CategoryPriceByMerchantGrpcHandler
        extends MutinyCategoryPriceByMerchantGrpc.CategoryPriceByMerchantImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<CategoryCommon.ApiResponseCategoryMonthPrice> findMonthPriceByMerchant(CategoryCommon.FindYearCategoryByMerchant request) {
        return repo.findMonthlyCategoryPriceByMerchant(request.getYear(), request.getMerchantId())
                .map(rows -> {
                    var b = CategoryCommon.ApiResponseCategoryMonthPrice.newBuilder()
                            .setStatus("success").setMessage("Retrieved monthly category prices by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CategoryCommon.CategoryMonthPriceResponse.newBuilder()
                                .setMonth(strOf(r, "month"))
                                .setCategoryId(intOf(r, "category_id"))
                                .setCategoryName(strOf(r, "category_name"))
                                .setOrderCount(intOf(r, "order_count"))
                                .setItemsSold(intOf(r, "items_sold"))
                                .setTotalRevenue(intOf(r, "total_revenue"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<CategoryCommon.ApiResponseCategoryYearPrice> findYearPriceByMerchant(CategoryCommon.FindYearCategoryByMerchant request) {
        return repo.findYearlyCategoryPriceByMerchant(request.getYear(), request.getMerchantId())
                .map(rows -> {
                    var b = CategoryCommon.ApiResponseCategoryYearPrice.newBuilder()
                            .setStatus("success").setMessage("Retrieved yearly category prices by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CategoryCommon.CategoryYearPriceResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setCategoryId(intOf(r, "category_id"))
                                .setCategoryName(strOf(r, "category_name"))
                                .setOrderCount(intOf(r, "order_count"))
                                .setItemsSold(intOf(r, "items_sold"))
                                .setTotalRevenue(intOf(r, "total_revenue"))
                                .setUniqueProductsSold(intOf(r, "unique_products_sold"))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
