package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "team_talk_views")
data class TeamTalkView(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0,

    @Column(name = "team_talk_uuid", nullable = false)
    val teamTalkUuid: String,

    @Column(nullable = false)
    val username: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column(nullable = false, length = 10)
    val team: String,

    @Column(name = "viewed_at", nullable = false, updatable = false)
    val viewedAt: Timestamp = Timestamp(System.currentTimeMillis())
)
