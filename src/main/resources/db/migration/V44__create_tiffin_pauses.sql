CREATE TABLE tiffin_pauses (
  id              BIGSERIAL    PRIMARY KEY,
  subscription_id BIGINT       NOT NULL REFERENCES tiffin_subscriptions(id),
  pause_from      DATE         NOT NULL,
  pause_until     DATE,
  note            VARCHAR(255),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      BIGINT,
  updated_by      BIGINT
);
CREATE INDEX idx_tiffin_pauses_sub ON tiffin_pauses(subscription_id);
