USE uaxc;

CREATE TABLE frontend_toggles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    toggle_name VARCHAR(100) UNIQUE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_frontend_toggles_name (toggle_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feature toggles queried by the frontend app';

INSERT INTO frontend_toggles (toggle_name, enabled, description) VALUES
    ('why_i_can', TRUE, 'Why I Can confidence affirmations feature');
