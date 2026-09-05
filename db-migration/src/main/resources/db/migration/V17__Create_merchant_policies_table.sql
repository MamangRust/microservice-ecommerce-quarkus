-- Create merchant_policies table
CREATE TABLE IF NOT EXISTS "merchant_policies" (
    "id" BIGINT PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL REFERENCES "merchants" ("id") ON DELETE CASCADE,
    "policy_type" VARCHAR(100) NOT NULL,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_policies_merchant_id" ON "merchant_policies" ("merchant_id");
CREATE INDEX "idx_policies_policy_type" ON "merchant_policies" ("policy_type");
