-- Add healthkit_import feature toggle
-- Controls whether the "Import from Apple Watch" button is shown in training run, workout, and cross training log forms
INSERT IGNORE INTO uaxc.frontend_toggles (toggle_name, team, enabled, description, created_at, updated_at)
VALUES ('healthkit_import', NULL, TRUE, 'Allow runners to import workouts from Apple Watch / HealthKit when logging runs, workouts, and cross training', NOW(), NOW());
