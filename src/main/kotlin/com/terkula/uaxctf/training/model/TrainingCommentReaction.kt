package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
    name = "training_comment_reactions",
    schema = "uaxc",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["comment_uuid", "reactor_username", "emoji"])
    ]
)
class TrainingCommentReaction(
    @Column(name = "comment_uuid", nullable = false)
    var commentUuid: String,

    @Column(name = "reactor_username", nullable = false)
    var reactorUsername: String,

    @Column(name = "reactor_display_name", nullable = false)
    var reactorDisplayName: String,

    @Column(name = "emoji", nullable = false, length = 20)
    var emoji: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
    }
}
