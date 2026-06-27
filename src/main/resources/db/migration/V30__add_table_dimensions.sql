ALTER TABLE cafe_tables
    ADD COLUMN IF NOT EXISTS table_width  INT NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS table_height INT NOT NULL DEFAULT 68,
    ADD COLUMN IF NOT EXISTS rotation     INT NOT NULL DEFAULT 0;

-- Restore the wider visual size for existing counter rows
UPDATE cafe_tables SET table_width = 200, table_height = 52 WHERE is_counter = TRUE;
