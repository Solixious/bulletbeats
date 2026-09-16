UPDATE customers c
SET last_visit_date = latest.last_paid_at
FROM (
  SELECT customer_id, MAX(created_at) AS last_paid_at
  FROM bills
  WHERE status = 'PAID' AND customer_id IS NOT NULL
  GROUP BY customer_id
) latest
WHERE c.id = latest.customer_id
  AND c.last_visit_date IS NULL;
