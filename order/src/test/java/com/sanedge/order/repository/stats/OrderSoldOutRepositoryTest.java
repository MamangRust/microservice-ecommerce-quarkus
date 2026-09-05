package com.sanedge.order.repository.stats;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
class OrderSoldOutRepositoryTest {

    @Inject
    OrderSoldOutRepository orderSoldOutRepository;

    @Test
    @WithSession
    Uni<Void> testFindMonthlyOrders_ReturnsTwoEntries() {
        return orderSoldOutRepository
                .findMonthlyOrders(2023, 12)
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
    Uni<Void> testFindYearlyOrders_ReturnsFiveEntries() {
        return orderSoldOutRepository
                .findYearlyOrders(2024)
                .invoke(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(5);
                    assertThat(result.get(0).getOrderCount()).isZero();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindMonthlyOrders_ReturnsNonNullResult() {
        return orderSoldOutRepository
                .findMonthlyOrders(2024, 6)
                .invoke(result -> assertThat(result).isNotNull())
                .replaceWithVoid();
    }
}