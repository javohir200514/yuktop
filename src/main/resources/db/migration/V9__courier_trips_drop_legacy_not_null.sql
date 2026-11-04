DO $$
DECLARE
    col RECORD;
BEGIN
    FOR col IN
        SELECT column_name
        FROM information_schema.columns
        WHERE table_name = 'courier_trips'
          AND is_nullable = 'NO'
          AND column_name NOT IN (
            'id', 'courier_id', 'from_region', 'to_region', 'departure_at',
            'max_package_size', 'status', 'created_at'
          )
    LOOP
        EXECUTE format('ALTER TABLE courier_trips ALTER COLUMN %I DROP NOT NULL', col.column_name);
    END LOOP;
END $$;