CREATE TABLE bill_sequence (
  bill_date DATE PRIMARY KEY,
  last_seq  INT NOT NULL DEFAULT 0
);

ALTER TABLE loyalty_transactions
  ADD CONSTRAINT fk_loyalty_bill
  FOREIGN KEY (bill_id) REFERENCES bills(id);

-- Seed runtime-configurable app settings:
INSERT INTO app_config(key, value) VALUES
  ('gst.rate',                   '18.00'),
  ('loyalty.earn_rate',          '10.00'),
  ('cafe.name',                  'Bullet Beats Café'),
  ('cafe.address',               ''),
  ('app.base-url',               'http://localhost:8080'),
  ('table.idle.timeout.minutes', '15')
ON CONFLICT (key) DO NOTHING;
