INSERT INTO app_config(key, value)
VALUES ('delivery.hours.enabled', 'false'),
       ('delivery.closed.start', '00:00'),
       ('delivery.closed.end', '00:00')
ON CONFLICT (key) DO NOTHING;
