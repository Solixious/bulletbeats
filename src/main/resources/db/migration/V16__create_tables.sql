CREATE TABLE cafe_tables (
  id            BIGSERIAL PRIMARY KEY,
  name          VARCHAR(50) NOT NULL,
  capacity      INT,
  status        VARCHAR(20) NOT NULL DEFAULT 'FREE',
  qr_code       VARCHAR(100),
  qr_image_path VARCHAR(255),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  tenant_id     BIGINT NOT NULL DEFAULT 1,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    BIGINT,
  updated_by    BIGINT
);
CREATE UNIQUE INDEX idx_cafe_tables_name_unique ON cafe_tables(lower(name));
CREATE UNIQUE INDEX idx_cafe_tables_qr_code ON cafe_tables(qr_code);
CREATE INDEX idx_cafe_tables_status ON cafe_tables(status);
