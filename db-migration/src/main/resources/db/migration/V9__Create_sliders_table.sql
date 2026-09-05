-- Create sliders table
CREATE TABLE IF NOT EXISTS "sliders" (
    "id" BIGINT PRIMARY KEY,
    "name" VARCHAR(255) NOT NULL,
    "image" VARCHAR(255) NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_sliders_name" ON "sliders" ("name");
