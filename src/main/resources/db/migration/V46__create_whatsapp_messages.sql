CREATE TABLE whatsapp_messages (
    id          BIGSERIAL PRIMARY KEY,
    from_number VARCHAR(30)  NOT NULL,
    body        TEXT,
    twilio_sid  VARCHAR(50)  UNIQUE,
    received_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE INDEX idx_wa_messages_from_number ON whatsapp_messages (from_number);
CREATE INDEX idx_wa_messages_received_at ON whatsapp_messages (received_at DESC);
