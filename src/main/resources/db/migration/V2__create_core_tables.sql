ALTER TABLE users ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'UZ';

CREATE TABLE phone_verifications (
    id BIGSERIAL PRIMARY KEY, phone VARCHAR(20) NOT NULL, code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL, attempts INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT NOW());

CREATE TABLE courier_profiles (
    id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    document_url VARCHAR(500), selfie_url VARCHAR(500),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', rejection_reason VARCHAR(300),
    trust_score NUMERIC(5,2) NOT NULL DEFAULT 0, completed_deliveries INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW());

CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id), courier_id BIGINT REFERENCES users(id),
    pickup_address VARCHAR(300) NOT NULL, pickup_lat DOUBLE PRECISION NOT NULL, pickup_lng DOUBLE PRECISION NOT NULL,
    dropoff_address VARCHAR(300) NOT NULL, dropoff_lat DOUBLE PRECISION NOT NULL, dropoff_lng DOUBLE PRECISION NOT NULL,
    description VARCHAR(500), package_size VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(120) NOT NULL, recipient_phone VARCHAR(20) NOT NULL,
    reward NUMERIC(12,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    sealed_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    picked_up_at TIMESTAMP, delivered_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT NOW());
CREATE INDEX idx_deliveries_status ON deliveries(status);
CREATE INDEX idx_deliveries_sender ON deliveries(sender_id);
CREATE INDEX idx_deliveries_courier ON deliveries(courier_id);

CREATE TABLE courier_trips (
    id BIGSERIAL PRIMARY KEY, courier_id BIGINT NOT NULL REFERENCES users(id),
    from_address VARCHAR(300) NOT NULL, from_lat DOUBLE PRECISION NOT NULL, from_lng DOUBLE PRECISION NOT NULL,
    to_address VARCHAR(300) NOT NULL, to_lat DOUBLE PRECISION NOT NULL, to_lng DOUBLE PRECISION NOT NULL,
    departure_time TIMESTAMP NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW());

CREATE TABLE otp_codes (
    id BIGSERIAL PRIMARY KEY, delivery_id BIGINT NOT NULL REFERENCES deliveries(id),
    type VARCHAR(20) NOT NULL, code_hash VARCHAR(100) NOT NULL, expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0, used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW());

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY, delivery_id BIGINT NOT NULL UNIQUE REFERENCES deliveries(id),
    amount NUMERIC(12,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_tx_id VARCHAR(100), held_at TIMESTAMP, released_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW());

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY, delivery_id BIGINT NOT NULL REFERENCES deliveries(id),
    author_id BIGINT NOT NULL REFERENCES users(id), target_id BIGINT NOT NULL REFERENCES users(id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5), comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW());

CREATE TABLE disputes (
    id BIGSERIAL PRIMARY KEY, delivery_id BIGINT NOT NULL REFERENCES deliveries(id),
    opened_by_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(30) NOT NULL, description VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN', resolution VARCHAR(1000),
    resolved_by_id BIGINT REFERENCES users(id), resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW());

CREATE TABLE dispute_evidence (
    id BIGSERIAL PRIMARY KEY, dispute_id BIGINT NOT NULL REFERENCES disputes(id),
    file_url VARCHAR(500) NOT NULL, note VARCHAR(300), created_at TIMESTAMP NOT NULL DEFAULT NOW());

CREATE TABLE location_pings (
    id BIGSERIAL PRIMARY KEY, delivery_id BIGINT NOT NULL REFERENCES deliveries(id),
    courier_id BIGINT NOT NULL REFERENCES users(id),
    lat DOUBLE PRECISION NOT NULL, lng DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW());
CREATE INDEX idx_pings_delivery ON location_pings(delivery_id, created_at);