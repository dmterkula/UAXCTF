package com.terkula.uaxctf.training.model.lifting

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
    name = "lifting_prs",
    schema = "uaxc",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["runner_id", "exercise_uuid", "pr_type"])
    ]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
class LiftingPR(
    @Column(name = "uuid", unique = true, nullable = false)
    var uuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int,

    @Column(name = "exercise_uuid", nullable = false)
    var exerciseUuid: String,

    @Column(name = "pr_type", nullable = false)
    var prType: String, // MAX_WEIGHT or MAX_DURATION

    @Column(name = "weight")
    var weight: Double?,

    @Column(name = "weight_unit")
    var weightUnit: String?,

    @Column(name = "rep_number")
    var repNumber: Int?,

    @Column(name = "duration")
    var duration: String?,

    @Column(name = "achieved_date", nullable = false)
    var achievedDate: Timestamp,

    @Column(name = "lifting_record_uuid", nullable = false)
    var liftingRecordUuid: String,

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
