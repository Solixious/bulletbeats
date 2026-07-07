CREATE TABLE tiffin_pricing (
  id              BIGSERIAL    PRIMARY KEY,
  meal_type       VARCHAR(10)  NOT NULL UNIQUE,
  price_per_month NUMERIC(10,2) NOT NULL DEFAULT 0.00,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      BIGINT,
  updated_by      BIGINT
);
