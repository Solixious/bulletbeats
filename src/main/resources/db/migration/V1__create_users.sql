CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name   VARCHAR(150) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF')),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    tenant_id   BIGINT       NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT users_username_uq UNIQUE (username)
);

-- username index is covered by the unique constraint above
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
