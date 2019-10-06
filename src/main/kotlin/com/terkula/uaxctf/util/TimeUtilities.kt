package com.terkula.uaxctf.util

import java.sql.Date
import java.util.Calendar



fun String.calculateSecondsFrom(): Double {
    val splitTime = this.split(":")
    assert(splitTime.size == 2)
    return splitTime[0].toDouble() * 60 + splitTime[1].toDouble()
}

fun Double.round(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()

fun Double.toPaddedString(): String {
    return if (this < 10) {
        "0$this"
    } else {
        this.toString()
    }
}

fun Double.toMinutesAndSeconds(): String {

    val minutes = (this / 60).toInt()
    val seconds = this % 60

    if (minutes <= 0) {
        return seconds.round(2).toString()
    } else {
        if (seconds < 10)
            if(seconds < 1) {
                return minutes.toString() + ":" + "00" + seconds.toString()
            }
        return minutes.toString() + ":" + seconds.toString()
    }
}

fun Double.toMinuteSecondString(): String {
    val minutes: Int = (this / 60).toInt()
    var seconds = (this % 60).round(1)
    return if (this < 1) {
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
    val test = "0:07:22"
    val test1 = "07:22"

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
