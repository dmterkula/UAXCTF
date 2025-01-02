package com.terkula.uaxctf.training.model.CrossTraining

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "cross_training_records", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class CrossTrainingRecord(
        var uuid: String,
        @Column(name = "cross_training_uuid")
        var crossTrainingUuid: String,
        @Column(name = "runner_id")
        var runnerId: Int,
        var distance: Double,
        var time: String,
        var notes: String?,
        @Column(name = "coaches_notes")
        var coachesNotes: String?,
        @Column(name = "effort_level")
        var effortLevel: Double?,
        @Column(name = "avg_hr")
        var avgHr: Int?,
        @Column(name = "max_hr")
        var maxHr: Int?,
        @Column(name = "avg_power")
        var avgPower: Int?,
        @Column(name = "max_power")
        var maxPower: Int?,
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}