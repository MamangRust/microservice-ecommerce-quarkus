-- Create merchant_details table
CREATE TABLE IF NOT EXISTS "merchant_details" (
    "id" BIGINT PRIMARY KEY,
    "merchant_id" BIGINT NOT NULL REFERENCES "merchants" ("id") ON DELETE CASCADE,
    "display_name" VARCHAR(255),
    "cover_image_url" VARCHAR(255),
    "logo_url" VARCHAR(255),
    "short_description" TEXT,
    "website_url" VARCHAR(255),
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_merchant_details_merchant" ON "merchant_details" ("merchant_id");
