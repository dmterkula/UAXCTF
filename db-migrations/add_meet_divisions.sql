USE uaxc;

-- Meet Divisions Feature
-- Lets a coach split an XC meet into multiple races (Varsity, JV, Open, etc.), each with
-- its own start time (used for heat-adjusted pace) and running order. division_id is
-- nullable -- existing meets/performances are completely unaffected and keep behaving
-- exactly as they do today (single meet-level start_time, no division).

CREATE TABLE meet_divisions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    meet_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    sequence INT NOT NULL COMMENT 'Running order within the meet, 1 = first race of the day',
    start_time TIME NULL,
    distance_meters INT NULL,

    CONSTRAINT fk_meet_divisions_meet FOREIGN KEY (meet_id) REFERENCES meets(id) ON DELETE CASCADE,
    INDEX idx_meet_divisions_meet (meet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Individual races within an XC meet (Varsity/JV/Open/etc.)';

ALTER TABLE xc_meet_performances
    ADD COLUMN division_id INT NULL AFTER skulls_earned,
    ADD CONSTRAINT fk_xc_meet_performances_division FOREIGN KEY (division_id) REFERENCES meet_divisions(id) ON DELETE SET NULL;
