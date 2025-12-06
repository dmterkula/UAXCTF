-- ============================================================
-- Migration: Add Seasonal Pride Points Rewards System
-- Date: 2024-12-04
-- Description: Adds team-based seasonal rewards infrastructure
--              including reward configurations and seasonal
--              points tracking tables
-- ============================================================

USE uaxc;

-- ============================================================
-- Table 1: seasonal_rewards
-- Purpose: Store coach-configured reward tiers for each season
-- ============================================================

CREATE TABLE seasonal_rewards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    season VARCHAR(20) NOT NULL COMMENT 'xc or track',
    year VARCHAR(10) NOT NULL COMMENT 'Season year (e.g., 2024, 2025)',
    reward_name VARCHAR(255) NOT NULL COMMENT 'Name of the reward (e.g., Bronze Tier, Gold Achievement)',
    description TEXT NOT NULL COMMENT 'Reward description (e.g., Team Pizza Party)',
    point_threshold INT NOT NULL COMMENT 'Points required to unlock this reward',
    display_order INT NOT NULL DEFAULT 0 COMMENT 'Display order for UI (1, 2, 3...)',
    is_achieved BOOLEAN DEFAULT FALSE COMMENT 'Whether team has reached this threshold',
    achieved_date TIMESTAMP NULL COMMENT 'When the team unlocked this reward',
    created_by VARCHAR(100) NULL COMMENT 'Coach who created the reward',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE KEY uk_season_year_order (season, year, display_order),

    -- Indexes for performance
    INDEX idx_season_year (season, year),
    INDEX idx_achieved (is_achieved, achieved_date)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Team-based seasonal reward configurations';

-- ============================================================
-- Table 2: seasonal_points_tracking
-- Purpose: Denormalized table tracking individual runner's
--          seasonal points for efficient team total calculation
-- ============================================================

CREATE TABLE seasonal_points_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    runner_id INT NOT NULL,
    season VARCHAR(20) NOT NULL COMMENT 'xc or track',
    year VARCHAR(10) NOT NULL COMMENT 'Season year',
    points_earned INT NOT NULL DEFAULT 0 COMMENT 'Points earned this season',
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE KEY uk_runner_season_year (runner_id, season, year),
    FOREIGN KEY (runner_id) REFERENCES runners(id) ON DELETE CASCADE,

    -- Indexes for performance
    INDEX idx_season_year (season, year),
    INDEX idx_points_earned (points_earned DESC)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Individual runner seasonal points for team progress calculation';

-- ============================================================
-- Seed Data: Sample rewards for testing
-- Note: Coaches can delete/modify these via API
-- ============================================================

INSERT INTO seasonal_rewards (season, year, reward_name, description, point_threshold, display_order, created_by)
VALUES
    ('xc', '2024', 'Bronze Achievement', 'Team Pizza Party', 1000, 1, 'system'),
    ('xc', '2024', 'Silver Achievement', 'Custom Team Apparel', 2500, 2, 'system'),
    ('xc', '2024', 'Gold Achievement', 'Post-Season Team Trip', 5000, 3, 'system'),
    ('track', '2025', 'Bronze Achievement', 'Indoor Track Meet Trip', 1500, 1, 'system'),
    ('track', '2025', 'Silver Achievement', 'Team Bonding Event', 3000, 2, 'system');

-- ============================================================
-- Verification Queries (Run manually to verify installation)
-- ============================================================

-- Verify seasonal_rewards table structure
-- DESCRIBE seasonal_rewards;

-- Verify seasonal_points_tracking table structure
-- DESCRIBE seasonal_points_tracking;

-- Check indexes on seasonal_rewards
-- SHOW INDEX FROM seasonal_rewards;

-- Check indexes on seasonal_points_tracking
-- SHOW INDEX FROM seasonal_points_tracking;

-- View sample rewards
-- SELECT * FROM seasonal_rewards ORDER BY season, year, display_order;

-- Check if any seasonal points exist
-- SELECT COUNT(*) FROM seasonal_points_tracking;

-- ============================================================
-- Backfill Strategy (Optional - for historical data)
-- ============================================================
-- Note: Only run if historical point_transactions have season/year populated
-- Currently, season field is null in existing records, so no backfill needed

-- Example backfill query (commented out):
--
-- INSERT INTO seasonal_points_tracking (runner_id, season, year, points_earned)
-- SELECT
--     runner_id,
--     season,
--     CAST(SUBSTRING_INDEX(season, '_', -1) AS UNSIGNED) as year,
--     SUM(CASE WHEN transaction_type = 'EARNED' THEN points_amount ELSE 0 END) as points_earned
-- FROM point_transactions
-- WHERE season IS NOT NULL
--   AND season != ''
-- GROUP BY runner_id, season
-- ON DUPLICATE KEY UPDATE
--     points_earned = VALUES(points_earned),
--     last_updated = CURRENT_TIMESTAMP;

-- ============================================================
-- Migration Complete
-- ============================================================
