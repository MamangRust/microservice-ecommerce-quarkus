-- Stub table for cross-module SQL JOIN in order stats tests.
-- Hibernate ORM (drop-and-create) creates only the `orders` table from Order entity.
-- Stats SQL JOINs `order_items`. This IF NOT EXISTS adds stub columns needed.

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
