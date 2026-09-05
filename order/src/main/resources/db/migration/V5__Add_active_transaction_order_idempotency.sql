-- Prevent duplicate active transactions for the same order.
-- Trashed transaction history remains allowed to coexist with a new active attempt.
-- Legacy duplicate active rows must be reconciled before this migration is applied.
-- NOTE: The ecommerce_transaction.transactions table may not exist yet (cross-schema),
-- so we skip this migration gracefully if it doesn't.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'ecommerce_transaction' AND table_name = 'transactions'
    ) THEN
        RAISE NOTICE 'ecommerce_transaction.transactions does not exist yet, skipping idempotency index creation';
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ecommerce_transaction.transactions
        WHERE "deleted_at" IS NULL
          AND LOWER("payment_status") <> 'failed'
        GROUP BY "order_id"
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot create active transaction idempotency index: duplicate active order_id rows exist';
    END IF;
END $$;

-- Only create if the table exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'ecommerce_transaction' AND table_name = 'transactions'
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS "ux_transactions_active_order_id"
            ON ecommerce_transaction.transactions ("order_id")
            WHERE "deleted_at" IS NULL
              AND LOWER("payment_status") <> 'failed';
    END IF;
END $$;
