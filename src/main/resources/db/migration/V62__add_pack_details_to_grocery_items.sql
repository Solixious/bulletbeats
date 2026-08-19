ALTER TABLE grocery_items
    ADD COLUMN brand VARCHAR(100),
    ADD COLUMN pack_cost NUMERIC(12, 2),
    ADD COLUMN pack_quantity NUMERIC(12, 3),
    ADD COLUMN pack_unit VARCHAR(30),
    ADD COLUMN minor_unit VARCHAR(30);
