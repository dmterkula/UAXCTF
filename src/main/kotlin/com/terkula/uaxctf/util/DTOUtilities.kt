package com.terkula.uaxctf.util

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO

class DTOUtilities {

}

fun getImprovedUpon(list: List<MeetPerformanceDTO>): MeetPerformanceDTO? {
    return if (list.size >= 2) {
        list[1]
    } else {
        null
    }
}