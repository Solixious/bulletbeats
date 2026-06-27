CREATE TABLE loyalty_transactions (
  id               BIGSERIAL PRIMARY KEY,
  customer_id      BIGINT NOT NULL REFERENCES customers(id),
  points           INT NOT NULL,       -- positive=earn, negative=redeem (future)
  transaction_type VARCHAR(20) NOT NULL,  -- EARN, REDEEM, ADJUST
  bill_id          BIGINT,             -- FK to bills (wired in billing module)
  description      TEXT,               -- human readable e.g. "Earned from Bill #042"
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by       BIGINT
  -- Immutable — no updated_at
);
CREATE INDEX idx_loyalty_txn_customer ON loyalty_transactions(customer_id);
CREATE INDEX idx_loyalty_txn_bill ON loyalty_transactions(bill_id);
