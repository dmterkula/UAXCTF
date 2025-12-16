-- Create suggestions table
CREATE TABLE IF NOT EXISTS uaxc.suggestions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    runner_id INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'under_review',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_changed_at TIMESTAMP NULL,
    team VARCHAR(50) NOT NULL,
    season VARCHAR(10),
    INDEX idx_runner_id (runner_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at),
    INDEX idx_uuid (uuid),
    FOREIGN KEY (runner_id) REFERENCES uaxc.runners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create suggestion thumbs up table
CREATE TABLE IF NOT EXISTS uaxc.suggestion_thumbs_up (
    id INT PRIMARY KEY AUTO_INCREMENT,
    suggestion_uuid VARCHAR(255) NOT NULL,
    runner_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_thumbs_up (suggestion_uuid, runner_id),
    INDEX idx_suggestion_uuid (suggestion_uuid),
    INDEX idx_runner_id (runner_id),
    FOREIGN KEY (runner_id) REFERENCES uaxc.runners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create suggestion comments table
CREATE TABLE IF NOT EXISTS uaxc.suggestion_comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(255) UNIQUE NOT NULL,
    suggestion_uuid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_suggestion_uuid (suggestion_uuid),
    INDEX idx_created_at (created_at),
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
