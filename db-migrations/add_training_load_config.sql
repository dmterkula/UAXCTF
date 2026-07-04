-- Add training_load_config table
-- Stores all tunable constants for readiness and insights computation.
-- team = NULL means global (applies to all teams; a team-specific row overrides if present).
-- All values are DOUBLE — integer-meaning constants (e.g. min_weekly_logs) are stored as 3.0.
-- Update any value with: PUT /xc/training-load-config/{key}  { "value": <new_value> }

USE uaxc;

CREATE TABLE IF NOT EXISTS training_load_config (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    config_key  VARCHAR(100) NOT NULL,
    team        VARCHAR(10)  NULL,
    value       DOUBLE       NOT NULL,
    description VARCHAR(500) NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_key_team (config_key, team)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Readiness signals ────────────────────────────────────────────────────────

INSERT IGNORE INTO training_load_config (config_key, team, value, description) VALUES
    ('readiness.pain_yellow_threshold',        NULL, 3.0,  'Pain level above this value triggers a YELLOW flag'),
    ('readiness.pain_red_threshold',           NULL, 5.0,  'Pain level at or above this value (or pain notes present) triggers a RED flag'),
    ('readiness.recovery_effort_ceiling',      NULL, 5.0,  'Max acceptable effort on a RECOVERY-labelled session before flagging'),
    ('readiness.default_effort_ceiling',       NULL, 7.0,  'Max acceptable effort on BASE_BUILDING / MR / HIGH_AEROBIC / N/A sessions before flagging'),
    ('readiness.spike_yellow_pct',             NULL, 15.0, 'Week-over-week load increase % that triggers a YELLOW load spike flag'),
    ('readiness.spike_red_pct',                NULL, 30.0, 'Week-over-week load increase % that triggers a RED load spike flag'),
    ('readiness.min_weekly_logs',              NULL, 3.0,  'Minimum sessions expected per week; fewer triggers a YELLOW logging gap flag'),
    ('readiness.window_weeks',                 NULL, 5.0,  'Number of trailing weeks used to compute EWMA and detect load trends'),
    ('readiness.ewma_lambda',                  NULL, 0.3,  'EWMA decay factor (0–1). Higher = more weight on recent weeks. Standard = 0.3'),
    ('readiness.workout_proxy_effort',         NULL, 7.5,  'Default effort assigned to workouts logged before effort was a required field'),
    ('readiness.ct_default_effort',            NULL, 5.0,  'Default effort assigned to cross-training records with no effort logged'),
    ('readiness.cross_training_load_multiplier', NULL, 0.8,
        'Fraction of aerobic load credited to cross-training. 1.0 = pure aerobic equivalence; 0.8 = discounts for lower musculoskeletal stress vs running');

-- ── Coach insights ───────────────────────────────────────────────────────────

INSERT IGNORE INTO training_load_config (config_key, team, value, description) VALUES
    ('insights.logging_strong_threshold', NULL, 5.0, 'Sessions/week required to earn a LOGGING_STRONG positive insight'),
    ('insights.top_load_count',           NULL, 3.0, 'Number of top load leaders to surface in the insights feed each week'),
    ('insights.sb_stale_days',            NULL, 21.0, 'Days without a season-best improvement before triggering a DAYS_SINCE_SB MEDIUM flag');
