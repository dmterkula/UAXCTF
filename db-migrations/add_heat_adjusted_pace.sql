USE uaxc;

-- Heat-Adjusted Pace Feature
-- Adds venue geolocation (auto-resolved from an address via Open-Meteo geocoding),
-- an optional start time on events, and a cache table for fetched weather so we don't
-- re-hit the weather API on every view.

ALTER TABLE venues
    ADD COLUMN address VARCHAR(500) NULL AFTER image_path,
    ADD COLUMN latitude DOUBLE NULL AFTER address,
    ADD COLUMN longitude DOUBLE NULL AFTER latitude;

ALTER TABLE meets
    ADD COLUMN start_time TIME NULL AFTER date;

ALTER TABLE track_meets
    ADD COLUMN start_time TIME NULL AFTER date;

ALTER TABLE xc_training_runs
    ADD COLUMN start_time TIME NULL AFTER date;

ALTER TABLE workout_clone
    ADD COLUMN start_time TIME NULL AFTER date;

CREATE TABLE event_weather (
    id INT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL COMMENT 'e.g. MEET, TRACK_MEET, WORKOUT, TRAINING_RUN',
    event_id INT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    event_datetime DATETIME NOT NULL,
    temperature_c DOUBLE NOT NULL,
    humidity_pct DOUBLE NOT NULL,
    source VARCHAR(50) NOT NULL COMMENT 'open-meteo-forecast or open-meteo-archive',
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_event_weather_event (event_type, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Cached weather lookups used to compute heat-adjusted paces';
