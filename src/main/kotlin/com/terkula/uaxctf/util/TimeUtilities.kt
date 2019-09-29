package com.terkula.uaxctf.util

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