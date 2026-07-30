ALTER TABLE categories ADD COLUMN parent_id BIGINT REFERENCES categories(id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);

DROP INDEX idx_categories_name_unique;
CREATE UNIQUE INDEX idx_categories_name_unique_top ON categories (lower(name)) WHERE parent_id IS NULL;
CREATE UNIQUE INDEX idx_categories_name_unique_sub ON categories (lower(name), parent_id) WHERE parent_id IS NOT NULL;
