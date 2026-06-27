CREATE TABLE bills (
  id              BIGSERIAL PRIMARY KEY,
  bill_number     VARCHAR(30) UNIQUE NOT NULL,
  cafe_table_id   BIGINT NOT NULL REFERENCES cafe_tables(id),
  customer_id     BIGINT REFERENCES customers(id),
  status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  discount_type   VARCHAR(10),
  discount_value  NUMERIC(10,2),
  subtotal        NUMERIC(10,2) NOT NULL DEFAULT 0,
  discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
  taxable_amount  NUMERIC(10,2) NOT NULL DEFAULT 0,
  gst_rate        NUMERIC(5,2)  NOT NULL DEFAULT 0,
  gst_amount      NUMERIC(10,2) NOT NULL DEFAULT 0,
  total_amount    NUMERIC(10,2) NOT NULL DEFAULT 0,
  notes           TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by      BIGINT,
  updated_by      BIGINT,
  tenant_id       BIGINT NOT NULL DEFAULT 1
);
CREATE INDEX idx_bills_cafe_table ON bills(cafe_table_id);
CREATE INDEX idx_bills_customer   ON bills(customer_id);
CREATE INDEX idx_bills_status     ON bills(status);
CREATE INDEX idx_bills_created_at ON bills(created_at DESC);

CREATE SEQUENCE bill_daily_seq START 1;

CREATE TABLE bill_items (
  id           BIGSERIAL PRIMARY KEY,
  bill_id      BIGINT NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
  menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
  item_name    VARCHAR(150) NOT NULL,
  unit_price   NUMERIC(10,2) NOT NULL,
  quantity     INT NOT NULL DEFAULT 1,
  line_total   NUMERIC(10,2) NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_bill_items_bill ON bill_items(bill_id);
