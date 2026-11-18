USE uaxc;

-- Heart Rate Time Series
-- Stores the downsampled HR curve for a logged activity (training run, workout, cross training).
-- Purely additive: separate from the *_records tables so existing endpoints/DTOs are untouched
-- and older app versions keep working. avg/max stay on the original records.
-- activity_uuid is the EVENT uuid (training_run_uuid / workout_uuid / cross_training_uuid) paired with runner_id.

CREATE TABLE IF NOT EXISTS heart_rate_series (
    id INT PRIMARY KEY AUTO_INCREMENT,
    activity_type VARCHAR(20) NOT NULL COMMENT 'training_run | workout | cross_training',
    activity_uuid VARCHAR(255) NOT NULL,
    runner_id INT NOT NULL,
    sample_count INT NOT NULL,
    duration_seconds INT NOT NULL,
    avg_hr INT NULL,
    max_hr INT NULL,
    min_hr INT NULL,
    samples LONGTEXT NOT NULL COMMENT 'JSON array of {offsetSeconds, bpm}',
    source VARCHAR(100) NULL COMMENT 'e.g. Apple Watch, Garmin Connect',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY unique_hr_series (activity_type, activity_uuid, runner_id),
    INDEX idx_hr_runner (runner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Downsampled heart rate time series per logged activity';
