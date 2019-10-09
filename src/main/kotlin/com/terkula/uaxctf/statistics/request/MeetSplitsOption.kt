package com.terkula.uaxctf.statistics.request

enum class MeetSplitsOption(val value: String) {

    FirstToSecondMile("firstToSecond"),
    SecondToThirdMile("secondToThird"),
    Spread("spread"),
    Combined("combined");

    companion object {
        private val map = MeetSplitsOption.values().associateBy(MeetSplitsOption::value)
        fun fromString(type: String) = map[type]
    }


}
