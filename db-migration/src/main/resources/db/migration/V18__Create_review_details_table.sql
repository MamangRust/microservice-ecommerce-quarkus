-- Create review_details table
CREATE TABLE IF NOT EXISTS "review_details" (
    "id" BIGINT PRIMARY KEY,
    "review_id" BIGINT NOT NULL REFERENCES "reviews" ("id") ON DELETE CASCADE,
    "type" VARCHAR(20) NOT NULL CHECK ("type" IN ('photo', 'video')),
    "url" TEXT NOT NULL,
    "caption" TEXT,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_review_details_review_id" ON "review_details" ("review_id");
