USE uaxc;

-- Team Talks Feature Database Migration
-- Creates tables for team talks, reactions, and threaded comments

-- 1. Team Talks Table
CREATE TABLE team_talks (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    author VARCHAR(100) NOT NULL COMMENT 'Coach username/ID who created the talk',
    season VARCHAR(20) NOT NULL COMMENT 'xc or track',
    year VARCHAR(10) NOT NULL COMMENT 'e.g., 2024, 2025',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL COMMENT 'Markdown text content',
    team VARCHAR(20) NOT NULL DEFAULT 'UA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_season_year (season, year, created_at DESC),
    INDEX idx_uuid (uuid),
    INDEX idx_team_season (team, season, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coach team talks stored as markdown';

-- 2. Team Talk Reactions Table
CREATE TABLE team_talk_reactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    team_talk_uuid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL COMMENT 'AppUser username who reacted',
    display_name VARCHAR(255) NOT NULL COMMENT 'Display name for UI',
    emoji VARCHAR(20) NOT NULL COMMENT 'Unicode emoji character(s)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY unique_reaction (team_talk_uuid, username, emoji),
    INDEX idx_team_talk (team_talk_uuid),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Emoji reactions on team talks';

-- 3. Team Talk Comments Table (Threaded/Nested)
CREATE TABLE team_talk_comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    team_talk_uuid VARCHAR(255) NOT NULL,
    parent_comment_uuid VARCHAR(255) NULL COMMENT 'NULL for top-level, UUID for replies',
    username VARCHAR(255) NOT NULL COMMENT 'AppUser username for profile picture cache key',
    display_name VARCHAR(255) NOT NULL COMMENT 'Display name (runner.name for athletes)',
    device_id VARCHAR(255) NULL COMMENT 'Device ID for push notifications',
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_team_talk (team_talk_uuid, created_at),
    INDEX idx_parent (parent_comment_uuid),
    INDEX idx_username (username),
    INDEX idx_team_talk_username (team_talk_uuid, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Threaded comments on team talks';

-- 4. Add Points Configuration for Team Talk Comments
INSERT INTO points_configuration (config_key, config_value, description) VALUES
    ('EARN_TEAM_TALK_COMMENT', 3, 'Points awarded for commenting on a team talk');
