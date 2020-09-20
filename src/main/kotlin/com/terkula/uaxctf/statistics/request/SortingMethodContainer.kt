package com.terkula.uaxctf.statistics.request

import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.util.calculateSecondsFrom
import kotlin.reflect.KFunction1


enum class SortingMethodContainer(val value: String, val sortingFunction: KFunction1<MutableList<MeetPerformanceDTO>, MutableList<MeetPerformanceDTO>>) {
    RECENT_DATE("latest", MutableList<MeetPerformanceDTO>::sortedByMostRecentDate),
    OLDER_DATE("furthest", MutableList<MeetPerformanceDTO>::sortedByOlderDate),
    TIME("time", MutableList<MeetPerformanceDTO>::sortedByTime),
    NOSORT("", MutableList<MeetPerformanceDTO>::defaultSort)
}

fun MutableList<MeetPerformanceDTO>.sortedByTime(): MutableList<MeetPerformanceDTO> {
    return this.sortedBy{ it.time.calculateSecondsFrom() }.toMutableList()
}

fun MutableList<MeetPerformanceDTO>.sortedByOlderDate(): MutableList<MeetPerformanceDTO> {
    return this.sortedBy{ it.meetDate }.toMutableList()
}

fun MutableList<MeetPerformanceDTO>.sortedByMostRecentDate(): MutableList<MeetPerformanceDTO> {
    return this.sortedBy{ it.meetDate }.toMutableList()
}

fun MutableList<MeetPerformanceDTO>.defaultSort(): MutableList<MeetPerformanceDTO> {
    return this
}