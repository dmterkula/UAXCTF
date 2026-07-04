USE uaxc;

-- Add nullable team column
ALTER TABLE frontend_toggles ADD COLUMN team VARCHAR(10) NULL AFTER toggle_name;

-- Replace the old unique constraint on toggle_name with a composite unique on (toggle_name, team)
ALTER TABLE frontend_toggles DROP INDEX toggle_name;
ALTER TABLE frontend_toggles ADD UNIQUE INDEX idx_toggle_name_team (toggle_name, team);
