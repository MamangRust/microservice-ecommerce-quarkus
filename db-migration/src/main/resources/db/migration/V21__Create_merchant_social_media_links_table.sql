-- Create merchant_social_media_links table
CREATE TABLE IF NOT EXISTS "merchant_social_media_links" (
    "id" BIGINT PRIMARY KEY,
    "merchant_detail_id" BIGINT NOT NULL REFERENCES "merchant_details" ("id") ON DELETE CASCADE,
    "platform" VARCHAR(100) NOT NULL,
    "url" TEXT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_merchant_social_media_links_merchant_detail_id" ON "merchant_social_media_links" ("merchant_detail_id");
