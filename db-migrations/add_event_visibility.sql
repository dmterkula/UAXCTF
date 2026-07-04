-- Migration: Add Event Visibility Table
-- Date: 2026-06-19
-- Description: Allows coaches to restrict any calendar event to a subset of runners.
--              If no rows exist for an event_uuid, the event is visible to everyone.
--              If rows exist, only those runner_ids can see the event.

USE uaxc;

CREATE TABLE IF NOT EXISTS event_visibility (
    id INT PRIMARY KEY AUTO_INCREMENT,
    event_uuid VARCHAR(255) NOT NULL,
    runner_id INT NOT NULL,
    UNIQUE KEY unique_event_runner (event_uuid, runner_id),
    INDEX idx_event_uuid (event_uuid),
    FOREIGN KEY (runner_id) REFERENCES runners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Per-event runner visibility assignments. Empty = visible to all.';
