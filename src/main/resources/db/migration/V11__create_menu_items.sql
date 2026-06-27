CREATE TABLE combos (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(150) NOT NULL,
  description TEXT,
  is_active   BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id   BIGINT NOT NULL DEFAULT 1,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by  BIGINT,
  updated_by  BIGINT
);
CREATE UNIQUE INDEX idx_combos_name_unique ON combos(lower(name));

CREATE TABLE combo_ingredients (
  id                BIGSERIAL PRIMARY KEY,
  combo_id          BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
  grocery_item_id   BIGINT NOT NULL REFERENCES grocery_items(id),
  quantity_required NUMERIC(10,3) NOT NULL,
  UNIQUE(combo_id, grocery_item_id)
);
CREATE INDEX idx_combo_ingredients_combo ON combo_ingredients(combo_id);

CREATE TABLE menu_items (
  id                    BIGSERIAL PRIMARY KEY,
  name                  VARCHAR(150) NOT NULL,
  category_id           BIGINT NOT NULL REFERENCES categories(id),
  dish_id               BIGINT REFERENCES dishes(id),
  combo_id              BIGINT REFERENCES combos(id),
  price                 NUMERIC(10,2) NOT NULL,
  is_available          BOOLEAN NOT NULL DEFAULT TRUE,
  availability_override BOOLEAN,
  image_path            VARCHAR(255),
  display_order         INT NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id             BIGINT NOT NULL DEFAULT 1,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by            BIGINT,
  updated_by            BIGINT,
  CONSTRAINT chk_dish_or_combo CHECK (num_nonnulls(dish_id, combo_id) = 1)
);
CREATE INDEX idx_menu_items_category ON menu_items(category_id);
CREATE INDEX idx_menu_items_dish     ON menu_items(dish_id);
CREATE INDEX idx_menu_items_combo    ON menu_items(combo_id);
