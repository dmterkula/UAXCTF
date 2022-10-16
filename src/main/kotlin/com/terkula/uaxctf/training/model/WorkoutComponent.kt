package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "workout_component", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class WorkoutComponent(
        var uuid: String,
        @Column(name = "workout_uuid")
        var workoutUuid: String,
        var type: String,
        var description: String,
        @Column(name = "target_distance")
        var targetDistance: Int,
        @Column(name = "target_count")
        var targetCount: Int,
        @Column(name = "target_pace")
        var pace: String,
        @Column(name = "target_duration")
        var duration: String?,
        @Column(name = "target_pace_adjustment")
        var targetPaceAdjustment: String = ""
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

}