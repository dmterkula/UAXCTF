-- Pace Calibration: add source tracking to runners_training_base_performance
-- Run once against the uaxc schema

ALTER TABLE uaxc.runners_training_base_performance
    ADD COLUMN source VARCHAR(32) NOT NULL DEFAULT 'COACH_ESTIMATE',
    ADD COLUMN derived_from_meet_id INT NULL;
