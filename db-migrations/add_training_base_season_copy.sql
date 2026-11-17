-- Migration: Track where a copied base training pace came from
-- Date: 2026-11-12
-- Description: Supports POST training/base-performances/copy-season, which carries base training paces
--              from one season to another (e.g. XC 2026 5000m -> track 2027 5000m) with an optional
--              slow-down adjustment. Copied rows get source = 'SEASON_COPY' plus these two columns.
--
-- IMPORTANT: run this BEFORE deploying the backend build that maps these columns
-- (spring.jpa.hibernate.ddl-auto=none, so every read of the table fails if the columns are missing).

USE uaxc;

ALTER TABLE runners_training_base_performance
    ADD COLUMN derived_from_base_performance_id INT NULL,
    ADD COLUMN source_detail VARCHAR(255) NULL;
