CREATE TABLE categories (
  id            BIGSERIAL PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  description   TEXT,
  display_order INT NOT NULL DEFAULT 0,
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id     BIGINT NOT NULL DEFAULT 1,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    BIGINT,
  updated_by    BIGINT
);
CREATE UNIQUE INDEX idx_categories_name_unique ON categories(lower(name));
CREATE INDEX idx_categories_display_order ON categories(display_order);
