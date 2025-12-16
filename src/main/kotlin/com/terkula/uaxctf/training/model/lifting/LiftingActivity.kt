package com.terkula.uaxctf.training.model.lifting

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Date
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "lifting_activities", schema = "uaxc")
@JsonInclude(JsonInclude.Include.NON_NULL)
class LiftingActivity(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "date", nullable = false)
    var date: Date,

    @Column(name = "duration")
    var duration: String?,

    @Column(name = "icon", nullable = false)
    var icon: String = "dumbbell",

    @Column(name = "season", nullable = false)
    var season: String,

    @Column(name = "team", nullable = false)
    var team: String,

    @Column(name = "suggested_exercises", columnDefinition = "TEXT")
    var suggestedExercises: String?, // JSON array of exercise UUIDs

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
