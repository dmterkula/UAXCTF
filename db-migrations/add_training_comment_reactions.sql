USE uaxc;

-- Training Comment Reactions Feature
-- Adds emoji reaction support to training run, workout, and cross-training comments.
-- All three activity types share the training_comments table (via training_entity_uuid),
-- so one reactions table covers all of them.

CREATE TABLE training_comment_reactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    comment_uuid VARCHAR(255) NOT NULL COMMENT 'UUID of the training_comments row being reacted to',
    reactor_username VARCHAR(255) NOT NULL COMMENT 'AppUser username who reacted',
    reactor_display_name VARCHAR(255) NOT NULL COMMENT 'Display name for UI',
    emoji VARCHAR(20) NOT NULL COMMENT 'Unicode emoji character(s)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY unique_reaction (comment_uuid, reactor_username, emoji),
    INDEX idx_comment (comment_uuid),
    INDEX idx_reactor (reactor_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Emoji reactions on training run, workout, and cross-training comments';
