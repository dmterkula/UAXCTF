package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "encouragement", schema = "uaxc")
class Encouragement(
    @Column(name = "uuid", unique = true, nullable = false, length = 36)
    var uuid: String,

    @Column(name = "author_runner_id")
    var authorRunnerId: Int?,

    @Column(name = "author_username", nullable = false)
    var authorUsername: String,

    @Column(name = "author_display_name", nullable = false)
    var authorDisplayName: String,

    @Column(name = "recipient_runner_id")
    var recipientRunnerId: Int?,

    @Column(name = "recipient_name")
    var recipientName: String?,

    @Column(name = "text", nullable = false, length = 150)
    var text: String,

    @Column(name = "team", nullable = false)
    var team: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Timestamp = Timestamp(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

    @PrePersist
    protected fun onCreate() {
        val now = Timestamp(System.currentTimeMillis())
        createdAt = now
        expiresAt = Timestamp(now.time + 24 * 60 * 60 * 1000)
    }
}