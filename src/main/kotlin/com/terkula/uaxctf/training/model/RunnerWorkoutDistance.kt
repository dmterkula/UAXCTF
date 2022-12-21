package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "workout_distance", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class RunnerWorkoutDistance(
        @Column(name = "workout_uuid")
        var workoutUuid: String,
        @Column(name = "runner_id")
        var runnerId: Int,
        var distance: Double
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}

