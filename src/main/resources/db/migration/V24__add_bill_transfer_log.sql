-- Add transfer tracking to bills
ALTER TABLE bills
  ADD COLUMN transferred_from_table_id BIGINT REFERENCES cafe_tables(id),
  ADD COLUMN transferred_at TIMESTAMPTZ;
