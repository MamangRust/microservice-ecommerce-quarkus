-- Stub tables for transaction stats repository tests.
-- Transaction entity creates the `transactions` table. Stats SQL uses CTE date_ranges
-- and may reference transactions only. Create minimal stub for orders since some
-- native queries use JOINs in broader context.

CREATE TABLE IF NOT EXISTS "orders" (
    "id" SERIAL PRIMARY KEY,
    "user_id" INT NOT NULL DEFAULT 0,
    "merchant_id" INT NOT NULL DEFAULT 0,
    "total_price" INT NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "order_items" (
    "id" SERIAL PRIMARY KEY,
    "order_id" INT NOT NULL DEFAULT 0,
    "product_id" INT NOT NULL DEFAULT 0,
    "quantity" INT NOT NULL DEFAULT 0,
    "price" INT NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP
);
