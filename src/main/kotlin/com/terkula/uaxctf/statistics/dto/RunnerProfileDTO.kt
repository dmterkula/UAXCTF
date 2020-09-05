package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.NamedRank
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.ValuedRank
import com.terkula.uaxctf.training.dto.RunnerWorkoutResultsDTO

@JsonInclude(JsonInclude.Include.NON_NULL)
class RunnerProfileDTO(val runner: Runner,
                       val goalTimes: List<String?>,
                       val seasonBest: RunnerMeetSplitDTO?,
                       val PR: RunnerMeetSplitDTO?,
                       val mostConsistentRace: RunnerMeetSplitDTO?,
                       val lastWorkout: RunnerWorkoutResultsDTO?,
                       val workoutConsistencyRank: ValuedRank?,
                       val raceConsistencyRank: ValuedRank?,
                       val combinedConsistencyRank: ValuedRank?,
                       val progressionRank: ValuedRank?,
                       val seasonBestRank: NamedRank?,
                       val upcomingMeetSummary: RunnerMeetSplitDTO?
                       )