-- Add per-meeting alarm toggle (default ON)
ALTER TABLE meetings
    ADD COLUMN IF NOT EXISTS alarm_enabled boolean NOT NULL DEFAULT true;

-- Backfill (in case the column existed but had nulls in some environments)
UPDATE meetings SET alarm_enabled = true WHERE alarm_enabled IS NULL;
