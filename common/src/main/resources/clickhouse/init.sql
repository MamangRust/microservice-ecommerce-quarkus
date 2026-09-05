-- ClickHouse schema init for ecommerce stats pipeline
-- Run once during deployment: clickhouse-client < init.sql

CREATE DATABASE IF NOT EXISTS ecommerce_stats;

CREATE TABLE IF NOT EXISTS ecommerce_stats.order_daily
(
    event_id      String,
    occurred_at   DateTime,
    order_id      String,
    merchant_id   String,
    status        LowCardinality(String),
    total_amount  Decimal(18,2),
    event_version UInt64
) ENGINE = ReplacingMergeTree(event_version)
ORDER BY (toDate(occurred_at), order_id, event_id)
TTL toDate(occurred_at) + INTERVAL 2 YEAR;

CREATE TABLE IF NOT EXISTS ecommerce_stats.order_item_daily
(
    event_id      String,
    occurred_at   DateTime,
    order_item_id String,
    order_id      String,
    merchant_id   String,
    category_id   String,
    product_id    String,
    quantity      UInt32,
    unit_price    Decimal(18,2),
    subtotal      Decimal(18,2),
    event_version UInt64
) ENGINE = ReplacingMergeTree(event_version)
ORDER BY (toDate(occurred_at), order_item_id, event_id)
TTL toDate(occurred_at) + INTERVAL 2 YEAR;

CREATE TABLE IF NOT EXISTS ecommerce_stats.transaction_daily
(
    event_id       String,
    occurred_at    DateTime,
    transaction_id String,
    order_id       String,
    merchant_id    String,
    payment_method LowCardinality(String),
    status         LowCardinality(String),
    amount         Decimal(18,2),
    event_version  UInt64
) ENGINE = ReplacingMergeTree(event_version)
ORDER BY (toDate(occurred_at), transaction_id, event_id)
TTL toDate(occurred_at) + INTERVAL 2 YEAR;
