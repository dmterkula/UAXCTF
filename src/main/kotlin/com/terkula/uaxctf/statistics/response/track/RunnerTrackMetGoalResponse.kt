package com.terkula.uaxctf.statistics.response.track

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.track.TrackMetGoalDTO

class RunnerTrackMetGoalDTO (val runner: Runner, val metGoals: List<TrackMetGoalDTO>)

class TrackMetGoalsResponse(val metGoals: List<RunnerTrackMetGoalDTO>)