CREATE TABLE bill_activity_log (
  id            BIGSERIAL PRIMARY KEY,
  bill_id       BIGINT NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
  actor_type    VARCHAR(20) NOT NULL,  -- STAFF, CUSTOMER, SYSTEM
  actor_name    VARCHAR(150),          -- staff username or customer name/phone
  message       TEXT NOT NULL,         -- human readable log entry
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
  -- Immutable — no updated_at, no created_by FK
  -- actor identified by actor_type + actor_name directly
);
CREATE INDEX idx_activity_log_bill ON bill_activity_log(bill_id);
CREATE INDEX idx_activity_log_created ON bill_activity_log(created_at DESC);
