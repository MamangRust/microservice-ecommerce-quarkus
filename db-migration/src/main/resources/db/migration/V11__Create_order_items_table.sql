-- Create order_items table
CREATE TABLE IF NOT EXISTS "order_items" (
    "id" BIGINT PRIMARY KEY,
    "order_id" BIGINT NOT NULL REFERENCES "orders" ("id") ON DELETE CASCADE,
    "product_id" BIGINT NOT NULL REFERENCES "products" ("id") ON DELETE CASCADE,
    "quantity" INT NOT NULL,
    "price" INT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_order_items_order_id" ON "order_items" ("order_id");
