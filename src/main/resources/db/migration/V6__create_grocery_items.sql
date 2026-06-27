CREATE TABLE grocery_items (
  id                 BIGSERIAL PRIMARY KEY,
  name               VARCHAR(150) NOT NULL,
  unit               VARCHAR(30) NOT NULL,
  quantity_in_stock  NUMERIC(12,3) NOT NULL DEFAULT 0,
  min_threshold      NUMERIC(12,3) NOT NULL DEFAULT 0,
  reorder_quantity   NUMERIC(12,3) NOT NULL DEFAULT 0,
  supplier_id        BIGINT REFERENCES suppliers(id),
  is_active          BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id          BIGINT NOT NULL DEFAULT 1,
  version            BIGINT NOT NULL DEFAULT 0,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by         BIGINT,
  updated_by         BIGINT
);
CREATE INDEX idx_grocery_items_name ON grocery_items(lower(name));
CREATE INDEX idx_grocery_items_unit ON grocery_items(lower(unit));
CREATE UNIQUE INDEX idx_grocery_items_name_unique ON grocery_items(lower(name));
