package com.terkula.uaxctf.training.model.trainingbase

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "runners_base_training_percentages", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class  RunnerBaseTrainingPercentage (
        @Column(name = "runner_id")
        var runnerId: Int,
        @Column(name = "event")
        var event: String,
        @Column(name = "pace_type")
        var paceType: String,
        @Column(name = "pace_name")
        var paceName: String,
        @Column(name = "percent")
        var percent: Int,
        @Column(name = "season")
        var season: String,
        @Column(name = "year")
        var year: String,
        @Column(name = "uuid")
        var uuid: String

) {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}