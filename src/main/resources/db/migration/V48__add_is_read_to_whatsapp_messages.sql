ALTER TABLE whatsapp_messages
    ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;

-- Outbound messages are always considered read
UPDATE whatsapp_messages SET is_read = TRUE WHERE direction = 'OUTBOUND';
