-- Spring Security remember-me persistent token store
CREATE TABLE IF NOT EXISTS persistent_logins (
    username  VARCHAR(64)  NOT NULL,
    series    VARCHAR(64)  PRIMARY KEY,
    token     VARCHAR(64)  NOT NULL,
    last_used TIMESTAMPTZ  NOT NULL
);
