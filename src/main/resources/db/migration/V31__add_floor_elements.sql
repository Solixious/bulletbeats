CREATE TABLE floor_elements (
    id           BIGSERIAL PRIMARY KEY,
    floor_id     BIGINT      NOT NULL REFERENCES floors(id) ON DELETE CASCADE,
    element_type VARCHAR(30) NOT NULL DEFAULT 'CASH_COUNTER',
    label        VARCHAR(50) NOT NULL DEFAULT 'CASH',
    x_pos        INT         NOT NULL DEFAULT 60,
    y_pos        INT         NOT NULL DEFAULT 60
);
