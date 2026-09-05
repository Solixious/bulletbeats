CREATE TABLE inventory_categories (
  id            BIGSERIAL PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  tenant_id     BIGINT NOT NULL DEFAULT 1,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    BIGINT,
  updated_by    BIGINT
);
CREATE UNIQUE INDEX idx_inventory_categories_name_unique ON inventory_categories(lower(name));
CREATE INDEX idx_inventory_categories_display_order ON inventory_categories(display_order);

ALTER TABLE grocery_items ADD COLUMN category_id BIGINT REFERENCES inventory_categories(id);
CREATE INDEX idx_grocery_items_category_id ON grocery_items(category_id);
