-- Create transactions table
CREATE TABLE IF NOT EXISTS "transactions" (
    "id" BIGINT PRIMARY KEY,
    "order_id" BIGINT NOT NULL REFERENCES "orders" ("id") ON DELETE CASCADE,
    "merchant_id" BIGINT NOT NULL REFERENCES "merchants" ("id") ON DELETE CASCADE,
    "payment_method" VARCHAR(50) NOT NULL,
    "amount" INT NOT NULL,
    "payment_status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_transactions_order_id" ON "transactions" ("order_id");
CREATE INDEX "idx_transactions_merchant_id" ON "transactions" ("merchant_id");
CREATE INDEX "idx_transactions_payment_status" ON "transactions" ("payment_status");
