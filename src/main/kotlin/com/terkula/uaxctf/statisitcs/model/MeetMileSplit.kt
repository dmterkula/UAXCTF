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
                      var mileOne: String,
                      @Column(name = "second_mile")
                      var mileTwo: String,
                      @Column(name = "third_mile")
                      var mileThree: String) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0

}



fun MeetMileSplit.isConsistentRace(): Boolean {

    val splits: List<Double> = listOf(mileOne.calculateSecondsFrom(),
            mileTwo.calculateSecondsFrom(),
            mileThree.calculateSecondsFrom())

    val max = splits.maxOrNull()
    val min = splits.minOrNull()

    return if (max != null && min != null) {
        val spread = max!! - min!!

        spread <= 20
    } else {
        false
    }

}

fun MeetMileSplit.matches(splits: List<MeetMileSplit>): Boolean {
   splits.forEach {
       if (this.meetId == it.meetId && this.runnerId == it.runnerId) {
           return true
       }
   }
    return false
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

    return list.maxOrNull()!! - list.minOrNull()!!

}

fun List<MeetMileSplit>.calculateAverageFirstToSecondMileDifference(): Double {
    return if (this.isEmpty()) {
        0.0
    } else {
        this.map { it.calculateFirstToSecondMileDifference() }.average().round(2)
    }
}

fun List<MeetMileSplit>.calculateAverageSecondToThirdMileDifference(): Double {
    return if (this.isEmpty()) {
        0.0
    } else {
        this.map { it.calculateSecondToThirdMileDifference() }.average().round(2)
    }
}

fun List<MeetMileSplit>.calculateTotalAverageDifference(): Double {
    return if (this.isEmpty()) {
        0.0
    } else {
        return this.map { it.calculateTotalDifferenceBetween() }.average().round(2)
    }

}

fun List<MeetMileSplit>.calculateAverageSpread(): Double {

    return if (this.isEmpty()) {
        0.0
    } else {
        this.map { it.calculateSpread() }.average().round(2)
    }
}