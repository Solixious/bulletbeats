INSERT INTO tiffin_pricing (meal_type, price_per_month)
VALUES ('BREAKFAST', 2200.00),
       ('LUNCH',     2200.00),
       ('DINNER',    2200.00)
ON CONFLICT (meal_type) DO NOTHING;
