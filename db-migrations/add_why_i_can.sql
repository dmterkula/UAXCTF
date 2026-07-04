USE uaxc;

-- Why I Can Entries Table
CREATE TABLE why_i_can (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    runner_id INT NOT NULL,
    text TEXT NOT NULL,
    size INT NOT NULL DEFAULT 3 COMMENT '1-5 scale for visual weight/prominence',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_why_i_can_runner (runner_id),
    INDEX idx_why_i_can_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Personal confidence affirmations per runner';

-- Why I Can Comments Table (coaches commenting on a runner''s collection)
CREATE TABLE why_i_can_comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    why_i_can_runner_id INT NOT NULL COMMENT 'The runner whose Why I Can page is being commented on',
    parent_comment_uuid VARCHAR(255) NULL COMMENT 'NULL for top-level, UUID for replies',
    username VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    device_id VARCHAR(255) NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_why_i_can_comments_runner (why_i_can_runner_id, created_at),
    INDEX idx_why_i_can_comments_parent (parent_comment_uuid),
    INDEX idx_why_i_can_comments_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Coach comments on runner Why I Can collections';

