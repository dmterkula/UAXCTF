-- Coaching Groups - Run manually against the uaxc MySQL database
-- (ddl-auto=none, so these must be executed directly)

CREATE TABLE IF NOT EXISTS uaxc.coaching_groups (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100) NOT NULL,
  year           VARCHAR(10)  NOT NULL,
  coach_username VARCHAR(100) NOT NULL,
  created_by     VARCHAR(100),
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS uaxc.coaching_group_memberships (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id   BIGINT NOT NULL,
  runner_id  INT    NOT NULL,
  CONSTRAINT fk_coaching_group_memberships_group FOREIGN KEY (group_id) REFERENCES uaxc.coaching_groups(id) ON DELETE CASCADE,
  CONSTRAINT ux_coaching_group_memberships_runner_group UNIQUE (runner_id, group_id)
);
