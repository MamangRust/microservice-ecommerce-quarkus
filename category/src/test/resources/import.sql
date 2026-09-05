-- Stub tables for cross-module SQL JOINs in category stats repository tests.
-- Hibernate ORM (drop-and-create) creates only the `categories` table from Category entity.
-- Stats SQL references: orders, order_items, products. These IF NOT EXISTS create stub tables
-- so the SQL parses and executes without "relation does not exist" errors.

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

CREATE TABLE IF NOT EXISTS "products" (
    "id" SERIAL PRIMARY KEY,
    "merchant_id" INT NOT NULL DEFAULT 0,
    "category_id" INT NOT NULL DEFAULT 0,
    "name" VARCHAR(255) NOT NULL DEFAULT '',
    "price" INT NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP
);
