package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
    name = "team_talk_reactions",
    schema = "uaxc",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["team_talk_uuid", "username", "emoji"])
    ]
)
class TeamTalkReaction(
    @Column(name = "team_talk_uuid", nullable = false)
    var teamTalkUuid: String,

    @Column(name = "username", nullable = false)
    var username: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

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
