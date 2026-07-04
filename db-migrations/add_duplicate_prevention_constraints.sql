-- Duplicate-prevention constraints
--
-- Context: the runner-logging endpoints (training run, cross training, workout)
-- perform a non-atomic read-then-write upsert keyed on a natural key. Under
-- concurrent requests (e.g. a rapid multi-tap on the Save button) every request
-- reads "no existing row" before any of them commits, so each inserts a row and
-- the result is duplicate records. The same pattern in PointsService.earnPoints
-- can double-award points for a single activity.
--
-- These UNIQUE constraints are the database-level backstop: they make the
-- second (and later) racing insert fail instead of creating a duplicate,
-- independent of client behavior. The iOS Save-button submit guards remain the
-- first line of defense; this guarantees correctness even if that guard is
-- bypassed or a future client regresses.
--
-- Note: each natural key already matches the dedup lookups in the services
-- (findByTrainingRunUuidAndRunnerId, findByCrossTrainingUuidAndRunnerId,
-- findByWorkoutUuidAndRunnerId, findByActivityUuid), so these constraints only
-- formalize an invariant the application already assumes.
--
-- activity_uuid is nullable (spend/refund rows leave it NULL); MySQL unique
-- indexes permit multiple NULLs, so only EARNED rows (which set activity_uuid)
-- are constrained to be unique.

ALTER TABLE uaxc.xc_training_run_records
    ADD CONSTRAINT uq_training_run_records_run_runner
    UNIQUE (training_run_uuid, runner_id);

ALTER TABLE uaxc.cross_training_records
    ADD CONSTRAINT uq_cross_training_records_xtrain_runner
    UNIQUE (cross_training_uuid, runner_id);

ALTER TABLE uaxc.workout_distance
    ADD CONSTRAINT uq_workout_distance_workout_runner
    UNIQUE (workout_uuid, runner_id);

ALTER TABLE uaxc.point_transactions
    ADD CONSTRAINT uq_point_transactions_activity_uuid
    UNIQUE (activity_uuid);
