INSERT INTO app_config(key, value)
  VALUES ('table.idle.timeout.minutes', '10')
  ON CONFLICT (key) DO NOTHING;

-- Also seed base-url if not already present
INSERT INTO app_config(key, value)
  VALUES ('app.base-url', 'http://localhost:8080')
  ON CONFLICT (key) DO NOTHING;
