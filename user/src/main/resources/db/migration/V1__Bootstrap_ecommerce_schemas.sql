-- Bootstrap all ecommerce schemas
CREATE SCHEMA IF NOT EXISTS ecommerce_identity;
CREATE SCHEMA IF NOT EXISTS ecommerce_merchant;
CREATE SCHEMA IF NOT EXISTS ecommerce_catalog;
CREATE SCHEMA IF NOT EXISTS ecommerce_order;
CREATE SCHEMA IF NOT EXISTS ecommerce_transaction;
CREATE SCHEMA IF NOT EXISTS ecommerce_content;

-- Shared Hibernate sequence for Panache entity ID generation
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 100 INCREMENT BY 50;

-- Set search_path so all native SQL (including cross-schema FK resolution)
-- can reference unqualified table names within the path.
ALTER ROLE CURRENT_USER SET search_path =
    ecommerce_identity, ecommerce_merchant, ecommerce_catalog,
    ecommerce_order, ecommerce_transaction, ecommerce_content, public;
