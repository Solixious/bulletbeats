INSERT INTO app_config(key, value)
VALUES ('social.instagram_url', ''),
       ('social.google_review_url', '')
ON CONFLICT (key) DO NOTHING;
