ALTER TABLE deliveries
    ADD COLUMN delivery_type      VARCHAR(20) NOT NULL DEFAULT 'INTRACITY',
    ADD COLUMN from_region        VARCHAR(30) NOT NULL DEFAULT 'TOSHKENT_SHAHRI',
    ADD COLUMN to_region          VARCHAR(30) NOT NULL DEFAULT 'TOSHKENT_SHAHRI',
    ADD COLUMN weight_kg          NUMERIC(6,2),
    ADD COLUMN courier_lat        DOUBLE PRECISION,
    ADD COLUMN courier_lng        DOUBLE PRECISION,
    ADD COLUMN courier_updated_at TIMESTAMP;
ALTER TABLE deliveries
    ALTER COLUMN delivery_type DROP DEFAULT,
    ALTER COLUMN from_region   DROP DEFAULT,
    ALTER COLUMN to_region     DROP DEFAULT;

ALTER TABLE courier_trips
    ADD COLUMN from_region VARCHAR(30) NOT NULL DEFAULT 'TOSHKENT_SHAHRI',
    ADD COLUMN to_region   VARCHAR(30) NOT NULL DEFAULT 'TOSHKENT_SHAHRI';
ALTER TABLE courier_trips
    ALTER COLUMN from_region DROP DEFAULT,
    ALTER COLUMN to_region   DROP DEFAULT;

ALTER TABLE location_pings ADD COLUMN accuracy_m DOUBLE PRECISION;

CREATE INDEX idx_deliveries_match ON deliveries(status, from_region, to_region);
CREATE INDEX idx_trips_match ON courier_trips(status, from_region, to_region, departure_time);