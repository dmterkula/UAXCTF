package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import javax.persistence.*

@Entity
@Table(name = "xc_meet_splits", schema = "uaxc")
class MeetMileSplit (
                      @Column(name = "meet_id")
                      val meetId: Int,
                      @Column(name = "runner_id")
                      val runnerId: Int,
                      @Column(name = "first_mile")
                      val mileOne: String,
                      @Column(name = "second_mile")
                      val mileTwo: String,
                      @Column(name = "third_mile")
                      val mileThree: String) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
}

fun MeetMileSplit.calculateFirstToSecondMileDifference(): Double {
    return mileTwo.calculateSecondsFrom() - mileOne.calculateSecondsFrom()
}

fun MeetMileSplit.calculateSecondToThirdMileDifference(): Double {
    return mileThree.calculateSecondsFrom() - mileTwo.calculateSecondsFrom()
}

fun MeetMileSplit.calculateTotalDifferenceBetween(): Double {
    return (mileThree.calculateSecondsFrom() - mileTwo.calculateSecondsFrom()) + (mileTwo.calculateSecondsFrom() - mileOne.calculateSecondsFrom())
}

fun MeetMileSplit.calculateSpread(): Double {

    val list = listOf(this.mileOne.calculateSecondsFrom(), this.mileTwo.calculateSecondsFrom(), this.mileThree.calculateSecondsFrom())

    return list.max()!! - list.min()!!

}

fun List<MeetMileSplit>.calculateAverageFirstToSecondMileDifference(): Double {
    return this.map { it.calculateFirstToSecondMileDifference() }.average().round(2)
}

fun List<MeetMileSplit>.calculateAverageSecondToThirdMileDifference(): Double {
    return this.map { it.calculateSecondToThirdMileDifference() }.average().round(2)
}

fun List<MeetMileSplit>.calculateTotalAverageDifference(): Double {
    return this.map { it.calculateTotalDifferenceBetween() }.average().round(2)
}

fun List<MeetMileSplit>.calculateAverageSpread(): Double {

    return this.map { it.calculateSpread() }.average().round(2)

}