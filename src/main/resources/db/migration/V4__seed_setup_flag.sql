CREATE TABLE IF NOT EXISTS app_config (
    key        VARCHAR(100) PRIMARY KEY,
    value      TEXT         NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO app_config (key, value)
VALUES ('setup.completed', 'false')
ON CONFLICT (key) DO NOTHING;
