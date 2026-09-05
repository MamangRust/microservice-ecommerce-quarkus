package com.sanedge.statsreader.repository;

import com.sanedge.common.clickhouse.ClickHouseClient;
import com.sanedge.statsreader.cache.StatsCache;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Generic ClickHouse reader for ecommerce stats. All queries run over the HTTP
 * interface with {@code FORMAT JSON} (added automatically by
 * {@link ClickHouseClient#queryJson(String)}) and are parsed into
 * {@link JsonArray} rows.
 *
 * <p>Query results are cached (cache-aside) under the {@code apigw:stats:}
 * namespace via {@link StatsCache}; cache misses or Redis outages fall back to
 * ClickHouse transparently.
 */
@ApplicationScoped
public class ClickHouseStatsRepository {

    @Inject
    ClickHouseClient clickHouse;

    @Inject
    StatsCache statsCache;

    // ========== Category Price Stats ==========

    public Uni<JsonArray> findMonthlyCategoryPrice(int year) {
        String sql = "SELECT formatDateTime(occurred_at, '%b') AS month, "
                + "category_id AS category_id, '' AS category_name, "
                + "count(DISTINCT order_id) AS order_count, "
                + "sum(quantity) AS items_sold, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " GROUP BY month, category_id, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at), category_id";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyCategoryPrice(int year) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "category_id AS category_id, '' AS category_name, "
                + "count(DISTINCT order_id) AS order_count, "
                + "sum(quantity) AS items_sold, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue, "
                + "count(DISTINCT product_id) AS unique_products_sold "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " GROUP BY year, category_id ORDER BY category_id";
        return query(sql);
    }

    public Uni<JsonArray> findMonthlyCategoryPriceByMerchant(int year, int merchantId) {
        String sql = "SELECT formatDateTime(occurred_at, '%b') AS month, "
                + "category_id AS category_id, '' AS category_name, "
                + "count(DISTINCT order_id) AS order_count, "
                + "sum(quantity) AS items_sold, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND merchant_id = '" + merchantId + "'"
                + " GROUP BY month, category_id, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at), category_id";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyCategoryPriceByMerchant(int year, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "category_id AS category_id, '' AS category_name, "
                + "count(DISTINCT order_id) AS order_count, "
                + "sum(quantity) AS items_sold, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue, "
                + "count(DISTINCT product_id) AS unique_products_sold "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND merchant_id = '" + merchantId + "'"
                + " GROUP BY year, category_id ORDER BY category_id";
        return query(sql);
    }

    public Uni<JsonArray> findMonthlyCategoryPriceById(int year, int categoryId) {
        String sql = "SELECT formatDateTime(occurred_at, '%b') AS month, "
                + "category_id AS category_id, '' AS category_name, "
                + "count(DISTINCT order_id) AS order_count, "
                + "sum(quantity) AS items_sold, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND category_id = '" + categoryId + "'"
                + " GROUP BY month, category_id, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at), category_id";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyCategoryPriceById(int year, int categoryId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "category_id AS category_id, '' AS category_name, "
                + "count(DISTINCT order_id) AS order_count, "
                + "sum(quantity) AS items_sold, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue, "
                + "count(DISTINCT product_id) AS unique_products_sold "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND category_id = '" + categoryId + "'"
                + " GROUP BY year, category_id ORDER BY category_id";
        return query(sql);
    }

    // ========== Category Total Price Stats ==========

    public Uni<JsonArray> findMonthlyCategoryTotalPrice(int year, int month) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "formatDateTime(occurred_at, '%b') AS month, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND toMonth(occurred_at) = " + month
                + " GROUP BY year, month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyCategoryTotalPrice(int year) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    public Uni<JsonArray> findMonthlyCategoryTotalPriceById(int year, int month, int categoryId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "formatDateTime(occurred_at, '%b') AS month, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND toMonth(occurred_at) = " + month
                + " AND category_id = '" + categoryId + "'"
                + " GROUP BY year, month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyCategoryTotalPriceById(int year, int categoryId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND category_id = '" + categoryId + "'"
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    public Uni<JsonArray> findMonthlyCategoryTotalPriceByMerchant(int year, int month, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "formatDateTime(occurred_at, '%b') AS month, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND toMonth(occurred_at) = " + month
                + " AND merchant_id = '" + merchantId + "'"
                + " GROUP BY year, month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyCategoryTotalPriceByMerchant(int year, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "cast(sum(subtotal) AS Int32) AS total_revenue "
                + "FROM order_item_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND merchant_id = '" + merchantId + "'"
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    // ========== Order Revenue Stats ==========

    public Uni<JsonArray> findMonthlyOrderRevenue(int year, int month) {
        String sql = "SELECT formatDateTime(occurred_at, '%b') AS month, "
                + "count() AS order_count, "
                + "cast(sum(total_amount) AS Int64) AS total_revenue, "
                + "cast(sum(total_amount) AS Int32) AS total_items_sold "
                + "FROM order_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND toMonth(occurred_at) = " + month
                + " AND status = 'completed'"
                + " GROUP BY month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyOrderRevenue(int year) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "count() AS order_count, "
                + "cast(sum(total_amount) AS Int64) AS total_revenue, "
                + "cast(sum(total_amount) AS Int32) AS total_items_sold, "
                + "0 AS active_cashiers, "
                + "0 AS unique_products_sold "
                + "FROM order_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = 'completed'"
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    public Uni<JsonArray> findMonthlyOrderRevenueByMerchant(int year, int month, int merchantId) {
        String sql = "SELECT formatDateTime(occurred_at, '%b') AS month, "
                + "count() AS order_count, "
                + "cast(sum(total_amount) AS Int64) AS total_revenue, "
                + "cast(sum(total_amount) AS Int32) AS total_items_sold "
                + "FROM order_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND toMonth(occurred_at) = " + month
                + " AND merchant_id = '" + merchantId + "'"
                + " AND status = 'completed'"
                + " GROUP BY month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyOrderRevenueByMerchant(int year, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "count() AS order_count, "
                + "cast(sum(total_amount) AS Int64) AS total_revenue, "
                + "cast(sum(total_amount) AS Int32) AS total_items_sold, "
                + "0 AS active_cashiers, "
                + "0 AS unique_products_sold "
                + "FROM order_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND merchant_id = '" + merchantId + "'"
                + " AND status = 'completed'"
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    // ========== Order Total Revenue Stats ==========

    public Uni<JsonArray> findMonthlyTotalRevenue(int year, int month) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "formatDateTime(occurred_at, '%b') AS month, "
                + "count() AS order_count, "
                + "cast(sum(total_amount) AS Int64) AS total_revenue, "
                + "cast(sum(total_amount) AS Int32) AS total_items_sold "
                + "FROM order_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND toMonth(occurred_at) = " + month
                + " AND status = 'completed'"
                + " GROUP BY year, month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyTotalRevenue(int year) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "count() AS order_count, "
                + "cast(sum(total_amount) AS Int64) AS total_revenue, "
                + "cast(sum(total_amount) AS Int32) AS total_items_sold, "
                + "0 AS active_cashiers, "
                + "0 AS unique_products_sold "
                + "FROM order_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = 'completed'"
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    public Uni<JsonArray> findMonthlyTotalRevenueByMerchant(int year, int month, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "formatDateTime(occurred_at, '%b') AS month, "
                + "count() AS order_count, "
                + "cast(sum(total_amount) AS Int64) AS total_revenue, "
                + "cast(sum(total_amount) AS Int32) AS total_items_sold "
                + "FROM order_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND toMonth(occurred_at) = " + month
                + " AND merchant_id = '" + merchantId + "'"
                + " AND status = 'completed'"
                + " GROUP BY year, month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyTotalRevenueByMerchant(int year, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "count() AS order_count, "
                + "cast(sum(total_amount) AS Int64) AS total_revenue, "
                + "cast(sum(total_amount) AS Int32) AS total_items_sold, "
                + "0 AS active_cashiers, "
                + "0 AS unique_products_sold "
                + "FROM order_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND merchant_id = '" + merchantId + "'"
                + " AND status = 'completed'"
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    // ========== Transaction Amount Stats ==========

    public Uni<JsonArray> findMonthlyTransactionAmountByStatus(int year, String status) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "formatDateTime(occurred_at, '%b') AS month, "
                + "count() AS total_transactions, "
                + "cast(sum(amount) AS Int32) AS total_amount "
                + "FROM transaction_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = '" + status + "'"
                + " GROUP BY year, month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyTransactionAmountByStatus(int year, String status) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "count() AS total_transactions, "
                + "cast(sum(amount) AS Int32) AS total_amount "
                + "FROM transaction_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = '" + status + "'"
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    public Uni<JsonArray> findMonthlyTransactionAmountByStatusMerchant(int year, String status, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "formatDateTime(occurred_at, '%b') AS month, "
                + "count() AS total_transactions, "
                + "cast(sum(amount) AS Int32) AS total_amount "
                + "FROM transaction_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = '" + status + "'"
                + " AND merchant_id = '" + merchantId + "'"
                + " GROUP BY year, month, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at)";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyTransactionAmountByStatusMerchant(int year, String status, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "count() AS total_transactions, "
                + "cast(sum(amount) AS Int32) AS total_amount "
                + "FROM transaction_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = '" + status + "'"
                + " AND merchant_id = '" + merchantId + "'"
                + " GROUP BY year ORDER BY year";
        return query(sql);
    }

    // ========== Transaction Method Stats ==========

    public Uni<JsonArray> findMonthlyTransactionMethodByStatus(int year, String status) {
        String sql = "SELECT formatDateTime(occurred_at, '%b') AS month, "
                + "payment_method AS method, "
                + "count() AS total_transactions, "
                + "cast(sum(amount) AS Int32) AS total_amount "
                + "FROM transaction_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = '" + status + "'"
                + " GROUP BY month, method, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at), method";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyTransactionMethodByStatus(int year, String status) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "payment_method AS method, "
                + "count() AS total_transactions, "
                + "cast(sum(amount) AS Int32) AS total_amount "
                + "FROM transaction_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = '" + status + "'"
                + " GROUP BY year, method ORDER BY year, method";
        return query(sql);
    }

    public Uni<JsonArray> findMonthlyTransactionMethodByStatusMerchant(int year, String status, int merchantId) {
        String sql = "SELECT formatDateTime(occurred_at, '%b') AS month, "
                + "payment_method AS method, "
                + "count() AS total_transactions, "
                + "cast(sum(amount) AS Int32) AS total_amount "
                + "FROM transaction_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = '" + status + "'"
                + " AND merchant_id = '" + merchantId + "'"
                + " GROUP BY month, method, toMonth(occurred_at) "
                + "ORDER BY toMonth(occurred_at), method";
        return query(sql);
    }

    public Uni<JsonArray> findYearlyTransactionMethodByStatusMerchant(int year, String status, int merchantId) {
        String sql = "SELECT toString(toYear(occurred_at)) AS year, "
                + "payment_method AS method, "
                + "count() AS total_transactions, "
                + "cast(sum(amount) AS Int32) AS total_amount "
                + "FROM transaction_daily "
                + "WHERE toYear(occurred_at) = " + year
                + " AND status = '" + status + "'"
                + " AND merchant_id = '" + merchantId + "'"
                + " GROUP BY year, method ORDER BY year, method";
        return query(sql);
    }

    // ========== Raw query helper ==========

    /**
     * Executes a SQL query against ClickHouse (appends FORMAT JSON) and
     * returns the {@code data} JsonArray. Results are cached (cache-aside)
     * with fail-open behavior.
     */
    public Uni<JsonArray> query(String sql) {
        String key = "query:" + StatsCache.hash(sql);
        return statsCache.cacheArray(key, () -> clickHouse.queryJson(sql));
    }
}
