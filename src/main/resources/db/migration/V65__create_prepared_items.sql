-- Pre-prepped items (e.g. frozen momo, ginger-garlic paste) made in-house from grocery items,
-- carrying their own stock, and usable as an ingredient in dish/combo recipes alongside grocery items.
CREATE TABLE prepared_items (
  id                    BIGSERIAL PRIMARY KEY,
  name                  VARCHAR(150) NOT NULL,
  description           TEXT,
  prep_time_minutes     INT,
  unit                  VARCHAR(30) NOT NULL,
  minor_unit            VARCHAR(30),
  batch_yield_quantity  NUMERIC(12,3) NOT NULL,
  quantity_in_stock     NUMERIC(12,3) NOT NULL DEFAULT 0,
  min_threshold         NUMERIC(12,3) NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id             BIGINT NOT NULL DEFAULT 1,
  version               BIGINT NOT NULL DEFAULT 0,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by            BIGINT,
  updated_by            BIGINT
);
CREATE UNIQUE INDEX idx_prepared_items_name_unique ON prepared_items(lower(name));

-- The prep recipe: what grocery items (and how much of each) go into one batch of a prepared item.
CREATE TABLE prepared_item_ingredients (
  id                 BIGSERIAL PRIMARY KEY,
  prepared_item_id   BIGINT NOT NULL REFERENCES prepared_items(id) ON DELETE CASCADE,
  grocery_item_id    BIGINT NOT NULL REFERENCES grocery_items(id),
  quantity_required  NUMERIC(10,3) NOT NULL,
  UNIQUE(prepared_item_id, grocery_item_id)
);
CREATE INDEX idx_prepared_item_ingredients_prepared ON prepared_item_ingredients(prepared_item_id);
CREATE INDEX idx_prepared_item_ingredients_grocery  ON prepared_item_ingredients(grocery_item_id);

CREATE TABLE prepared_item_stock_movements (
  id                 BIGSERIAL PRIMARY KEY,
  prepared_item_id   BIGINT NOT NULL REFERENCES prepared_items(id),
  movement_type      VARCHAR(20) NOT NULL,
  quantity           NUMERIC(12,3) NOT NULL,
  stock_before       NUMERIC(12,3) NOT NULL,
  stock_after        NUMERIC(12,3) NOT NULL,
  reference_type     VARCHAR(30),
  reference_id       BIGINT,
  notes              TEXT,
  created_by         BIGINT,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_prepared_item_stock_movements_item ON prepared_item_stock_movements(prepared_item_id);

-- Dish/combo recipe lines can now point at either a grocery item or a prepared item.
ALTER TABLE dish_ingredients
  ALTER COLUMN grocery_item_id DROP NOT NULL,
  ADD COLUMN prepared_item_id BIGINT REFERENCES prepared_items(id),
  ADD CONSTRAINT chk_dish_ingredients_one_source CHECK (
    (grocery_item_id IS NOT NULL AND prepared_item_id IS NULL) OR
    (grocery_item_id IS NULL AND prepared_item_id IS NOT NULL)
  ),
  ADD CONSTRAINT uq_dish_ingredients_prepared_item UNIQUE (dish_id, prepared_item_id);
CREATE INDEX idx_dish_ingredients_prepared ON dish_ingredients(prepared_item_id);

ALTER TABLE combo_ingredients
  ALTER COLUMN grocery_item_id DROP NOT NULL,
  ADD COLUMN prepared_item_id BIGINT REFERENCES prepared_items(id),
  ADD CONSTRAINT chk_combo_ingredients_one_source CHECK (
    (grocery_item_id IS NOT NULL AND prepared_item_id IS NULL) OR
    (grocery_item_id IS NULL AND prepared_item_id IS NOT NULL)
  ),
  ADD CONSTRAINT uq_combo_ingredients_prepared_item UNIQUE (combo_id, prepared_item_id);
CREATE INDEX idx_combo_ingredients_prepared ON combo_ingredients(prepared_item_id);
