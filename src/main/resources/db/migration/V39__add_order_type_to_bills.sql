-- Make cafe_table_id nullable (non-dine-in bills have no table)
ALTER TABLE bills
    ALTER COLUMN cafe_table_id DROP NOT NULL;

-- Order type and optional online platform
ALTER TABLE bills
    ADD COLUMN order_type         VARCHAR(20) NOT NULL DEFAULT 'DINE_IN',
    ADD COLUMN online_order_platform VARCHAR(20);
