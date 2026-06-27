ALTER TABLE purchase_orders
    ADD COLUMN total_amount NUMERIC(12,2),
    ADD COLUMN ordered_at  TIMESTAMPTZ;
