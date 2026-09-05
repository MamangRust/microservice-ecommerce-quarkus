package com.sanedge.order.repository.statsbymerchant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sanedge.order.domain.requests.MonthOrderMerchantRequest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@Disabled("Native SQL contains PostgreSQL functions; verify compatibility before enabling")
@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class OrderSoldOutByMerchantRepositoryTest {

    @Inject
    OrderSoldOutByMerchantRepository orderSoldOutByMerchantRepository;

    @Test
    @WithSession
    Uni<Void> testFindMonthlyOrdersByMerchant_ReturnsTwoEntries() {
        MonthOrderMerchantRequest req = new MonthOrderMerchantRequest();
        req.setMerchantId(1);
        req.setYear(2023);
        req.setMonth(12);

        return orderSoldOutByMerchantRepository
                .findMonthlyOrdersByMerchant(req)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(2);
                    assertThat(result.get(0).getOrderCount()).isZero();
                    assertThat(result.get(0).getTotalRevenue()).isZero();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindYearlyOrdersByMerchant_ReturnsFiveEntries() {
        return orderSoldOutByMerchantRepository
                .findYearlyOrdersByMerchant(1, 2024)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(5);
                    assertThat(result.get(0).getOrderCount()).isZero();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindMonthlyOrdersByMerchant_ReturnsNonNullResult() {
        MonthOrderMerchantRequest req = new MonthOrderMerchantRequest();
        req.setMerchantId(1);
        req.setYear(2024);
        req.setMonth(6);

        return orderSoldOutByMerchantRepository
                .findMonthlyOrdersByMerchant(req)
                .invoke(result -> assertThat(result).isNotNull())
                .replaceWithVoid();
    }
}