CREATE TABLE customer_notes (
  id           BIGSERIAL PRIMARY KEY,
  customer_id  BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  note         TEXT NOT NULL,
  created_by   BIGINT NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
  -- Append-only: no updated_at, no deleted_at
);
CREATE INDEX idx_customer_notes_customer ON customer_notes(customer_id);
