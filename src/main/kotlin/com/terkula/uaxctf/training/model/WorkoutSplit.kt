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

fun List<WorkoutSplit>.calculateSpread(): Double {

    val list = this.map { it.time.calculateSecondsFrom() }
    return (list.max()!! - list.min()!!)

}