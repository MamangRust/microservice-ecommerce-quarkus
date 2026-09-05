-- Prevent duplicate active transactions for the same order.
-- Trashed transaction history remains allowed to coexist with a new active attempt.
-- Legacy duplicate active rows must be reconciled before this migration is applied.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM "transactions"
        WHERE "deleted_at" IS NULL
          AND LOWER("payment_status") <> 'failed'
        GROUP BY "order_id"
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot create active transaction idempotency index: duplicate active order_id rows exist';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS "ux_transactions_active_order_id"
    ON "transactions" ("order_id")
    WHERE "deleted_at" IS NULL
      AND LOWER("payment_status") <> 'failed';
