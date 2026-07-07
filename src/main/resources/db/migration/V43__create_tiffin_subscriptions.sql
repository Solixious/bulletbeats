CREATE TABLE tiffin_subscriptions (
  id            BIGSERIAL    PRIMARY KEY,
  customer_id   BIGINT       NOT NULL REFERENCES customers(id),
  status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
  has_breakfast BOOLEAN      NOT NULL DEFAULT FALSE,
  has_lunch     BOOLEAN      NOT NULL DEFAULT FALSE,
  has_dinner    BOOLEAN      NOT NULL DEFAULT FALSE,
  all_days      BOOLEAN      NOT NULL DEFAULT TRUE,
  custom_days   VARCHAR(50),
  start_date    DATE         NOT NULL,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by    BIGINT,
  updated_by    BIGINT
);
CREATE INDEX idx_tiffin_sub_customer ON tiffin_subscriptions(customer_id);
CREATE INDEX idx_tiffin_sub_status   ON tiffin_subscriptions(status);
