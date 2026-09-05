-- Allow a prepared item's own recipe to consume another prepared item as an ingredient
-- (e.g. "frozen veg momo" made from a "veg momo filling" prepared item), alongside grocery items.
ALTER TABLE prepared_item_ingredients
  ALTER COLUMN grocery_item_id DROP NOT NULL,
  ADD COLUMN ingredient_prepared_item_id BIGINT REFERENCES prepared_items(id),
  ADD CONSTRAINT chk_prepared_item_ingredients_one_source CHECK (
    (grocery_item_id IS NOT NULL AND ingredient_prepared_item_id IS NULL) OR
    (grocery_item_id IS NULL AND ingredient_prepared_item_id IS NOT NULL)
  ),
  ADD CONSTRAINT chk_prepared_item_ingredients_no_self_reference CHECK (
    ingredient_prepared_item_id IS DISTINCT FROM prepared_item_id
  ),
  ADD CONSTRAINT uq_prepared_item_ingredients_ingredient_prepared
    UNIQUE (prepared_item_id, ingredient_prepared_item_id);

CREATE INDEX idx_prepared_item_ingredients_ingredient_prepared
  ON prepared_item_ingredients(ingredient_prepared_item_id);
