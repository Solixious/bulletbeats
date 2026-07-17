ALTER TABLE tiffin_subscriptions
  ADD COLUMN custom_monthly_price NUMERIC(10,2),
  ADD COLUMN delivery_charge NUMERIC(10,2) NOT NULL DEFAULT 0.00;
