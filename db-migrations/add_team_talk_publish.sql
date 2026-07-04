USE uaxc;

-- Adds draft/publish workflow to team_talks.
-- New talks default to unpublished; coaches flip `published` when they go live.
ALTER TABLE team_talks
    ADD COLUMN published    BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN published_at TIMESTAMP NULL;

-- Backfill existing rows so they stay visible to runners after the migration.
UPDATE team_talks SET published = TRUE, published_at = created_at WHERE published = FALSE;

ALTER TABLE team_talks
    ADD INDEX idx_list (team, season, year, published, published_at DESC);
