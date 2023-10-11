package com.terkula.uaxctf.statistics.dto.track

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO

class TrackPRsDTO (val runner: Runner, val prs: List<TrackPRPerformance>)

class TrackPRPerformance(val event: String, val meet: TrackMeet, val pr: TrackMeetPerformance, val previousPR: TrackMeetPerformance?, val timeDifference: String)