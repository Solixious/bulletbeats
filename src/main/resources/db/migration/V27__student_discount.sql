-- Add student flag and discount counter to customers
ALTER TABLE customers
  ADD COLUMN is_student BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN student_discount_count INT NOT NULL DEFAULT 0;

-- Add student discount fields to bills
ALTER TABLE bills
  ADD COLUMN student_discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
  ADD COLUMN student_discount_applied BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN student_discount_applied_by BIGINT,
  ADD COLUMN student_discount_applied_at TIMESTAMPTZ;

-- Add student discount config keys
INSERT INTO app_config(key, value) VALUES
  ('student.discount.percentage', '10.00'),
  ('student.discount.min_bill_amount', '200.00')
ON CONFLICT (key) DO NOTHING;
