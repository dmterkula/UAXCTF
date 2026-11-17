USE uaxc;

-- Coach Reply Queue Feature
-- Backs the coach-facing action queue of athlete logs still awaiting a coach response.
--
-- The queue itself is fully derived at request time (an item is "outstanding" when no coach has
-- commented on it and no coach notes were written), so nothing about the queue is stored. The one
-- thing that cannot be derived is intent: a coach reading a log and deciding it needs no reply.
-- This table records exactly that, per coach, so one coach dismissing an item does not silence it
-- for another.
--
-- entity_uuid is the same polymorphic key used by training_comments.training_entity_uuid, so a
-- dismissal and a reply address an item identically:
--   training run    -> xc_training_run_records.uuid
--   cross training  -> cross_training_records.uuid
--   lifting         -> lifting_records.uuid
--   workout         -> workout uuid + runner id (concatenated, matching existing comment keying)
--   pre-meet log    -> pre_meet_log.uuid
--   post-meet log   -> meet_uuid + "-" + runner_id
--   track post-meet -> meet_uuid + "-" + runner_id + "-" + event

CREATE TABLE coach_reply_dismissals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    coach_username VARCHAR(255) NOT NULL COMMENT 'AppUser username of the coach who dismissed the item',
    entity_uuid VARCHAR(255) NOT NULL COMMENT 'Polymorphic log key, same convention as training_comments.training_entity_uuid',
    entity_type VARCHAR(50) NOT NULL COMMENT 'TRAINING_RUN | WORKOUT | CROSS_TRAINING | LIFTING | PRE_MEET | POST_MEET | TRACK_POST_MEET',
    log_date DATE NOT NULL COMMENT 'Date of the underlying log, so old dismissals can be pruned by range',
    dismissed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY unique_dismissal (coach_username, entity_uuid),
    INDEX idx_coach (coach_username),
    INDEX idx_coach_date (coach_username, log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Per-coach dismissals of athlete logs in the coach reply queue';
