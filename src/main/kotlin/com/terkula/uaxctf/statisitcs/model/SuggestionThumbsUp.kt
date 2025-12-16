package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
    name = "suggestion_thumbs_up",
    schema = "uaxc",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["suggestion_uuid", "runner_id"])
    ]
)
class SuggestionThumbsUp(
    @Column(name = "suggestion_uuid", nullable = false)
    var suggestionUuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

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
