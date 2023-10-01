package com.terkula.uaxctf.statistics.response.track

import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO
import kotlin.reflect.KFunction1


enum class TrackSortingMethodContainer(val value: String, val sortingFunction: KFunction1<MutableList<TrackMeetPerformanceDTO>, MutableList<TrackMeetPerformanceDTO>>) {
//    RECENT_DATE("latest", MutableList<TrackMeetPerformanceDTO>::sortedByMostRecentDate),
//    OLDER_DATE("furthest", MutableList<TrackMeetPerformanceDTO>::sortedByOlderDate),
//    TIME("time", MutableList<TrackMeetPerformanceDTO>::sortedByTime),
    NOSORT("", MutableList<TrackMeetPerformanceDTO>::defaultSort)
}

//fun MutableList<TrackMeetPerformanceDTO>.sortedByTime(): MutableList<TrackMeetPerformanceDTO> {
//    return this.sortedBy{ it.time }.toMutableList()
//}
//
//fun MutableList<TrackMeetPerformanceDTO>.sortedByOlderDate(): MutableList<TrackMeetPerformanceDTO> {
//    return this.sortedBy{ it.meetDate }.toMutableList()
//}
//
//fun MutableList<TrackMeetPerformanceDTO>.sortedByMostRecentDate(): MutableList<TrackMeetPerformanceDTO> {
//    return this.sortedBy{ it.meetDate }.toMutableList()
//}

fun MutableList<TrackMeetPerformanceDTO>.defaultSort(): MutableList<TrackMeetPerformanceDTO> {
    return this
}