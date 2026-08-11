-- Customer cohorts (rule-based customer segments) ---------------------------

CREATE TABLE customer_cohorts (
  id          BIGSERIAL     PRIMARY KEY,
  name        VARCHAR(150)  NOT NULL,
  description TEXT,
  built_in    BOOLEAN       NOT NULL DEFAULT FALSE,
  active      BOOLEAN       NOT NULL DEFAULT TRUE,
  tenant_id   BIGINT        NOT NULL DEFAULT 1,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  created_by  BIGINT,
  updated_by  BIGINT
);

CREATE TABLE customer_cohort_rules (
  id          BIGSERIAL     PRIMARY KEY,
  cohort_id   BIGINT        NOT NULL REFERENCES customer_cohorts(id) ON DELETE CASCADE,
  field       VARCHAR(25)   NOT NULL,
  operator    VARCHAR(5)    NOT NULL,
  value       NUMERIC(12,2) NOT NULL,
  period_days INT
);
CREATE INDEX idx_cohort_rules_cohort ON customer_cohort_rules(cohort_id);

-- Offers -----------------------------------------------------------------

CREATE TABLE offers (
  id                     BIGSERIAL     PRIMARY KEY,
  name                   VARCHAR(150)  NOT NULL,
  description            TEXT,
  mechanism              VARCHAR(20)   NOT NULL,
  percentage_value       NUMERIC(5,2),
  fixed_value            NUMERIC(10,2),
  buy_quantity           INT,
  get_quantity           INT,
  target_type            VARCHAR(10)   NOT NULL DEFAULT 'ALL',
  cohort_id              BIGINT        REFERENCES customer_cohorts(id),
  customer_id            BIGINT        REFERENCES customers(id),
  min_spend              NUMERIC(10,2) NOT NULL DEFAULT 0,
  starts_at              TIMESTAMPTZ,
  ends_at                TIMESTAMPTZ,
  requires_code          BOOLEAN       NOT NULL DEFAULT FALSE,
  max_total_uses         INT,
  max_uses_per_customer  INT,
  active                 BOOLEAN       NOT NULL DEFAULT TRUE,
  is_system              BOOLEAN       NOT NULL DEFAULT FALSE,
  legacy_student_discount BOOLEAN      NOT NULL DEFAULT FALSE,
  tenant_id              BIGINT        NOT NULL DEFAULT 1,
  created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
  created_by             BIGINT,
  updated_by             BIGINT
);
CREATE INDEX idx_offers_active ON offers(active);
CREATE INDEX idx_offers_cohort ON offers(cohort_id);
CREATE INDEX idx_offers_customer ON offers(customer_id);

CREATE TABLE offer_item_scopes (
  id            BIGSERIAL PRIMARY KEY,
  offer_id      BIGINT    NOT NULL REFERENCES offers(id) ON DELETE CASCADE,
  menu_item_id  BIGINT    REFERENCES menu_items(id),
  category_id   BIGINT    REFERENCES categories(id)
);
CREATE INDEX idx_offer_item_scopes_offer ON offer_item_scopes(offer_id);

CREATE TABLE offer_codes (
  id          BIGSERIAL     PRIMARY KEY,
  offer_id    BIGINT        NOT NULL REFERENCES offers(id) ON DELETE CASCADE,
  code        VARCHAR(40)   NOT NULL,
  customer_id BIGINT        REFERENCES customers(id),
  usage_type  VARCHAR(15)   NOT NULL,
  max_uses    INT,
  uses_count  INT           NOT NULL DEFAULT 0,
  active      BOOLEAN       NOT NULL DEFAULT TRUE,
  expires_at  TIMESTAMPTZ,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  created_by  BIGINT,
  updated_by  BIGINT
);
CREATE UNIQUE INDEX idx_offer_codes_code_unique ON offer_codes(upper(code));
CREATE INDEX idx_offer_codes_offer ON offer_codes(offer_id);

CREATE TABLE offer_redemptions (
  id               BIGSERIAL     PRIMARY KEY,
  offer_id         BIGINT        NOT NULL REFERENCES offers(id),
  offer_code_id    BIGINT        REFERENCES offer_codes(id),
  bill_id          BIGINT,
  customer_id      BIGINT        REFERENCES customers(id),
  discount_amount  NUMERIC(10,2) NOT NULL,
  applied_by       BIGINT,
  applied_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
  -- Immutable — no updated_at
);
CREATE INDEX idx_offer_redemptions_offer ON offer_redemptions(offer_id);
CREATE INDEX idx_offer_redemptions_customer ON offer_redemptions(customer_id);

-- Bill + Customer columns --------------------------------------------------

ALTER TABLE bills ADD COLUMN applied_offer_id BIGINT REFERENCES offers(id);
ALTER TABLE bills ADD COLUMN applied_offer_code_id BIGINT REFERENCES offer_codes(id);
ALTER TABLE bills ADD COLUMN offer_discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0;

ALTER TABLE customers ADD COLUMN last_visit_date TIMESTAMPTZ;

-- Seed built-in cohorts -----------------------------------------------------

INSERT INTO customer_cohorts (name, description, built_in, active)
VALUES
  ('All Customers', 'Every customer, no conditions', TRUE, TRUE),
  ('New Customers', 'First-time bill payers (no prior visits on file)', TRUE, TRUE),
  ('Existing Customers', 'Customers with at least one prior visit', TRUE, TRUE);

INSERT INTO customer_cohort_rules (cohort_id, field, operator, value)
SELECT id, 'TOTAL_VISITS', 'EQ', 0 FROM customer_cohorts WHERE name = 'New Customers';

INSERT INTO customer_cohort_rules (cohort_id, field, operator, value)
SELECT id, 'TOTAL_VISITS', 'GTE', 1 FROM customer_cohorts WHERE name = 'Existing Customers';

-- Seed the legacy student-discount system offer ------------------------------
-- Eligibility/pricing for this entry keeps using Customer.isStudent and the
-- student.discount.percentage/min_bill_amount app_config keys, not this row's
-- own fields — see legacy_student_discount flag handling in BillingService.

INSERT INTO offers (name, description, mechanism, target_type, requires_code, active, is_system, legacy_student_discount)
VALUES ('Student Discount', 'Legacy student discount, migrated into the offers panel', 'BILL_PERCENTAGE', 'ALL', FALSE, TRUE, TRUE, TRUE);
