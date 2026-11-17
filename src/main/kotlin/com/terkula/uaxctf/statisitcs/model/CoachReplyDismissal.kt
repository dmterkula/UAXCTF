package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Date
import java.sql.Timestamp
import javax.persistence.*

/**
 * A coach's decision that a given athlete log needs no reply from them.
 *
 * The reply queue is otherwise derived -- an item leaves it as soon as a coach comments or writes
 * coach notes -- so this records the one state that can't be inferred: "I read it, nothing to say."
 * Scoped per coach so one coach dismissing doesn't hide the log from another.
 */
@Entity
@Table(name = "coach_reply_dismissals", schema = "uaxc")
class CoachReplyDismissal(
    @Column(name = "coach_username")
    var coachUsername: String,
    @Column(name = "entity_uuid")
    var entityUuid: String,
    @Column(name = "entity_type")
    var entityType: String,
    @Column(name = "log_date")
    var logDate: Date,
    @Column(name = "dismissed_at")
    var dismissedAt: Timestamp = Timestamp(System.currentTimeMillis())
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
