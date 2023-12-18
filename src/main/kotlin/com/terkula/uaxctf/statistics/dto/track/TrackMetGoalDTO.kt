package com.terkula.uaxctf.statistics.dto.track

import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import com.terkula.uaxctf.statistics.response.track.TrackMeetPerformanceResponse

class TrackMetGoalDTO(val event: String, val time: String, val meet: TrackMeet, val result: TrackMeetPerformanceResponse)