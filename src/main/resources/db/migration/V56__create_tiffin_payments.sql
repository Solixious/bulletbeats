CREATE TABLE tiffin_payments (
  id              BIGSERIAL     PRIMARY KEY,
  subscription_id BIGINT        NOT NULL REFERENCES tiffin_subscriptions(id),
  amount_paid     NUMERIC(10,2) NOT NULL,
  coverage_from   DATE          NOT NULL,
  coverage_until  DATE          NOT NULL,
  paid_on         DATE          NOT NULL,
  note            VARCHAR(255),
  created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  created_by      BIGINT,
  updated_by      BIGINT
);
CREATE INDEX idx_tiffin_payments_sub ON tiffin_payments(subscription_id);
