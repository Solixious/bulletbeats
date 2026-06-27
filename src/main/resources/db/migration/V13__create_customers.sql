CREATE TABLE customers (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(150) NOT NULL,
  phone           VARCHAR(20) NOT NULL,
  email           VARCHAR(150),
  dob             DATE,
  visit_count     INT NOT NULL DEFAULT 0,
  total_spend     NUMERIC(12,2) NOT NULL DEFAULT 0,
  loyalty_points  INT NOT NULL DEFAULT 0,
  is_vip          BOOLEAN NOT NULL DEFAULT FALSE,
  notes_count     INT NOT NULL DEFAULT 0,  -- denormalized for quick display
  tenant_id       BIGINT NOT NULL DEFAULT 1,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by      BIGINT,
  updated_by      BIGINT
);
CREATE UNIQUE INDEX idx_customers_phone_unique ON customers(phone);
CREATE INDEX idx_customers_name ON customers(lower(name));
CREATE INDEX idx_customers_is_vip ON customers(is_vip) WHERE is_vip = TRUE;
