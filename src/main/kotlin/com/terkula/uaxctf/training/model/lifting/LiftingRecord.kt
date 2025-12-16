package com.terkula.uaxctf.training.model.lifting

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "lifting_records", schema = "uaxc")
@JsonInclude(JsonInclude.Include.NON_NULL)
class LiftingRecord(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "lifting_activity_uuid", nullable = false)
    var liftingActivityUuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "exercises_data", columnDefinition = "TEXT", nullable = false)
    var exercisesData: String, // JSON string of LiftingExerciseEntry array

    @Column(name = "total_duration")
    var totalDuration: String?,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String?,

    @Column(name = "coaches_notes", columnDefinition = "TEXT")
    var coachesNotes: String?,

    @Column(name = "effort_level")
    var effortLevel: Double?,

    @Column(name = "date_logged", nullable = false)
    var dateLogged: Timestamp = Timestamp(System.currentTimeMillis()),

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
        if (dateLogged.time == 0L) {
            dateLogged = Timestamp(System.currentTimeMillis())
        }
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }
}
