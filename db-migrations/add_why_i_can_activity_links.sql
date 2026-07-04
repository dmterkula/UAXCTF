USE uaxc;

CREATE TABLE why_i_can_activity_links (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    runner_id INT NOT NULL,
    activity_type VARCHAR(50) NOT NULL COMMENT 'TRAINING_RUN, MEET_LOG, TRACK_MEET_LOG, CROSS_TRAINING, LIFTING, WORKOUT',
    activity_uuid VARCHAR(255) NOT NULL COMMENT 'UUID of the linked activity; composite key for types without native UUID',
    display_title VARCHAR(500) NOT NULL COMMENT 'Pre-computed at star time; custom or auto-generated',
    activity_date DATE NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uq_runner_activity (runner_id, activity_type, activity_uuid),
    INDEX idx_why_i_can_links_runner (runner_id, display_order),
    INDEX idx_why_i_can_links_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Starred logged activities linked to a runner Why I Can experience';
