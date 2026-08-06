CREATE TABLE feedback_requests (
  id                  BIGSERIAL     PRIMARY KEY,
  bill_id             BIGINT        NOT NULL REFERENCES bills(id),
  customer_id         BIGINT        REFERENCES customers(id),
  phone               VARCHAR(30)   NOT NULL,
  status              VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
  requested_at        TIMESTAMPTZ   NOT NULL,
  expires_at          TIMESTAMPTZ   NOT NULL,
  response_body       TEXT,
  responded_at        TIMESTAMPTZ,
  response_message_id BIGINT        REFERENCES whatsapp_messages(id),
  created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
  created_by          BIGINT,
  updated_by          BIGINT
);
CREATE INDEX idx_feedback_requests_bill ON feedback_requests(bill_id);
CREATE INDEX idx_feedback_requests_phone_status ON feedback_requests(phone, status);

INSERT INTO app_config(key, value)
VALUES ('feedback.window_minutes', '60')
ON CONFLICT (key) DO NOTHING;
