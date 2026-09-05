-- Create shipping_addresses table
CREATE TABLE IF NOT EXISTS "shipping_addresses" (
    "id" BIGINT PRIMARY KEY,
    "order_id" BIGINT NOT NULL REFERENCES "orders" ("id") ON DELETE CASCADE,
    "alamat" TEXT NOT NULL,
    "provinsi" VARCHAR(255) NOT NULL,
    "negara" VARCHAR(255) NOT NULL,
    "kota" VARCHAR(255) NOT NULL,
    "courier" VARCHAR(100) NOT NULL,
    "shipping_method" VARCHAR(255) NOT NULL,
    "shipping_cost" INT NOT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX "idx_shipping_addresses_order_id" ON "shipping_addresses" ("order_id");
CREATE INDEX "idx_shipping_addresses_provinsi" ON "shipping_addresses" ("provinsi");
CREATE INDEX "idx_shipping_addresses_negara" ON "shipping_addresses" ("negara");
CREATE INDEX "idx_shipping_addresses_kota" ON "shipping_addresses" ("kota");
CREATE INDEX "idx_shipping_addresses_method" ON "shipping_addresses" ("shipping_method");
