-- Migration: Add Lifting PR History Table
-- Date: 2025-01-12
-- Description: Adds history tracking for all PRs set over time

USE uaxc;

-- Create lifting_pr_history table
-- This table logs every PR as it's set, creating a historical record
-- Unlike lifting_prs, this table does NOT have a unique constraint,
-- allowing multiple PR records for the same runner/exercise/type combination
CREATE TABLE IF NOT EXISTS lifting_pr_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    runner_id INT NOT NULL,
    exercise_uuid VARCHAR(255) NOT NULL COMMENT 'Soft reference to exercises',
    pr_type VARCHAR(50) NOT NULL COMMENT 'MAX_WEIGHT or MAX_DURATION',
    weight DOUBLE NULL,
    weight_unit VARCHAR(10) NULL COMMENT 'lbs or kg',
    rep_number INT NULL COMMENT 'For weight PRs',
    duration VARCHAR(50) NULL COMMENT 'For timed exercises',
    achieved_date TIMESTAMP NOT NULL COMMENT 'When this PR was achieved',
    lifting_record_uuid VARCHAR(255) NOT NULL COMMENT 'Source record for this PR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When this PR record was logged to history',

    INDEX idx_runner_exercise_date (runner_id, exercise_uuid, pr_type, achieved_date DESC),
    INDEX idx_runner_date (runner_id, achieved_date DESC),
    INDEX idx_exercise (exercise_uuid, pr_type, weight DESC),
    INDEX idx_lifting_record (lifting_record_uuid),
    INDEX idx_uuid (uuid),
    FOREIGN KEY (runner_id) REFERENCES runners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Historical record of all PRs set over time';

-- Optional: Migrate existing PRs from lifting_prs to lifting_pr_history
-- This preserves current PRs as the first entries in history
INSERT INTO lifting_pr_history (
    uuid,
    runner_id,
    exercise_uuid,
    pr_type,
    weight,
    weight_unit,
    rep_number,
    duration,
    achieved_date,
    lifting_record_uuid,
    created_at
)
SELECT
    UUID() as uuid,
    runner_id,
    exercise_uuid,
    pr_type,
    weight,
    weight_unit,
    rep_number,
    duration,
    achieved_date,
    lifting_record_uuid,
    created_at
FROM lifting_prs;

-- Verification queries (optional - run manually to verify)
-- SELECT COUNT(*) as total_pr_history FROM lifting_pr_history;
-- SELECT * FROM lifting_pr_history WHERE runner_id = ? AND exercise_uuid = ? ORDER BY achieved_date DESC;