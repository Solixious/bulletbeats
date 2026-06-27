CREATE TABLE price_history (
  id           BIGSERIAL PRIMARY KEY,
  menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
  old_price    NUMERIC(10,2) NOT NULL,
  new_price    NUMERIC(10,2) NOT NULL,
  changed_by   BIGINT NOT NULL,
  changed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_price_history_menu_item ON price_history(menu_item_id);

CREATE TABLE menu_item_availability_log (
  id              BIGSERIAL PRIMARY KEY,
  menu_item_id    BIGINT NOT NULL REFERENCES menu_items(id),
  changed_by      BIGINT NOT NULL,
  changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  override_before BOOLEAN,
  override_after  BOOLEAN,
  reason          TEXT
);
CREATE INDEX idx_availability_log_menu_item ON menu_item_availability_log(menu_item_id);
