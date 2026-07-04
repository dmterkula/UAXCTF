USE uaxc;

-- Venues Feature
-- Adds an optional reusable location (venue) to all event types.
-- Venue images are stored in Firebase Storage at venue_images/<id>.jpg.
-- All venue_id foreign keys are nullable — existing rows are unaffected.

CREATE TABLE venues (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    image_path VARCHAR(500) NULL COMMENT 'Firebase Storage path, e.g. venue_images/42.jpg',
    team VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_team (team)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reusable locations that can be attached to any event type';

ALTER TABLE meets
    ADD COLUMN venue_id INT NULL AFTER team,
    ADD CONSTRAINT fk_meets_venue FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL;

ALTER TABLE track_meets
    ADD COLUMN venue_id INT NULL AFTER team,
    ADD CONSTRAINT fk_track_meets_venue FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL;

ALTER TABLE xc_training_runs
    ADD COLUMN venue_id INT NULL AFTER effort_label,
    ADD CONSTRAINT fk_xc_training_runs_venue FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL;

ALTER TABLE workout_clone
    ADD COLUMN venue_id INT NULL AFTER team,
    ADD CONSTRAINT fk_workout_clone_venue FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL;

ALTER TABLE cross_training
    ADD COLUMN venue_id INT NULL AFTER cross_training_type,
    ADD CONSTRAINT fk_cross_training_venue FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL;

ALTER TABLE lifting_activities
    ADD COLUMN venue_id INT NULL AFTER suggested_exercises,
    ADD CONSTRAINT fk_lifting_activities_venue FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL;
