-- Migration: Add timestamp fields to runners_training_base_performance
-- Date: 2025-11-23
-- Description: Adds created_at and updated_at timestamp fields to track when base performance records are created and modified.
--              This enables historical tracking of base performance values over time.

USE uaxc;

-- Add created_at column (when the record was created)
ALTER TABLE runners_training_base_performance
ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP AFTER uuid;

-- Add updated_at column (when the record was last updated)
ALTER TABLE runners_training_base_performance
ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- Backfill existing records with a timestamp in the past
-- We use a date before any workouts were created to ensure historical workouts can find base performances
-- This assumes the earliest workout in your system is after 2020-01-01
-- Adjust this date if needed to be before your earliest workout
-- Both created_at and updated_at are set to the same date since we don't know when they were actually modified
UPDATE runners_training_base_performance
SET created_at = '2020-01-01 00:00:00',
    updated_at = '2020-01-01 00:00:00'
WHERE created_at IS NULL;Whe
-- Alternative approach: Set created_at based on the year field if you want more accuracy
-- This would set the timestamp to the beginning of the season year
-- UPDATE runners_training_base_performance
-- SET created_at = CONCAT(year, '-01-01 00:00:00'),
--     updated_at = CONCAT(year, '-01-01 00:00:00')
-- WHERE created_at IS NULL;

-- After verifying the migration worked, you can make the columns non-nullable if desired:
-- ALTER TABLE runners_training_base_performance MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
-- ALTER TABLE runners_training_base_performance MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Create index on created_at for faster historical queries
CREATE INDEX idx_created_at ON runners_training_base_performance(created_at);

-- Create composite index for common query patterns
CREATE INDEX idx_runner_season_created ON runners_training_base_performance(runner_id, season, created_at DESC);
CREATE INDEX idx_runner_event_season_created ON runners_training_base_performance(runner_id, event, season, created_at DESC);