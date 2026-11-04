
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'courier_trips'
          AND column_name = 'from_address'
    ) THEN
        ALTER TABLE courier_trips ALTER COLUMN from_address DROP NOT NULL;
    END IF;
END $$;