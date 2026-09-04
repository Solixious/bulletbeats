-- "While you wait" entertainment list (games, books/comics, etc.) shown to customers
-- on the QR/delivery order-confirmed screen while they wait for their order.
CREATE TABLE waiting_activities (
    id          BIGSERIAL PRIMARY KEY,
    category    VARCHAR(20) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

INSERT INTO waiting_activities (category, name, sort_order) VALUES
    ('GAME', 'Rubik''s Cube', 0),
    ('GAME', 'UNO', 1),
    ('GAME', 'Monopoly', 2),
    ('GAME', 'Chess', 3),
    ('GAME', 'Jenga', 4);

INSERT INTO app_config(key, value) VALUES
    ('waiting.enabled', 'true'),
    ('waiting.teaser_message', 'Bored while you wait? We''ve got games to keep you entertained!'),
    ('waiting.help_message', 'Can''t find something on the list? Just ask a staff member — we''re happy to help!')
ON CONFLICT (key) DO NOTHING;
