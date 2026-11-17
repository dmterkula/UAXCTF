-- Course elevation profiles per venue, versioned since courses get rerouted year to year.
-- A venue can have multiple profiles over time; is_current flags the default for new meets.
CREATE TABLE uaxc.venue_course_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    venue_id INT NOT NULL,
    label VARCHAR(255) NULL,
    is_current TINYINT(1) NOT NULL DEFAULT 0,
    gpx_content LONGTEXT NOT NULL,
    distance_meters DOUBLE NULL,
    elevation_gain_ft DOUBLE NULL,
    elevation_loss_ft DOUBLE NULL,
    min_elevation_ft DOUBLE NULL,
    max_elevation_ft DOUBLE NULL,
    max_grade_pct DOUBLE NULL,
    min_grade_pct DOUBLE NULL,
    course_cost_factor DOUBLE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_venue_course_profiles_venue_id (venue_id)
);

ALTER TABLE uaxc.meets
    ADD COLUMN course_profile_id BIGINT NULL;
