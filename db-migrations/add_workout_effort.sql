-- Migration: Add effort_level to workout_distance table
-- Date: 2026-06-17
-- Description: Adds required effort level tracking to workout results for sRPE load computation

USE uaxc;

ALTER TABLE workout_distance
    ADD COLUMN IF NOT EXISTS effort_level DOUBLE NULL;
