CREATE TABLE stock_movements (
  id               BIGSERIAL PRIMARY KEY,
  grocery_item_id  BIGINT NOT NULL REFERENCES grocery_items(id),
  movement_type    VARCHAR(20) NOT NULL,
  quantity         NUMERIC(12,3) NOT NULL,
  stock_before     NUMERIC(12,3) NOT NULL,
  stock_after      NUMERIC(12,3) NOT NULL,
  reference_type   VARCHAR(30),
  reference_id     BIGINT,
  notes            TEXT,
  created_by       BIGINT,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_stock_movements_grocery_item ON stock_movements(grocery_item_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at DESC);
