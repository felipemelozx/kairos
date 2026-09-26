-- FIX-002: reconcile legacy series_id before enforcing V6 constraints on real data,
-- and add optimistic-locking version columns.
-- Orphan series references (possible between V4 and V6) are detached, not deleted.
UPDATE time_blocks
SET series_id = NULL
WHERE series_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM time_block_series s WHERE s.id = time_blocks.series_id);

-- Keep a single live occurrence per (series_id, start_datetime); soft-delete newer duplicates.
-- (UUID has no MIN() aggregate in Postgres, so compare via text cast.)
UPDATE time_blocks t
SET deleted_at = NOW()
WHERE t.deleted_at IS NULL
  AND t.series_id IS NOT NULL
  AND t.id::text NOT IN (
    SELECT MIN(id::text) FROM time_blocks
    WHERE deleted_at IS NULL AND series_id IS NOT NULL
    GROUP BY series_id, start_datetime
  );

ALTER TABLE projects ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE time_blocks ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE time_block_series ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
