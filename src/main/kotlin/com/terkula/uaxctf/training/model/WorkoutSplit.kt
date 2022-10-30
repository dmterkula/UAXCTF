package com.terkula.uaxctf.training.model

import com.terkula.uaxctf.util.calculateSecondsFrom
import javax.persistence.*

@Entity
@Table(name = "workout_split", schema = "uaxc")
class WorkoutSplit (
                    @Column(name= "workout_id")
                    val workoutId: Int,
                    @Column(name= "runner_id")
                    val runnerId: Int,
                    val distance: Int,
                    val time: String,
                    @Column(name= "split_number")
                    val splitNumber: Int) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}

@Entity
@Table(name = "workout_split_v2", schema = "uaxc")
class WorkoutSplitV2 (
        @Column(name= "component_uuid")
        val componentUUID: String,
        @Column(name= "runner_id")
        val runnerId: Int,
        val uuid: String,
        var number: Int,
        var value: String
    ) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}

fun List<WorkoutSplit>.calculateSpread(): Double {

    val list = this.map { it.time.calculateSecondsFrom() }
    return (list.maxOrNull()!! - list.minOrNull()!!)

}

fun List<WorkoutSplitV2>.calculateSpreadV2(): Double {

    val list = this.map { it.value.calculateSecondsFrom() }
    return (list.maxOrNull()!! - list.minOrNull()!!)

}