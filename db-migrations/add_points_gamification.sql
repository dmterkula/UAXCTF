-- Migration: Add points gamification system
-- Date: 2025-11-30
-- Description: Adds points-based gamification system including:
--              1. Points columns to runners table
--              2. point_transactions table for tracking all point activities
--              3. points_configuration table for backend-driven point values

USE uaxc;

-- ===== Step 1: Add points columns to runners table =====

ALTER TABLE runners
ADD COLUMN points INT DEFAULT 0 NOT NULL AFTER track_group;

ALTER TABLE runners
ADD COLUMN lifetime_points_earned INT DEFAULT 0 NOT NULL AFTER points;

ALTER TABLE runners
ADD COLUMN last_points_updated TIMESTAMP NULL AFTER lifetime_points_earned;

-- Create index for leaderboard queries
CREATE INDEX idx_points ON runners(points DESC);

-- ===== Step 2: Create point_transactions table =====

CREATE TABLE point_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    runner_id INT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL COMMENT 'EARNED, SPENT, REFUND',
    points_amount INT NOT NULL,
    activity_type VARCHAR(50) NULL COMMENT 'TRAINING_RUN, WORKOUT, CROSS_TRAINING, MEET_LOG, PRE_MEET_LOG',
    feature_type VARCHAR(50) NULL COMMENT 'SEND_GIF, PIN_MESSAGE, MESSAGE_ANIMATION, CHANGE_CHAT_NAME, CUSTOM_AVATAR',
    activity_uuid VARCHAR(255) NULL,
    description TEXT NULL,
    balance_after INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    season VARCHAR(20) NULL,
    is_refunded BOOLEAN DEFAULT FALSE,
    refunded_at TIMESTAMP NULL,
    related_transaction_id BIGINT NULL,
    FOREIGN KEY (runner_id) REFERENCES runners(id),
    FOREIGN KEY (related_transaction_id) REFERENCES point_transactions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tracks all point earning, spending, and refund transactions';

-- Indexes for point transactions
CREATE INDEX idx_point_txn_runner ON point_transactions(runner_id, created_at DESC);
CREATE INDEX idx_point_txn_type ON point_transactions(transaction_type);
CREATE INDEX idx_point_txn_activity ON point_transactions(activity_uuid);
CREATE INDEX idx_point_txn_season ON point_transactions(season, created_at DESC);

-- ===== Step 3: Create points_configuration table =====

CREATE TABLE points_configuration (
    id INT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value INT NOT NULL,
    description TEXT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Backend-driven configuration for points earning and spending';

-- ===== Step 4: Insert initial configuration values =====

INSERT INTO points_configuration (config_key, config_value, description) VALUES
    ('EARN_TRAINING_RUN', 10, 'Points awarded for logging a training run'),
    ('EARN_WORKOUT', 15, 'Points awarded for logging a workout'),
    ('EARN_CROSS_TRAINING', 8, 'Points awarded for logging cross training'),
    ('EARN_MEET_LOG', 20, 'Points awarded for logging a meet'),
    ('EARN_PRE_MEET_LOG', 5, 'Points awarded for logging pre-meet prep'),
    ('COST_SEND_GIF', 5, 'Points cost to send a GIF in chat'),
    ('COST_PIN_MESSAGE', 10, 'Points cost to pin a message'),
    ('COST_MESSAGE_ANIMATION', 3, 'Points cost for message animation effects'),
    ('COST_CHANGE_CHAT_NAME', 50, 'Points cost to change chat name'),
    ('COST_CUSTOM_AVATAR', 100, 'Points cost for custom avatar upload');

-- ===== Verification queries (commented out - run manually to verify) =====

-- Check runners table has new columns
-- DESCRIBE runners;

-- Check point_transactions table structure
-- DESCRIBE point_transactions;

-- Check points_configuration table and initial values
-- SELECT * FROM points_configuration;

-- Verify all runners have 0 points initially
-- SELECT name, points, lifetime_points_earned FROM runners LIMIT 10;