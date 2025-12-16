package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "suggestions", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class Suggestion(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    var description: String,

    @Column(name = "category", nullable = false)
    var category: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "status", nullable = false)
    var status: String = "under_review",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "status_changed_at")
    var statusChangedAt: Timestamp? = null,

    @Column(name = "team", nullable = false)
    var team: String,

    @Column(name = "season")
    var season: String? = null
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
