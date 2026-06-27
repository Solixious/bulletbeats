CREATE TABLE replenishment_requests (
  id               BIGSERIAL PRIMARY KEY,
  grocery_item_id  BIGINT NOT NULL REFERENCES grocery_items(id),
  status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  requested_qty    NUMERIC(12,3) NOT NULL,
  notes            TEXT,
  tenant_id        BIGINT NOT NULL DEFAULT 1,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by       BIGINT,
  updated_by       BIGINT
);
CREATE INDEX idx_replenishment_grocery_item ON replenishment_requests(grocery_item_id);
CREATE INDEX idx_replenishment_status ON replenishment_requests(status);

CREATE TABLE purchase_orders (
  id                     BIGSERIAL PRIMARY KEY,
  supplier_id            BIGINT NOT NULL REFERENCES suppliers(id),
  status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  whatsapp_text          TEXT,
  expected_delivery_date DATE,
  notes                  TEXT,
  tenant_id              BIGINT NOT NULL DEFAULT 1,
  created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by             BIGINT,
  updated_by             BIGINT
);

CREATE TABLE purchase_order_items (
  id                  BIGSERIAL PRIMARY KEY,
  purchase_order_id   BIGINT NOT NULL REFERENCES purchase_orders(id),
  grocery_item_id     BIGINT NOT NULL REFERENCES grocery_items(id),
  quantity_ordered    NUMERIC(12,3) NOT NULL,
  quantity_received   NUMERIC(12,3) NOT NULL DEFAULT 0,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_po_items_po_id ON purchase_order_items(purchase_order_id);
