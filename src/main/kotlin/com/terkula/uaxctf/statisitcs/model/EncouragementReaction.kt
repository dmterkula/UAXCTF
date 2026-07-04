package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "encouragement_reaction", schema = "uaxc")
class EncouragementReaction(
    @Column(name = "uuid", unique = true, nullable = false, length = 36)
    var uuid: String,

    @Column(name = "encouragement_uuid", nullable = false, length = 36)
    var encouragementUuid: String,

    @Column(name = "reactor_username", nullable = false)
    var reactorUsername: String,

    @Column(name = "reactor_display_name", nullable = false)
    var reactorDisplayName: String,

    @Column(name = "emoji", nullable = false)
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