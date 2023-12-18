package com.terkula.uaxctf.statistics.response.track

import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO


class TrackMeetSummaryResponse(val prs: List<TrackPerformancesDTO>, val sbs: List<TrackPerformancesDTO>, val metGoals: TrackMetGoalsResponse)