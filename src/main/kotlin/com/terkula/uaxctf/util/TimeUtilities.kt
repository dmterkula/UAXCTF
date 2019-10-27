package com.terkula.uaxctf.util

import java.sql.Date
import java.util.Calendar

fun String.calculateSecondsFrom(): Double {

    val splitTime = this.split(":")
    assert(splitTime.size == 2)

    return if (this.contains('-')) {
        splitTime[0].toDouble() * 60 + splitTime[1].toDouble() *-1
    } else {
        splitTime[0].toDouble() * 60 + splitTime[1].toDouble()
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

fun Date.getFirstDayOfYear(): Date {
    return Date.valueOf(this.getYearString() + "-01-01")

}

fun Date.getLastDayOfYear(): Date {
    return Date.valueOf(this.getYearString() + "-12-31")

}