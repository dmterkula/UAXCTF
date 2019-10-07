package com.terkula.uaxctf.statistics.dto

import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import com.terkula.uaxctf.util.toMinuteSecondString

class MeetSplitsDTO(val mileOne: String, val mileTwo: String, val mileThree: String) {

    var average: String = (((mileOne.calculateSecondsFrom() + mileTwo.calculateSecondsFrom()
    + mileThree.calculateSecondsFrom())/ 3).round(2)).toMinuteSecondString()

}
