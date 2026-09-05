package com.sanedge.order.repository.stats;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.order.entity.Order;
import com.sanedge.order.entity.OrderMonthly;
import com.sanedge.order.entity.OrderYearly;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderSoldOutRepository implements PanacheRepository<Order> {

    public Uni<List<OrderMonthly>> findMonthlyOrders(Integer year, Integer month) {
        java.time.LocalDate currentMonth = java.time.LocalDate.of(year, month, 1);
        java.time.LocalDate prevMonthDate = currentMonth.minusMonths(1);

        Integer year1 = year;
        Integer month1 = month;
        Integer year2 = prevMonthDate.getYear();
        Integer month2 = prevMonthDate.getMonthValue();

        String sql = """
            WITH
                date_ranges AS (
                    SELECT
                        MAKE_DATE(:year1, :month1, 1)::timestamp AS range1_start,
                        (MAKE_DATE(:year1, :month1, 1) + INTERVAL '1 month')::timestamp AS range1_end,
                        MAKE_DATE(:year2, :month2, 1)::timestamp AS range2_start,
                        (MAKE_DATE(:year2, :month2, 1) + INTERVAL '1 month')::timestamp AS range2_end
                ),
                all_months AS (
                    SELECT range1_start AS activity_month FROM date_ranges
                    UNION
                    SELECT range2_start FROM date_ranges
                ),
                monthly_orders AS (
                    SELECT
                        DATE_TRUNC('month', o.created_at) AS activity_month,
                        CAST(COUNT(o.id) AS INTEGER) AS order_count,
                        CAST(SUM(o.total_price) AS BIGINT) AS total_revenue,
                        CAST(SUM(oi.quantity) AS INTEGER) AS total_items_sold
                    FROM orders o
                    JOIN order_items oi ON o.id = oi.order_id
                    JOIN date_ranges dr ON (
                        (o.created_at >= dr.range1_start AND o.created_at < dr.range1_end)
                        OR
                        (o.created_at >= dr.range2_start AND o.created_at < dr.range2_end)
                    )
                    WHERE o.deleted_at IS NULL
                      AND oi.deleted_at IS NULL
                    GROUP BY DATE_TRUNC('month', o.created_at)
                )
            SELECT
                TO_CHAR(am.activity_month, 'Mon') AS "month",
                CAST(COALESCE(mo.order_count, 0) AS INTEGER) AS orderCount,
                CAST(COALESCE(mo.total_revenue, 0) AS BIGINT) AS totalRevenue,
                CAST(COALESCE(mo.total_items_sold, 0) AS INTEGER) AS totalItemsSold
            FROM all_months am
            LEFT JOIN monthly_orders mo
                   ON am.activity_month = mo.activity_month
            ORDER BY am.activity_month
            """;

        return Panache.getSession().chain(session -> {
            var dataQuery = session.createNativeQuery(sql);
            dataQuery.setParameter("year1", year1);
            dataQuery.setParameter("month1", month1);
            dataQuery.setParameter("year2", year2);
            dataQuery.setParameter("month2", month2);

            return dataQuery.getResultList().map(results -> {
                List<OrderMonthly> list = new ArrayList<>();
                for (Object item : results) {
                    Object[] row = (Object[]) item;
                    OrderMonthly om = new OrderMonthly();
                    om.setMonth((String) row[0]);
                    om.setOrderCount(((Number) row[1]).intValue());
                    om.setTotalRevenue(((Number) row[2]).longValue());
                    om.setTotalItemsSold(((Number) row[3]).intValue());
                    list.add(om);
                }
                return list;
            });
        });
    }

    public Uni<List<OrderYearly>> findYearlyOrders(Integer year) {
        String sql = """
            WITH last_five_years AS (
                SELECT
                    CAST(EXTRACT(YEAR FROM o.created_at) AS VARCHAR) AS "year",
                    CAST(COUNT(o.id) AS INTEGER) AS order_count,
                    CAST(SUM(o.total_price) AS BIGINT) AS total_revenue,
                    CAST(SUM(oi.quantity) AS INTEGER) AS total_items_sold,
                    CAST(COUNT(DISTINCT o.user_id) AS INTEGER) AS active_cashiers,
                    CAST(COUNT(DISTINCT oi.product_id) AS INTEGER) AS unique_products_sold
                FROM orders o
                JOIN order_items oi ON o.id = oi.order_id
                WHERE o.deleted_at IS NULL
                  AND oi.deleted_at IS NULL
                  AND EXTRACT(YEAR FROM o.created_at) BETWEEN :year - 4 AND :year
                GROUP BY EXTRACT(YEAR FROM o.created_at)
            )
            SELECT
                "year" AS "year",
                order_count AS orderCount,
                total_revenue AS totalRevenue,
                total_items_sold AS totalItemsSold,
                active_cashiers AS activeCashiers,
                unique_products_sold AS uniqueProductsSold
            FROM last_five_years
            ORDER BY "year"
            """;

        return Panache.getSession().chain(session -> {
            var dataQuery = session.createNativeQuery(sql);
            dataQuery.setParameter("year", year);
            return dataQuery.getResultList().map(results -> {
                List<OrderYearly> list = new ArrayList<>();
                for (Object item : results) {
                    Object[] row = (Object[]) item;
                    OrderYearly oy = new OrderYearly();
                    oy.setYear((String) row[0]);
                    oy.setOrderCount(((Number) row[1]).intValue());
                    oy.setTotalRevenue(((Number) row[2]).longValue());
                    oy.setTotalItemsSold(((Number) row[3]).intValue());
                    oy.setActiveCashiers(((Number) row[4]).intValue());
                    oy.setUniqueProductsSold(((Number) row[5]).intValue());
                    list.add(oy);
                }
                return list;
            });
        });
    }
}
