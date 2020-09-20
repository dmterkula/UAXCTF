package com.terkula.uaxctf.util

import com.terkula.uaxctf.statisitcs.model.Runner
import java.sql.Date
import java.util.Calendar

class TimeUtilities {

    companion object {
        fun getFirstDayOfYear(): Date {
            return Date.valueOf(Date(System.currentTimeMillis()).getYearString() + "-01-01")

        }

        fun getLastDayOfYear(): Date {
            return Date.valueOf(Date(System.currentTimeMillis()).getYearString() + "-12-31")
        }
    }
}


fun String.calculateSecondsFrom(): Double {

    val splitTime = this.split(":")
    assert(splitTime.size == 2)

    return if (this.contains('-')) {
        splitTime[0].toDouble() * 60 + splitTime[1].toDouble() *-1
    } else {
        try {
            splitTime[0].toDouble() * 60 + splitTime[1].toDouble()
        } catch (e: Exception) {
            return 0.0;
        }

    }
}

fun Double.round(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()

fun Double.toPaddedString(): String {
    return if (this < 10) {
        "0$this"
    } else {
        this.toString()
    }
}

fun Double.toMinuteSecondString(): String {
    val minutes: Int = (this / 60).toInt()
    var seconds = (this % 60).round(1)
    return if (this < 0) {
        seconds *= -1
        val secondsString = seconds.toPaddedString()
        "-" + minutes *-1 + ":" + secondsString
    } else {
        minutes.toString() + ":" + seconds.toPaddedString()
    }

}

fun substractDays(date: Date, days: Int): Date {
    val c = Calendar.getInstance()
    c.time = date
    c.add(Calendar.DATE, days *-1)
    return Date(c.timeInMillis)
}

fun String.convertHourMileSplitToMinuteSecond(): String {

    var corrected = this

    if (corrected[0] == '0') {
        corrected = corrected.substring(1)
    }

    if (corrected[0] == ':') {
        corrected = corrected.substring(1)
    }

    if(corrected[0] == '0') {
        corrected = corrected.substring(1)
    }

    return corrected

}

fun Date.getYearString(): String {
    return this.toLocalDate().year.toString()
}

fun scaleTo5k(distance: Double, time: Double): Double {

    // time1/dist1 = time2/dist2

    return (time / distance) * 5000

}

fun String.toPlace(): Int {

    return try {
        this.toInt()
    } catch (e: Exception) {
         0
    }

}

fun String.isValidTime(): Boolean {
    return this.appendDecimal().matches(Regex("\\d\\d:\\d\\d.\\d+"))
}

fun String.isValidMileSplit(): Boolean {
    return this.appendDecimal().matches(Regex("\\d:\\d\\d.\\d+")) || this.isValidTime()
}

fun String.appendDecimal(): String {
    return if (this.contains(".")) this
    else "$this.0"
}

fun MutableList<Pair<Runner, Double>>.calculateSpreadWith(otherRunner: Pair<Runner, Double>): Double {

    if (this.isEmpty()) {
        return 0.0
    }

    this.add(otherRunner)
    val times = this.map {
        it.second
    }

    val spread = times.max()!! - times.min()!!

    this.remove(otherRunner)

    return spread
}