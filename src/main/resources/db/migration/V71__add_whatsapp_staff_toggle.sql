INSERT INTO app_config(key, value)
VALUES ('notification.whatsapp.staff.enabled', 'true')
ON CONFLICT (key) DO NOTHING;
