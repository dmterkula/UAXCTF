package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "team_talks", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class TeamTalk(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "author", nullable = false, length = 100)
    var author: String,

    @Column(name = "season", nullable = false, length = 20)
    var season: String,

    @Column(name = "year", nullable = false, length = 10)
    var year: String,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(name = "team", nullable = false, length = 20)
    var team: String = "UA",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp = Timestamp(System.currentTimeMillis())
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
        updatedAt = Timestamp(System.currentTimeMillis())
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }
}
