-- Migration: Add Lifting/Strength Training Feature
-- Date: 2025-01-10
-- Description: Adds complete lifting tracking with exercises, activities, records, and PRs

USE uaxc;

-- ===== Step 1: Create exercises table =====
CREATE TABLE IF NOT EXISTS exercises (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    category VARCHAR(100) NOT NULL COMMENT 'UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, PLYOMETRIC, FLEXIBILITY',
    exercise_type VARCHAR(50) NOT NULL COMMENT 'WEIGHT, BODYWEIGHT, DURATION',
    is_global BOOLEAN NOT NULL DEFAULT FALSE,
    team VARCHAR(20) NULL,
    created_by VARCHAR(100) NULL,
    default_weight_unit VARCHAR(10) NOT NULL DEFAULT 'lbs',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_global_category (is_global, category),
    INDEX idx_team_exercises (team, category),
    INDEX idx_uuid (uuid),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Exercise library with global and team-specific exercises';

-- ===== Step 2: Create lifting_activities table =====
CREATE TABLE IF NOT EXISTS lifting_activities (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    date DATE NOT NULL,
    duration VARCHAR(50) NULL COMMENT 'Expected duration like "60 min"',
    icon VARCHAR(50) NOT NULL DEFAULT 'dumbbell',
    season VARCHAR(20) NOT NULL COMMENT 'xc or track',
    team VARCHAR(20) NOT NULL,
    suggested_exercises TEXT NULL COMMENT 'JSON array of exercise UUIDs',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_date_team (date, team),
    INDEX idx_season_team (season, team, date DESC),
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Planned lifting activities/sessions';

-- ===== Step 3: Create lifting_records table =====
CREATE TABLE IF NOT EXISTS lifting_records (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    lifting_activity_uuid VARCHAR(255) NOT NULL COMMENT 'Soft reference to lifting_activities',
    runner_id INT NOT NULL,
    exercises_data TEXT NOT NULL COMMENT 'JSON array of exercise entries with sets/reps',
    total_duration VARCHAR(50) NULL COMMENT 'Actual duration like "45 min"',
    notes TEXT NULL COMMENT 'Athlete notes',
    coaches_notes TEXT NULL COMMENT 'Coach feedback',
    effort_level DOUBLE NULL COMMENT 'Self-reported 1-10 scale',
    date_logged TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_activity (lifting_activity_uuid),
    INDEX idx_runner (runner_id, date_logged DESC),
    INDEX idx_activity_runner (lifting_activity_uuid, runner_id),
    INDEX idx_uuid (uuid),
    FOREIGN KEY (runner_id) REFERENCES runners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Individual lifting session records with nested exercise data';

-- ===== Step 4: Create lifting_prs table =====
CREATE TABLE IF NOT EXISTS lifting_prs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    runner_id INT NOT NULL,
    exercise_uuid VARCHAR(255) NOT NULL COMMENT 'Soft reference to exercises',
    pr_type VARCHAR(50) NOT NULL COMMENT 'MAX_WEIGHT or MAX_DURATION',
    weight DOUBLE NULL,
    weight_unit VARCHAR(10) NULL COMMENT 'lbs or kg',
    rep_number INT NULL COMMENT 'For weight PRs',
    duration VARCHAR(50) NULL COMMENT 'For timed exercises',
    achieved_date TIMESTAMP NOT NULL,
    lifting_record_uuid VARCHAR(255) NOT NULL COMMENT 'Source record for this PR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_runner_exercise (runner_id, exercise_uuid, pr_type),
    INDEX idx_runner_date (runner_id, achieved_date DESC),
    INDEX idx_exercise (exercise_uuid, pr_type, weight DESC),
    INDEX idx_uuid (uuid),
    FOREIGN KEY (runner_id) REFERENCES runners(id),
    UNIQUE KEY unique_pr (runner_id, exercise_uuid, pr_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Personal records for lifting exercises';

-- ===== Step 5: Seed default global exercises =====
INSERT INTO exercises (uuid, name, description, category, exercise_type, is_global, default_weight_unit) VALUES
    (UUID(), 'Barbell Back Squat', 'Traditional back squat with barbell', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Barbell Bench Press', 'Flat bench press', 'UPPER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Deadlift', 'Conventional deadlift', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Pull-ups', 'Bodyweight or weighted pull-ups', 'UPPER_BODY', 'BODYWEIGHT', TRUE, 'lbs'),
    (UUID(), 'Push-ups', 'Standard push-ups', 'UPPER_BODY', 'BODYWEIGHT', TRUE, 'lbs'),
    (UUID(), 'Romanian Deadlift (RDL)', 'Hip-focused deadlift variation', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Barbell Row', 'Bent-over barbell row', 'UPPER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Overhead Press', 'Standing or seated overhead press', 'UPPER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Front Squat', 'Barbell front squat', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Box Jumps', 'Plyometric box jumps', 'PLYOMETRIC', 'BODYWEIGHT', TRUE, 'lbs'),
    (UUID(), 'Burpees', 'Full body plyometric exercise', 'FULL_BODY', 'BODYWEIGHT', TRUE, 'lbs'),
    (UUID(), 'Plank Hold', 'Static plank hold for time', 'CORE', 'DURATION', TRUE, 'lbs'),
    (UUID(), 'Side Plank', 'Side plank hold for time', 'CORE', 'DURATION', TRUE, 'lbs'),
    (UUID(), 'Bulgarian Split Squat', 'Single leg squat variation', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Lunges', 'Walking or stationary lunges', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Dumbbell Shoulder Press', 'Seated or standing dumbbell press', 'UPPER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Lat Pulldown', 'Cable lat pulldown', 'UPPER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Leg Press', 'Machine leg press', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Single-Leg RDL', 'Single leg Romanian deadlift', 'LOWER_BODY', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Russian Twists', 'Weighted or bodyweight Russian twists', 'CORE', 'WEIGHT', TRUE, 'lbs'),
    (UUID(), 'Mountain Climbers', 'Dynamic core exercise', 'CORE', 'BODYWEIGHT', TRUE, 'lbs'),
    (UUID(), 'Yoga Flow', 'Dynamic stretching and flexibility', 'FLEXIBILITY', 'DURATION', TRUE, 'lbs'),
    (UUID(), 'Foam Rolling', 'Myofascial release and recovery', 'FLEXIBILITY', 'DURATION', TRUE, 'lbs'),
    (UUID(), 'Hip Flexor Stretch', 'Static hip flexor stretch', 'FLEXIBILITY', 'DURATION', TRUE, 'lbs'),
    (UUID(), 'Hamstring Stretch', 'Static hamstring stretch', 'FLEXIBILITY', 'DURATION', TRUE, 'lbs');

-- ===== Step 6: Add points configuration for lifting =====
INSERT INTO points_configuration (config_key, config_value, description)
VALUES ('EARN_LIFTING', 10, 'Points awarded for logging a lifting session')
ON DUPLICATE KEY UPDATE config_value = 10;

-- ===== Verification queries (optional - run manually to verify) =====
-- Check exercises table
-- SELECT * FROM exercises WHERE is_global = TRUE;

-- Check lifting tables structure
-- DESCRIBE lifting_activities;
-- DESCRIBE lifting_records;
-- DESCRIBE lifting_prs;

-- Check points configuration
-- SELECT * FROM points_configuration WHERE config_key = 'EARN_LIFTING';

-- Check counts
-- SELECT COUNT(*) as total_global_exercises FROM exercises WHERE is_global = TRUE;
