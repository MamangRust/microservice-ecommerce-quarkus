package com.sanedge.statsbackfill;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.clickhouse.ClickHouseClient;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class BackfillLifecycle {
    private static final Logger log = LoggerFactory.getLogger(BackfillLifecycle.class);

    @Inject ClickHouseClient clickhouse;
    @Inject Pool pool;

    @ConfigProperty(name = "backfill.domains", defaultValue = "order,transaction") String domainsConfig;
    @ConfigProperty(name = "backfill.from-date", defaultValue = "2024-01-01") String fromDate;

    void onStart(@Observes StartupEvent ev) {
        log.info("=== Stats Backfill starting ===");
        Set<String> domains = Arrays.stream(domainsConfig.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        backfillOrders(domains)
                .chain(() -> backfillTransactions(domains))
                .invoke(() -> { log.info("=== Stats Backfill completed ==="); Quarkus.asyncExit(0); })
                .onFailure().invoke(err -> { log.error("Backfill FAILED", err); Quarkus.asyncExit(1); })
                .subscribe().with(item -> {}, failure -> {});
    }

    private Uni<Void> backfillOrders(Set<String> domains) {
        if (!domains.contains("order")) return Uni.createFrom().voidItem();
        String sql = "SELECT \"id\", \"merchant_id\", \"total_price\", \"created_at\" "
                + "FROM ecommerce_order.orders WHERE \"deleted_at\" IS NULL AND \"created_at\" >= $1 ORDER BY \"id\"";
        AtomicInteger count = new AtomicInteger(0);
        return pool.preparedQuery(sql).execute(io.vertx.mutiny.sqlclient.Tuple.of(fromDate + " 00:00:00"))
                .chain(rows -> {
                    if (rows.rowCount() == 0) return Uni.createFrom().voidItem();
                    StringBuilder batchSql = new StringBuilder("INSERT INTO ecommerce_stats.order_daily (event_id, occurred_at, order_id, merchant_id, status, total_amount, event_version) FORMAT TabSeparated\n");
                    for (var row : rows) {
                        String eventId = "backfill:order:" + row.getLong("id");
                        String occurredAt = ClickHouseClient.normalizeDateTime(row.getLocalDateTime("created_at"));
                        batchSql.append(String.join("\t", eventId, occurredAt != null ? occurredAt : "",
                                String.valueOf(row.getLong("id")), String.valueOf(row.getLong("merchant_id")),
                                "completed", String.valueOf(row.getLong("total_price")),
                                String.valueOf(System.currentTimeMillis()))).append("\n");
                        count.incrementAndGet();
                    }
                    return clickhouse.execute(batchSql.toString());
                })
                .invoke(() -> log.info("Backfilled {} order records", count.get()));
    }

    private Uni<Void> backfillTransactions(Set<String> domains) {
        if (!domains.contains("transaction")) return Uni.createFrom().voidItem();
        String sql = "SELECT \"id\", \"order_id\", \"merchant_id\", \"payment_method\", \"amount\", \"payment_status\", \"created_at\" "
                + "FROM ecommerce_transaction.transactions WHERE \"deleted_at\" IS NULL AND \"created_at\" >= $1 ORDER BY \"id\"";
        AtomicInteger count = new AtomicInteger(0);
        return pool.preparedQuery(sql).execute(io.vertx.mutiny.sqlclient.Tuple.of(fromDate + " 00:00:00"))
                .chain(rows -> {
                    if (rows.rowCount() == 0) return Uni.createFrom().voidItem();
                    StringBuilder batchSql = new StringBuilder("INSERT INTO ecommerce_stats.transaction_daily (event_id, occurred_at, transaction_id, order_id, merchant_id, payment_method, status, amount, event_version) FORMAT TabSeparated\n");
                    for (var row : rows) {
                        String eventId = "backfill:transaction:" + row.getLong("id");
                        String occurredAt = ClickHouseClient.normalizeDateTime(row.getLocalDateTime("created_at"));
                        batchSql.append(String.join("\t", eventId, occurredAt != null ? occurredAt : "",
                                String.valueOf(row.getLong("id")), String.valueOf(row.getLong("order_id")),
                                String.valueOf(row.getLong("merchant_id")), row.getString("payment_method"),
                                row.getString("payment_status"), String.valueOf(row.getLong("amount")),
                                String.valueOf(System.currentTimeMillis()))).append("\n");
                        count.incrementAndGet();
                    }
                    return clickhouse.execute(batchSql.toString());
                })
                .invoke(() -> log.info("Backfilled {} transaction records", count.get()));
    }
}
