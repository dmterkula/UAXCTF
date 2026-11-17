-- Migration: Add Event Groups Table
-- Date: 2026-11-15
-- Description: Tags any calendar event (workout, training run, cross training, lifting) with one or more
--              track groups ('distance', 'mid-distance', '400').
--              If no rows exist for an event_uuid, the event is for all groups.
--              Runner.track_group may now hold a comma-separated list (e.g. 'distance,400') so a runner can
--              belong to more than one group; existing single values remain valid.

USE uaxc;

CREATE TABLE IF NOT EXISTS event_groups (
    id INT PRIMARY KEY AUTO_INCREMENT,
    event_uuid VARCHAR(255) NOT NULL,
    track_group VARCHAR(32) NOT NULL,
    UNIQUE KEY unique_event_group (event_uuid, track_group),
    INDEX idx_event_groups_uuid (event_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Per-event track group tags. Empty = all groups.';
