CREATE TABLE suppliers (
  id            BIGSERIAL PRIMARY KEY,
  name          VARCHAR(150) NOT NULL,
  phone         VARCHAR(20),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id     BIGINT NOT NULL DEFAULT 1,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    BIGINT,
  updated_by    BIGINT
);
CREATE INDEX idx_suppliers_name ON suppliers(lower(name));
