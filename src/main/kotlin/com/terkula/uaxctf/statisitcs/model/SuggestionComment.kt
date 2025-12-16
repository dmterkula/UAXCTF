package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "suggestion_comments", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class SuggestionComment(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "suggestion_uuid", nullable = false)
    var suggestionUuid: String,

    @Column(name = "username", nullable = false)
    var username: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    var message: String,

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
