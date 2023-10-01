package com.terkula.uaxctf.statistics.dto.track

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO

class TrackPRsDTO (val runner: Runner, val pr: Map<String, List<TrackMeetPerformanceDTO>>)