-- Create merchants table
CREATE TABLE IF NOT EXISTS "merchants" (
    "id" BIGINT PRIMARY KEY,
    "user_id" BIGINT NOT NULL REFERENCES "users" ("id") ON DELETE CASCADE,
    "name" VARCHAR(255) NOT NULL,
    "description" TEXT,
    "address" TEXT,
    "contact_email" VARCHAR(100),
    "contact_phone" VARCHAR(20),
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_merchants_user_id" ON "merchants" ("user_id");
CREATE INDEX "idx_merchants_status" ON "merchants" ("status");
CREATE INDEX "idx_merchants_created_at" ON "merchants" ("created_at");
