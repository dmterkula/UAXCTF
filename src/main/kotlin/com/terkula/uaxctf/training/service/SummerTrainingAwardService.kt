package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.repository.*
import com.terkula.uaxctf.training.response.runnerseasontrainingcount.RunnerSeasonTrainingCount
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.subtractDays
import org.springframework.stereotype.Service

@Service
class SummerTrainingAwardService(
        val runnerService: RunnerService,
        val runnerRepository: RunnerRepository,
        val trainingRunRepository: TrainingRunRepository,
        val runnersTrainingRunRepository: RunnersTrainingRunRepository,
        val workoutRepository: WorkoutRepository,
        val workoutDistanceRepository: RunnerWorkoutDistanceRepository,
        val crossTrainingRepository: CrossTrainingRepository,
        val crossTrainingRecordRepository: CrossTrainingRecordRepository

) {

    fun getAllRunnersSeasonTrainingStatus(season: String, year: String, team: String): List<RunnerSeasonTrainingCount> {

        val awardMiles = 200.0
        val awardTrainingCount = 40

        var runners = if (season == "track") {
            runnerService.getTrackRoster(true, year)
        } else {
            runnerService.getXcRoster(true, year)
        }.map{
            it.id to it
        }.toMap()

        if (team == "NU") {
            runners = runnerService.getRunnersByTeam(team).map {
                it.id to it
            }.toMap()
        }

        var startDate = TimeUtilities.getFirstDayOfGivenYear(year)
        var endDate = TimeUtilities.getLastDayOfGivenYear(year)

        if (season == "track") {
            startDate = startDate.subtractDays(90)
            endDate = endDate.subtractDays(180)
        }

        val allTrainingRuns = trainingRunRepository.findByDateBetweenAndSeasonAndTeam(startDate, endDate, season, team)

        val runnersToCount: MutableMap<Runner, Pair<Int, Double>> = mutableMapOf()

        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuid(it.uuid)
                    .forEach { loggedRun ->
                        val entry = runnersToCount[runners[loggedRun.runnerId]]
                        if (entry == null) {
                            if (runners[loggedRun.runnerId] != null) {
                                runnersToCount[runners[loggedRun.runnerId]!!] = 1 to loggedRun.getTotalDistance()
                            }

                        } else {
                            if (runners[loggedRun.runnerId] != null) {
                                runnersToCount[runners[loggedRun.runnerId]!!] = (entry.first + 1 ) to (entry.second + loggedRun.getTotalDistance())
                            }

                        }
                    }
        }

        val workouts = workoutRepository.findByDateBetweenAndSeasonAndTeam(startDate, endDate, season, team)

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuid(it.uuid)
                    .forEach { distance ->
                        val entry = runnersToCount[runners[distance.runnerId]]
                        if (entry == null) {
                            if (runners[distance.runnerId] != null) {
                                runnersToCount[runners[distance.runnerId]!!] = 1 to distance.getTotalDistance()
                            }

                        } else {
                            if (runners[distance.runnerId] != null) {
                                runnersToCount[runners[distance.runnerId]!!] = (entry.first + 1) to (entry.second + distance.getTotalDistance())
                            }

                        }
                    }

        }

        val crossTrainingRecords = crossTrainingRepository.findByDateBetweenAndSeasonAndTeam(startDate, endDate, season, team)
                .filter { it.crossTrainingType == "Elliptical" || it.crossTrainingType == "Bike" || it.crossTrainingType == "Swim"}

        crossTrainingRecords.forEach {
            crossTrainingRecordRepository.findByCrossTrainingUuid(it.uuid)
                    .forEach { crossTraining ->
                        val entry = runnersToCount[runners[crossTraining.runnerId]]
                        if (entry == null) {
                            if (runners[crossTraining.runnerId] != null) {
                                runnersToCount[runners[crossTraining.runnerId]!!] = 1 to 0.0
                            }

                        } else {
                            if (runners[crossTraining.runnerId] != null) {
                                runnersToCount[runners[crossTraining.runnerId]!!] = (entry.first + 1) to (entry.second)
                            }

                        }
                    }
        }

        return runnersToCount.map {
            RunnerSeasonTrainingCount(it.key, it.value.first, it.value.second, awardTrainingCount, awardMiles)
        }.sortedByDescending { it.trainingMiles }

    }

    fun getSeasonTrainingCountsForRunner(runnerId: Int, season: String, year: String, team: String): RunnerSeasonTrainingCount? {
        val awardMiles = 200.0
        val awardTrainingCount = 40

        var runners = if (season == "track") {
            runnerService.getTrackRoster(true, year)
        } else {
            runnerService.getXcRoster(true, year)
        }.map{
            it.id to it
        }.toMap()

        if (team == "NU") {
            runners = runnerService.getRunnersByTeam(team).map {
                it.id to it
            }.toMap()
        }

        var startDate = TimeUtilities.getFirstDayOfGivenYear(year)
        var endDate = TimeUtilities.getLastDayOfGivenYear(year)

        if (season == "track") {
            startDate = startDate.subtractDays(90)
            endDate = endDate.subtractDays(180)
        }

        val allTrainingRuns = trainingRunRepository.findByDateBetweenAndSeasonAndTeam(startDate, endDate, season, team)

        val runnersToCount: MutableMap<Runner, Pair<Int, Double>> = mutableMapOf()

        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { loggedRun ->
                        val entry = runnersToCount[runners[loggedRun.runnerId]]
                        if (entry == null) {
                            runnersToCount[runners[loggedRun.runnerId]!!] = 1 to loggedRun.getTotalDistance()
                        } else {
                            runnersToCount[runners[loggedRun.runnerId]!!] = (entry.first + 1 ) to (entry.second + loggedRun.getTotalDistance())
                        }
                    }
        }

        val workouts = workoutRepository.findByDateBetweenAndSeasonAndTeam(startDate, endDate, season, team)

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { distance ->
                        val entry = runnersToCount[runners[distance.runnerId]]
                        if (entry == null) {
                            runnersToCount[runners[distance.runnerId]!!] = 1 to distance.getTotalDistance()
                        } else {
                            runnersToCount[runners[distance.runnerId]!!] = (entry.first + 1) to (entry.second + distance.getTotalDistance())
                        }
                    }

        }

        val crossTrainingRecords = crossTrainingRepository.findByDateBetweenAndSeasonAndTeam(startDate, endDate, season, team)
                .filter { it.crossTrainingType == "Elliptical" || it.crossTrainingType == "Bike" || it.crossTrainingType == "Swim"}

        crossTrainingRecords.forEach {
            crossTrainingRecordRepository.findByCrossTrainingUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { crossTraining ->
                        val entry = runnersToCount[runners[crossTraining.runnerId]]
                        if (entry == null) {
                            runnersToCount[runners[crossTraining.runnerId]!!] = 1 to 0.0
                        } else {
                            runnersToCount[runners[crossTraining.runnerId]!!] = (entry.first + 1) to (entry.second)
                        }
                    }
        }

        return runnersToCount.map {
            RunnerSeasonTrainingCount(it.key, it.value.first, it.value.second, awardTrainingCount, awardMiles)
        }.firstOrNull()

    }

}

