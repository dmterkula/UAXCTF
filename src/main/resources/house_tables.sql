-- House Points System - Run manually against the uaxc MySQL database
-- (ddl-auto=none, so these must be executed directly)

CREATE TABLE IF NOT EXISTS uaxc.houses (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  season     VARCHAR(20)  NOT NULL,
  year       VARCHAR(10)  NOT NULL,
  created_by VARCHAR(100),
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS uaxc.house_memberships (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  house_id   BIGINT NOT NULL,
  runner_id  INT    NOT NULL,
  CONSTRAINT fk_house_memberships_house FOREIGN KEY (house_id) REFERENCES uaxc.houses(id) ON DELETE CASCADE,
  CONSTRAINT ux_house_memberships_runner_house UNIQUE (runner_id, house_id)
);

-- Seed the house_points feature toggle (disabled by default)
INSERT IGNORE INTO uaxc.frontend_toggles (toggle_name, team, enabled, description, created_at, updated_at)
VALUES ('house_points', NULL, FALSE, 'Show house points competition in pride points section', NOW(), NOW());
