ALTER TABLE bills ADD COLUMN delivery_fee NUMERIC(10,2) NOT NULL DEFAULT 0;

INSERT INTO app_config(key, value)
VALUES ('delivery.fee', '0.00')
ON CONFLICT (key) DO NOTHING;
