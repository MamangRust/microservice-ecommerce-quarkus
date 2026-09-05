-- Create orders table
CREATE TABLE IF NOT EXISTS "orders" (
    "id" BIGINT PRIMARY KEY,
    "user_id" BIGINT NOT NULL REFERENCES "users" ("id") ON DELETE CASCADE,
    "merchant_id" BIGINT NOT NULL REFERENCES "merchants" ("id") ON DELETE CASCADE,
    "total_price" INT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_orders_user_id" ON "orders" ("user_id");
CREATE INDEX "idx_orders_merchant_id" ON "orders" ("merchant_id");
CREATE INDEX "idx_orders_total_price" ON "orders" ("total_price");
