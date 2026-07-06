ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS notification_preference VARCHAR(10) NOT NULL DEFAULT 'WHATSAPP';

INSERT INTO app_config(key, value)
VALUES ('notification.enabled', 'false')
ON CONFLICT (key) DO NOTHING;
