ALTER TABLE users
    ALTER COLUMN phone DROP NOT NULL,
    ALTER COLUMN password_hash DROP NOT NULL,
    ADD COLUMN email             VARCHAR(190),
    ADD COLUMN google_id         VARCHAR(64),
    ADD COLUMN telegram_id       BIGINT,
    ADD COLUMN telegram_username VARCHAR(64),
    ADD COLUMN auth_provider     VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE users ALTER COLUMN auth_provider DROP DEFAULT;

CREATE UNIQUE INDEX uq_users_google_id   ON users(google_id)   WHERE google_id IS NOT NULL;
CREATE UNIQUE INDEX uq_users_telegram_id ON users(telegram_id) WHERE telegram_id IS NOT NULL;
CREATE INDEX idx_users_email ON users(lower(email));