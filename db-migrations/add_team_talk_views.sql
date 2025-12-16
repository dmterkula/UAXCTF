USE uaxc;

-- Team Talk Views Feature Database Migration
-- Tracks all views of team talks by authenticated users

CREATE TABLE team_talk_views (
    id INT PRIMARY KEY AUTO_INCREMENT,
    team_talk_uuid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL COMMENT 'AppUser username who viewed (soft reference)',
    display_name VARCHAR(255) NOT NULL COMMENT 'Display name for UI (cached at view time)',
    team VARCHAR(10) NOT NULL COMMENT 'Team identifier (e.g., UA, XC)',
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_team_talk (team_talk_uuid, viewed_at DESC),
    INDEX idx_username (username),
    INDEX idx_team_talk_username (team_talk_uuid, username),
    INDEX idx_team (team)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tracks all views of team talks by authenticated users';
