-- Coach Improvement Draft game - Run manually against the uaxc MySQL database
-- (ddl-auto=none, so these must be executed directly)

CREATE TABLE IF NOT EXISTS uaxc.draft_teams (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  coach_name VARCHAR(100) NOT NULL,
  season     VARCHAR(20)  NOT NULL,
  year       VARCHAR(10)  NOT NULL,
  created_by VARCHAR(100),
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS uaxc.draft_team_memberships (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  team_id    BIGINT NOT NULL,
  runner_id  INT    NOT NULL,
  CONSTRAINT fk_draft_team_memberships_team FOREIGN KEY (team_id) REFERENCES uaxc.draft_teams(id) ON DELETE CASCADE,
  CONSTRAINT ux_draft_team_memberships_runner_team UNIQUE (runner_id, team_id)
);
