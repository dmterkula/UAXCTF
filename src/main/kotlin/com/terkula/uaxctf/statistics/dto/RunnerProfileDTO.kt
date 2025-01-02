package com.terkula.uaxctf.statistics.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedSeasonBestDTO
import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO
import com.terkula.uaxctf.statistics.response.achievement.RunnerAchievementsDTO
import com.terkula.uaxctf.training.dto.RunnerWorkoutResultsDTO
import com.terkula.uaxctf.training.model.DateRangeRunSummaryDTO
import com.terkula.uaxctf.training.model.TrainingRunResult
import com.terkula.uaxctf.training.response.RankedRunnerDistanceRunDTO
import com.terkula.uaxctf.training.response.RunnerWorkoutResultResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordProfileResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordResponse

@JsonInclude(JsonInclude.Include.NON_NULL)
class RunnerProfileDTO(
           val runner: Runner,
           val adjustedTimeTrialTime: String?,
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


@JsonInclude(JsonInclude.Include.NON_NULL)
class RunnerProfileDTOV2(
        val runner: Runner,
        val rankedPRDTO: RankedPRDTO?,
        val rankedSBDTO: RankedSeasonBestDTO?,
        val raceConsistencyDTO: RankedRunnerConsistencyDTO?,
        val rankedDistanceRunDTO: RankedRunnerDistanceRunDTO?,
        val timeTrialImprovementDTO: TimeTrialImprovementDTO?,
        val goals: List<XcGoal>,
        val trainingRuns: List<TrainingRunResult>,
        val workouts: List<RunnerWorkoutResultResponse>,
        val meetResults: List<MeetPerformanceDTO>,
        val trackMeetResults: List<TrackMeetPerformanceDTO>,
        var trainingRunSummary: List<DateRangeRunSummaryDTO>,
        val achievements: RunnerAchievementsDTO,
        val trackPRs: TrackPerformancesDTO,
        val trackSBs: TrackPerformancesDTO,
        val crossTrainingRecords: List<CrossTrainingRecordProfileResponse>
)