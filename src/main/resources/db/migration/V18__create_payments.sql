CREATE TABLE payments (
  id             BIGSERIAL PRIMARY KEY,
  bill_id        BIGINT NOT NULL REFERENCES bills(id),
  amount         NUMERIC(10,2) NOT NULL,
  method         VARCHAR(20) NOT NULL,
  reference_note VARCHAR(255),
  paid_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by     BIGINT
);
CREATE INDEX idx_payments_bill ON payments(bill_id);
