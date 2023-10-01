package com.terkula.uaxctf.statistics.dto.track

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO

class TrackSBDTO (val runner: Runner, val seasonBests: Map<String, List<TrackMeetPerformanceDTO>>)