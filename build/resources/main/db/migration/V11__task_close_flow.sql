-- V11__task_close_flow.sql (PostgreSQL-safe)

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS close_requested BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS close_requested_at TIMESTAMPTZ NULL;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ NULL;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS closed_by_id BIGINT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_tasks_closed_by'
          AND conrelid = 'tasks'::regclass
    ) THEN
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_closed_by
        FOREIGN KEY (closed_by_id) REFERENCES users(id);
END IF;
END $$;
