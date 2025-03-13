package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.toMinuteSecondString
import javax.persistence.*

@Entity
@Table(name = "xc_training_run_records", schema = "uaxc")
class RunnersTrainingRun(
    @Column(name = "training_run_uuid")
    val trainingRunUuid: String,
    @Column(name = "runner_id")
    val runnerId: Int,
    var time: String,
    var distance: Double,
    @Column(name = "avg_pace")
    var avgPace: String,
    val uuid: String,
    var notes: String?,
    @Column(name = "warm_up_time")
    var warmUpTime: String?,
    var warmUpDistance: Double?,
    var warmUpPace: String?,
    @Column(name = "coach_notes")
    var coachNotes: String?,
    @Column(name = "effort_level")
    var effortLevel: Double?,
    @Column(name = "pain_level")
    var painLevel: Double?,
    @Column(name = "pain_notes")
    var painNotes: String?,
    @Column(name = "splits")
    var splits: String?
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

    fun splitsList(): List<String>? {
        var splits: List<String>? = null
        if (this.splits != null) {
            splits = this.splits!!.split(",")
        }
        return splits
    }

    fun getTotalDistance(): Double {

        var warmUpDist = 0.0
        if (warmUpDistance != null) {
            warmUpDist = warmUpDistance!!
        }

        return distance + warmUpDist
    }

    fun getTotalTime(): String {
        var warmUpSeconds = 0.0
        if (warmUpTime != null) {
            warmUpSeconds += warmUpTime!!.calculateSecondsFrom()
        }

        return (time.calculateSecondsFrom() + warmUpSeconds).toMinuteSecondString()
    }

    fun getTotalTimeSeconds(): Double {
        var warmUpSeconds = 0.0
        if (warmUpTime != null) {
            warmUpSeconds += warmUpTime!!.calculateSecondsFrom()
        }

        return time.calculateSecondsFrom() + warmUpSeconds
    }
}