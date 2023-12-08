package com.terkula.uaxctf.statistics.dto.track

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import com.terkula.uaxctf.statistics.response.track.TrackMeetPerformanceResponse

class TrackPerformancesDTO (val runner: Runner, val bestResults: List<TrackTopResult>)

class TrackTopResult(val event: String, val meet: TrackMeet, val best: TrackMeetPerformanceResponse, val previousBest: TrackMeetPerformanceResponse?, val timeDifference: String)