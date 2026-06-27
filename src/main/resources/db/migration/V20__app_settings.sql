-- Expose GST inclusive/exclusive mode as a runtime config
INSERT INTO app_config(key, value)
VALUES ('gst.inclusive', 'false')
ON CONFLICT (key) DO NOTHING;

-- Store the GST mode on each bill so historical bills render correctly
ALTER TABLE bills
    ADD COLUMN IF NOT EXISTS gst_inclusive BOOLEAN NOT NULL DEFAULT FALSE;
