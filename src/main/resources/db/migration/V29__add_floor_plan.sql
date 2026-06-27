CREATE TABLE floors (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    width         INT NOT NULL DEFAULT 1200,
    height        INT NOT NULL DEFAULT 700,
    tenant_id     BIGINT NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO floors (name, display_order, width, height)
VALUES ('Ground Floor', 0, 1200, 700);

ALTER TABLE cafe_tables
    ADD COLUMN floor_id   BIGINT REFERENCES floors(id),
    ADD COLUMN x_pos      INT NOT NULL DEFAULT 100,
    ADD COLUMN y_pos      INT NOT NULL DEFAULT 100,
    ADD COLUMN seat_count INT NOT NULL DEFAULT 4,
    ADD COLUMN is_counter BOOLEAN NOT NULL DEFAULT FALSE;

-- Distribute existing tables in a 4-column grid on the default floor
WITH numbered AS (
    SELECT id, (ROW_NUMBER() OVER (ORDER BY id) - 1) AS rn FROM cafe_tables
)
UPDATE cafe_tables SET
    floor_id = (SELECT id FROM floors ORDER BY id LIMIT 1),
    x_pos    = (numbered.rn % 4) * 260 + 80,
    y_pos    = (numbered.rn / 4) * 200 + 80
FROM numbered WHERE cafe_tables.id = numbered.id;
