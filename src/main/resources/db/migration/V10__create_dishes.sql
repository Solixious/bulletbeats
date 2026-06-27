CREATE TABLE dishes (
  id                BIGSERIAL PRIMARY KEY,
  name              VARCHAR(150) NOT NULL,
  description       TEXT,
  prep_time_minutes INT,
  is_active         BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id         BIGINT NOT NULL DEFAULT 1,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by        BIGINT,
  updated_by        BIGINT
);
CREATE UNIQUE INDEX idx_dishes_name_unique ON dishes(lower(name));

CREATE TABLE dish_ingredients (
  id                BIGSERIAL PRIMARY KEY,
  dish_id           BIGINT NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
  grocery_item_id   BIGINT NOT NULL REFERENCES grocery_items(id),
  quantity_required NUMERIC(10,3) NOT NULL,
  UNIQUE(dish_id, grocery_item_id)
);
CREATE INDEX idx_dish_ingredients_dish    ON dish_ingredients(dish_id);
CREATE INDEX idx_dish_ingredients_grocery ON dish_ingredients(grocery_item_id);
