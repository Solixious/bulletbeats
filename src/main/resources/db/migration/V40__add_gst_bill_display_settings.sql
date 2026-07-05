INSERT INTO app_config(key, value)
VALUES ('bill.show_gst', 'true'),
       ('cafe.gstin', '')
ON CONFLICT (key) DO NOTHING;
