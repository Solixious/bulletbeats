CREATE TABLE units (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(30) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);
CREATE UNIQUE INDEX idx_units_name ON units (lower(name));

CREATE TABLE unit_conversions (
    id            BIGSERIAL PRIMARY KEY,
    from_unit_id  BIGINT NOT NULL REFERENCES units(id),
    to_unit_id    BIGINT NOT NULL REFERENCES units(id),
    factor        NUMERIC(18, 6) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    BIGINT,
    updated_by    BIGINT,
    UNIQUE (from_unit_id, to_unit_id)
);

-- Common starter units
INSERT INTO units (name) VALUES
    ('kg'), ('g'), ('l'), ('ml'), ('piece'), ('dozen'), ('packet'), ('box')
ON CONFLICT DO NOTHING;

-- Backfill any unit names already in use on existing grocery items
INSERT INTO units (name)
SELECT DISTINCT lower(unit) FROM grocery_items WHERE unit IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO units (name)
SELECT DISTINCT lower(pack_unit) FROM grocery_items WHERE pack_unit IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO units (name)
SELECT DISTINCT lower(minor_unit) FROM grocery_items WHERE minor_unit IS NOT NULL
ON CONFLICT DO NOTHING;

-- Common starter conversions (factor = how many "to" units make up 1 "from" unit)
INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor)
SELECT k.id, g.id, 1000 FROM units k, units g WHERE k.name = 'kg' AND g.name = 'g'
ON CONFLICT DO NOTHING;

INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor)
SELECT g.id, k.id, 0.001 FROM units k, units g WHERE k.name = 'kg' AND g.name = 'g'
ON CONFLICT DO NOTHING;

INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor)
SELECT l.id, m.id, 1000 FROM units l, units m WHERE l.name = 'l' AND m.name = 'ml'
ON CONFLICT DO NOTHING;

INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor)
SELECT m.id, l.id, 0.001 FROM units l, units m WHERE l.name = 'l' AND m.name = 'ml'
ON CONFLICT DO NOTHING;

INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor)
SELECT d.id, p.id, 12 FROM units d, units p WHERE d.name = 'dozen' AND p.name = 'piece'
ON CONFLICT DO NOTHING;

INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor)
SELECT p.id, d.id, 0.083333 FROM units d, units p WHERE d.name = 'dozen' AND p.name = 'piece'
ON CONFLICT DO NOTHING;
