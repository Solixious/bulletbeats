CREATE TABLE online_platforms (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  is_active   BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id   BIGINT NOT NULL DEFAULT 1,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by  BIGINT,
  updated_by  BIGINT
);
CREATE UNIQUE INDEX idx_online_platforms_name_unique ON online_platforms(lower(name));

INSERT INTO online_platforms (name, is_active) VALUES ('Zomato', true);

ALTER TABLE bills ADD COLUMN online_platform_id BIGINT REFERENCES online_platforms(id);

-- Defensive backfill: if any existing bill already references an enum value
-- other than ZOMATO (e.g. SWIGGY), create a matching (inactive-by-default,
-- since it wasn't officially "on") platform row so no bill loses its link.
INSERT INTO online_platforms (name, is_active)
SELECT DISTINCT initcap(lower(b.online_order_platform)), false
FROM bills b
WHERE b.online_order_platform IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM online_platforms p WHERE lower(p.name) = lower(b.online_order_platform));

UPDATE bills b
SET online_platform_id = p.id
FROM online_platforms p
WHERE b.online_order_platform IS NOT NULL AND lower(p.name) = lower(b.online_order_platform);

ALTER TABLE bills DROP COLUMN online_order_platform;

CREATE TABLE menu_item_platform_prices (
  id           BIGSERIAL PRIMARY KEY,
  menu_item_id BIGINT NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
  platform_id  BIGINT NOT NULL REFERENCES online_platforms(id) ON DELETE CASCADE,
  price        NUMERIC(10,2) NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by   BIGINT,
  updated_by   BIGINT
);
CREATE UNIQUE INDEX idx_menu_item_platform_price_unique ON menu_item_platform_prices(menu_item_id, platform_id);
