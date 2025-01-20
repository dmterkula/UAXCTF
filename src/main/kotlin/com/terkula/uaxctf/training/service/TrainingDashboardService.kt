package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.repository.track.TrackMeetLogRepository
import com.terkula.uaxctf.statistics.service.MeetLogService
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.model.TrainingRunResult
import com.terkula.uaxctf.training.repository.MeetLogRepository
import com.terkula.uaxctf.training.response.MeetLogResponse
import com.terkula.uaxctf.training.response.RunnerWorkoutResultResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordProfileResponse
import com.terkula.uaxctf.training.response.getTotalDistance
import com.terkula.uaxctf.training.response.getTotalSeconds
import com.terkula.uaxctf.training.response.track.TrackMeetLogResponse
import com.terkula.uaxctf.training.response.trainingdashboard.RunnerTrainingDashboardEntry
import com.terkula.uaxctf.training.response.trainingdashboard.TrainingDashboardResponse
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.getYearString
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.stereotype.Service
import java.sql.Date

@Service
class TrainingDashboardService(
        val runnerService: RunnerService,
        val trainingRunsService: TrainingRunsService,
        val crossTrainingService: CrossTrainingService,
        val workoutSplitService: WorkoutSplitService,
        val meetLogService: MeetLogService

) {

    fun getTrainingDashboard(season: String, startDate: Date, endDate: Date, team: String?): TrainingDashboardResponse {


        val runners = if (team != null && !team.equals("UA", ignoreCase = true)) {
            runnerService.getRunnersByTeam(team)
        } else if (season == "track") {
            runnerService.getTrackRoster(true, startDate.getYearString())
        } else {
            runnerService.getXcRoster(true, startDate.getYearString())
        }

        val dashboardRows: List<RunnerTrainingDashboardEntry> = runners.map {

            val trainingRuns: List<TrainingRunResult> = trainingRunsService.getARunnersTrainingRunsByTypeWithinDates(it.id, startDate, endDate, season).trainingRunResults
            val crossTrainingWorkouts: List<CrossTrainingRecordProfileResponse> = crossTrainingService.getCrossTrainingActivitiesForRunnerBetweenDatesForSeason(it.id, startDate, endDate, season)
            val workouts: List<RunnerWorkoutResultResponse>  = workoutSplitService.getAllARunnersWorkoutResultsBySeason(it.id, startDate.getYearString(), startDate, endDate, season)
            val meetLogs = meetLogService.getRunnerMeetLogsBetweenDates(it.id, startDate, endDate)
            val xcMeetLogs = meetLogs.first.filter{ log -> log.meetLog != null }
            val trackMeetLogs = meetLogs.second

            val totalLoggedRuns: Int = trainingRuns.size + workouts.size + xcMeetLogs.size + trackMeetLogs.map { trackLogs -> trackLogs.trackMeetLogs }.flatten().size

            val trainingRunMiles = trainingRuns.map { it.results.map { trainingRun-> trainingRun.getTotalDistance() } }.flatten().sum()
            val workoutMiles = workouts.sumOf { it.getTotalDistance() }
            val xcMeetMiles = xcMeetLogs.sumOf { it.meetLog!!.getTotalDistance() }
            val trackMeetMiles = trackMeetLogs.map{ it.trackMeetLogs.map{ it.getTotalDistance()} }.flatten().sum()

            val weeklyMiles = trainingRunMiles + workoutMiles + trackMeetMiles + xcMeetMiles
            var weeklyTrainingRunAverageEffort = trainingRuns.asSequence().map { trainingRuns -> trainingRuns.results.map { result-> result.effortLevel } }.flatten().filter { effort-> effort != null}.map{ effort -> effort!!}.average()
            if (weeklyTrainingRunAverageEffort.isNaN()) {
                weeklyTrainingRunAverageEffort = 0.0
            }

            val trainingRunTotalSeconds = trainingRuns.map { it.results.map { it.time.calculateSecondsFrom() } }.flatten().sum()
            val workoutTotalSeconds = workouts.map { it.getTotalSeconds() }.sum()
            val totalXcMeetTime =  xcMeetLogs.map { it.meetLog!!.getTotalTimeSeconds() }.sum()
            val totalTrackMeetTime = trackMeetLogs.map { it.trackMeetLogs.map { it.getTotalTimeSeconds() } }.flatten().sum()

            val totalSeconds = trainingRunTotalSeconds + workoutTotalSeconds + totalXcMeetTime + totalTrackMeetTime

            val weeklyAveragePace = if (weeklyMiles > 0) {
                (totalSeconds / weeklyMiles).toMinuteSecondString()
            } else {
                "00:00"
            }

            return@map RunnerTrainingDashboardEntry(
                    it,
                    totalLoggedRuns,
                    weeklyMiles,
                    weeklyAveragePace,
                    weeklyTrainingRunAverageEffort,
                    trainingRuns.map{it.results}.flatten(),
                    workouts,
                    crossTrainingWorkouts,
                    xcMeetLogs,
                    trackMeetLogs.filter { it.trackMeetLogs.isNotEmpty() }
            )


        }
                .sortedByDescending { it.weeklyMiles }
        return TrainingDashboardResponse(dashboardRows)
    }


}