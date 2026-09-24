-- Daily promo rotation: every customer lives in one of 7 buckets (1 = Monday ... 7 = Sunday).
-- Backfill spreads customers with a PAID bill evenly first, then the rest, so both groups are balanced.
ALTER TABLE customers ADD COLUMN promo_bucket SMALLINT;

WITH ranked AS (
    SELECT c.id,
           ROW_NUMBER() OVER (
               ORDER BY EXISTS (SELECT 1 FROM bills b WHERE b.customer_id = c.id AND b.status = 'PAID') DESC,
                        c.id
           ) - 1 AS rn
    FROM customers c
)
UPDATE customers c
SET promo_bucket = (r.rn % 7) + 1
FROM ranked r
WHERE r.id = c.id;

ALTER TABLE customers ALTER COLUMN promo_bucket SET NOT NULL;
ALTER TABLE customers ADD CONSTRAINT chk_customers_promo_bucket CHECK (promo_bucket BETWEEN 1 AND 7);
CREATE INDEX idx_customers_promo_bucket ON customers (promo_bucket);
