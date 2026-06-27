-- Add QR session tracking to cafe_tables
ALTER TABLE cafe_tables
  ADD COLUMN last_scanned_at TIMESTAMPTZ,
  ADD COLUMN idle_timeout_minutes INT NOT NULL DEFAULT 10;
  -- idle_timeout_minutes overrides global config per table if needed
