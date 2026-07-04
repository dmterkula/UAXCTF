-- Add feature toggles for readiness flags and coach insights feed
-- team = NULL means global (applies to all teams; team-specific rows override if present)

INSERT IGNORE INTO uaxc.frontend_toggles (toggle_name, team, enabled, description, created_at, updated_at)
VALUES
    ('readiness', NULL, TRUE,  'Team Readiness view: roster red/yellow/green health check for coaches based on pain, effort, logging frequency, and sRPE load spikes', NOW(), NOW()),
    ('coach_insights', NULL, TRUE, 'Coach Insights Feed: ranked 7-day digest surfacing readiness alerts, PR timing, SB staleness, logging consistency, and load leaders', NOW(), NOW());
