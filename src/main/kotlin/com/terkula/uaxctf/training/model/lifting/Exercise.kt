package com.terkula.uaxctf.training.model.lifting

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "exercises", schema = "uaxc")
@JsonInclude(JsonInclude.Include.NON_NULL)
class Exercise(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "category", nullable = false)
    var category: String, // UPPER_BODY, LOWER_BODY, CORE, FULL_BODY, PLYOMETRIC, FLEXIBILITY

    @Column(name = "exercise_type", nullable = false)
    var exerciseType: String, // WEIGHT, BODYWEIGHT, DURATION

    @Column(name = "is_global", nullable = false)
    var isGlobal: Boolean = false,

    @Column(name = "team")
    var team: String?,

    @Column(name = "created_by")
    var createdBy: String?,

    @Column(name = "default_weight_unit", nullable = false)
    var defaultWeightUnit: String = "lbs",

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
