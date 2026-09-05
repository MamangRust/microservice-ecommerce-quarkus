-- Create carts table
CREATE TABLE IF NOT EXISTS "carts" (
    "id" BIGINT PRIMARY KEY,
    "user_id" BIGINT NOT NULL REFERENCES "users" ("id") ON DELETE CASCADE,
    "product_id" BIGINT NOT NULL REFERENCES "products" ("id") ON DELETE CASCADE,
    "name" VARCHAR(255) NOT NULL,
    "price" INT NOT NULL,
    "image" VARCHAR(255) NOT NULL,
    "quantity" INT NOT NULL,
    "weight" INT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_carts_user_id" ON "carts" ("user_id");
CREATE INDEX "idx_carts_product_id" ON "carts" ("product_id");
CREATE INDEX "idx_carts_name" ON "carts" ("name");
