-- Koordinatalar ixtiyoriy bo'lsin
ALTER TABLE deliveries ALTER COLUMN pickup_lat  DROP NOT NULL;
ALTER TABLE deliveries ALTER COLUMN pickup_lng  DROP NOT NULL;
ALTER TABLE deliveries ALTER COLUMN dropoff_lat DROP NOT NULL;
ALTER TABLE deliveries ALTER COLUMN dropoff_lng DROP NOT NULL;

-- Yuk hayot sikli
ALTER TABLE deliveries ADD COLUMN matched_at    TIMESTAMP;
ALTER TABLE deliveries ADD COLUMN cancelled_at  TIMESTAMP;
ALTER TABLE deliveries ADD COLUMN cancel_reason VARCHAR(300);