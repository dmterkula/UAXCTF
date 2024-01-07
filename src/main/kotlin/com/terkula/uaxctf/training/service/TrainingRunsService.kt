package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedAchievementDTO
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.service.MeetLogService
import com.terkula.uaxctf.training.model.*
import com.terkula.uaxctf.training.repository.RunnerWorkoutDistanceRepository
import com.terkula.uaxctf.training.repository.RunnersTrainingRunRepository
import com.terkula.uaxctf.training.repository.TrainingRunRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.request.CreateRunnersTrainingRunRequest
import com.terkula.uaxctf.training.request.CreateTrainingRunRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.training.response.track.TrackMeetLogResponse
import com.terkula.uaxctf.util.*
import org.springframework.stereotype.Service
import java.sql.Date
import java.time.*
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalAdjusters.firstDayOfMonth
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.time.temporal.WeekFields
import java.util.*


@Service
class TrainingRunsService(
    val trainingRunRepository: TrainingRunRepository,
    val runnersTrainingRunRepository: RunnersTrainingRunRepository,
    val runnerRepository: RunnerRepository,
    val workoutRepository: WorkoutRepository,
    val workoutDistanceRepository: RunnerWorkoutDistanceRepository,
    val meetLogService: MeetLogService
) {

    fun getTrainingRuns(startDate: Date, endDate: Date): TrainingRunResponse {
        return TrainingRunResponse(
                trainingRunRepository.findByDateBetween(startDate, endDate).map {
                    TrainingRunDTO(it.date, it.distance, it.time, it.icon, it.uuid,
                            it.name, it.minTime, it.minDistance, it.season)
                }
        )
    }

    fun createTrainingRun(createTrainingRunRequest: CreateTrainingRunRequest): TrainingRunResponse {

        if (trainingRunRepository.findByUuid(createTrainingRunRequest.uuid).isEmpty()) {

            val trainingRun = TrainingRun(
                    createTrainingRunRequest.date,
                    createTrainingRunRequest.distance,
                    createTrainingRunRequest.time,
                    createTrainingRunRequest.icon,
                    createTrainingRunRequest.uuid,
                    createTrainingRunRequest.name,
                    createTrainingRunRequest.minTime,
                    createTrainingRunRequest.minDistance,
                    createTrainingRunRequest.season
            )

            trainingRunRepository.save(
                    trainingRun
            )

            return TrainingRunResponse(listOf(
                    TrainingRunDTO(trainingRun.date, trainingRun.distance, trainingRun.time, trainingRun.icon,
                            trainingRun.uuid, trainingRun.name, trainingRun.minTime, trainingRun.minDistance, trainingRun.season)
            ))

        } else {
            throw RuntimeException("training run already exists for uuid: " + createTrainingRunRequest.uuid)
        }

    }

    fun updateTrainingRun(createTrainingRunRequest: CreateTrainingRunRequest): TrainingRunResponse {

        val foundTrainingRun = trainingRunRepository.findByUuid(createTrainingRunRequest.uuid).firstOrNull()

        if (foundTrainingRun == null) {

            val trainingRun = TrainingRun(
                    createTrainingRunRequest.date,
                    createTrainingRunRequest.distance,
                    createTrainingRunRequest.time,
                    createTrainingRunRequest.icon,
                    createTrainingRunRequest.uuid,
                    createTrainingRunRequest.name,
                    createTrainingRunRequest.minTime,
                    createTrainingRunRequest.minDistance,
                    createTrainingRunRequest.season
            )

            trainingRunRepository.save(
                    trainingRun
            )

            return TrainingRunResponse(listOf(
                    TrainingRunDTO(trainingRun.date, trainingRun.distance, trainingRun.time, trainingRun.icon,
                            trainingRun.uuid, trainingRun.name, trainingRun.minTime, trainingRun.minDistance, trainingRun.season)
            ))

        } else {

            foundTrainingRun.date = createTrainingRunRequest.date
            foundTrainingRun.icon = createTrainingRunRequest.icon
            foundTrainingRun.time = createTrainingRunRequest.time
            foundTrainingRun.distance = createTrainingRunRequest.distance
            foundTrainingRun.name = createTrainingRunRequest.name
            foundTrainingRun.minTime = createTrainingRunRequest.minTime
            foundTrainingRun.minDistance = createTrainingRunRequest.minDistance
            foundTrainingRun.season = createTrainingRunRequest.season

            trainingRunRepository.save(foundTrainingRun)

            return TrainingRunResponse(listOf(
                    TrainingRunDTO(foundTrainingRun.date, foundTrainingRun.distance, foundTrainingRun.time,
                            foundTrainingRun.icon, foundTrainingRun.uuid, foundTrainingRun.name,
                            foundTrainingRun.minTime, foundTrainingRun.minDistance, foundTrainingRun.season)
            ))
        }

    }

    fun deleteTrainingRun(uuid: String): TrainingRunResponse {

        val foundTrainingRun = trainingRunRepository.findByUuid(uuid).firstOrNull()

        if (foundTrainingRun == null) {
            return TrainingRunResponse(emptyList())
        } else {
            val runnerRecords = runnersTrainingRunRepository.findByTrainingRunUuid(uuid)

            if (runnerRecords.isEmpty()) {
                trainingRunRepository.delete(foundTrainingRun)

                return TrainingRunResponse(listOf(TrainingRunDTO(
                        foundTrainingRun.date, foundTrainingRun.distance, foundTrainingRun.time,
                        foundTrainingRun.icon, foundTrainingRun.uuid, foundTrainingRun.name,
                        foundTrainingRun.minTime, foundTrainingRun.minDistance, foundTrainingRun.season
                )))
            } else {
                // if there are training runs logged for this already, don't delete

                return TrainingRunResponse(emptyList())
            }

        }
    }

    fun getRunnersTrainingRun(runnerId: Int, trainingRunUUID: String): RunnersTrainingRunResponse {

        val runner: Optional<Runner> = runnerRepository.findById(runnerId)

        if (!runner.isPresent) {
            return RunnersTrainingRunResponse(emptyList())
        }

        val results = runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(trainingRunUUID, runnerId)

        return RunnersTrainingRunResponse(results.map {
            RunnerTrainingRunDTO(
                    runner.get(), it.uuid, it.trainingRunUuid, it.time, it.distance, it.avgPace, it.notes,
                    it.warmUpTime, it.warmUpDistance, it.warmUpPace, it.coachNotes
            )
        })

    }

    fun getARunnersTrainingRunsWithinDates(runnerId: Int, startDate: Date, endDate: Date): TrainingRunResults {

        val runner: Optional<Runner> = runnerRepository.findById(runnerId)

        if (!runner.isPresent) {
            return TrainingRunResults(emptyList())
        }

        val trainingRuns = trainingRunRepository.findByDateBetween(startDate, endDate).sortedBy { it.date }

        val runnersTrainingRuns = trainingRuns.map {
            TrainingRunResult(it, runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runner.get().id)
                    .map { result -> RunnerTrainingRunDTO(runner.get(), result.uuid, result.trainingRunUuid, result.time, result.distance,
                            result.avgPace, result.notes, result.warmUpTime, result.warmUpDistance, result.warmUpPace, result.coachNotes) })
        }
                .filter { it.results.isNotEmpty() }


        return TrainingRunResults(runnersTrainingRuns)

    }

    fun getARunnersTrainingRunsByTypeWithinDates(runnerId: Int, startDate: Date, endDate: Date, season: String): TrainingRunResults {

        val runner: Optional<Runner> = runnerRepository.findById(runnerId)

        if (!runner.isPresent) {
            return TrainingRunResults(emptyList())
        }

        val trainingRuns = trainingRunRepository.findByDateBetweenAndSeason(startDate, endDate, season).sortedBy { it.date }

        val runnersTrainingRuns = trainingRuns.map {
            TrainingRunResult(it, runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runner.get().id)
                    .map { result -> RunnerTrainingRunDTO(runner.get(), result.uuid, result.trainingRunUuid, result.time, result.distance,
                            result.avgPace, result.notes, result.warmUpTime, result.warmUpDistance, result.warmUpPace, result.coachNotes) })
        }
                .filter { it.results.isNotEmpty() }


        return TrainingRunResults(runnersTrainingRuns)

    }

    fun getAllRunnersTrainingRun(trainingRunUUID: String): RunnersTrainingRunResponse {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val results = runnersTrainingRunRepository.findByTrainingRunUuid(trainingRunUUID)

        return RunnersTrainingRunResponse(results.map {
            RunnerTrainingRunDTO(
                    runners[it.runnerId]!!, it.uuid, it.trainingRunUuid, it.time, it.distance, it.avgPace, it.notes,
                    it.warmUpTime, it.warmUpDistance, it.warmUpPace, it.coachNotes
            )
        })

    }

    fun createRunnersTrainingRun(createRunnersTrainingRunRequest: CreateRunnersTrainingRunRequest): RunnersTrainingRunResponse {

        val runner: Optional<Runner> = runnerRepository.findById(createRunnersTrainingRunRequest.runnerId)

        if (!runner.isPresent) {
            return RunnersTrainingRunResponse(emptyList())
        }

        val runnerRecord =
                runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(createRunnersTrainingRunRequest.trainingRunUUID, createRunnersTrainingRunRequest.runnerId)
                        .firstOrNull()

        if (runnerRecord == null) {

            val insertMe = RunnersTrainingRun(
                    createRunnersTrainingRunRequest.trainingRunUUID,
                    createRunnersTrainingRunRequest.runnerId,
                    createRunnersTrainingRunRequest.time,
                    createRunnersTrainingRunRequest.distance,
                    createRunnersTrainingRunRequest.avgPace,
                    createRunnersTrainingRunRequest.uuid,
                    createRunnersTrainingRunRequest.notes,
                    createRunnersTrainingRunRequest.warmUpTime,
                    createRunnersTrainingRunRequest.warmUpDistance,
                    createRunnersTrainingRunRequest.warmUpPace,
                    createRunnersTrainingRunRequest.coachNotes
            )
            runnersTrainingRunRepository.save(insertMe)

            return RunnersTrainingRunResponse(listOf(RunnerTrainingRunDTO(
                    runner.get(), insertMe.uuid, insertMe.trainingRunUuid, insertMe.time, insertMe.distance,
                    insertMe.avgPace, insertMe.notes, insertMe.warmUpTime, insertMe.warmUpDistance, insertMe.warmUpPace, insertMe.coachNotes
            )))

        } else {

            runnerRecord.distance = createRunnersTrainingRunRequest.distance
            runnerRecord.time = createRunnersTrainingRunRequest.time
            runnerRecord.avgPace = createRunnersTrainingRunRequest.avgPace
            runnerRecord.notes = createRunnersTrainingRunRequest.notes
            runnerRecord.warmUpTime = createRunnersTrainingRunRequest.warmUpTime
            runnerRecord.warmUpDistance = createRunnersTrainingRunRequest.warmUpDistance
            runnerRecord.warmUpPace = createRunnersTrainingRunRequest.warmUpPace
            runnerRecord.coachNotes = createRunnersTrainingRunRequest.coachNotes

            runnersTrainingRunRepository.save(runnerRecord)

            return RunnersTrainingRunResponse(listOf(RunnerTrainingRunDTO(
                    runner.get(), runnerRecord.uuid, runnerRecord.trainingRunUuid, runnerRecord.time, runnerRecord.distance,
                    runnerRecord.avgPace, runnerRecord.notes, runnerRecord.warmUpTime, runnerRecord.warmUpDistance, runnerRecord.warmUpPace,
                    runnerRecord.coachNotes
            )))

        }
    }

    fun updateRunnersTrainingRun(createRunnersTrainingRunRequest: CreateRunnersTrainingRunRequest): RunnersTrainingRunResponse {

        val runner: Optional<Runner> = runnerRepository.findById(createRunnersTrainingRunRequest.runnerId)

        if (!runner.isPresent) {
            return RunnersTrainingRunResponse(emptyList())
        }

        val runnerRecord =
                runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(createRunnersTrainingRunRequest.trainingRunUUID, createRunnersTrainingRunRequest.runnerId)
                        .firstOrNull()

        if (runnerRecord == null) {

            val insertMe = RunnersTrainingRun(
                    createRunnersTrainingRunRequest.trainingRunUUID,
                    createRunnersTrainingRunRequest.runnerId,
                    createRunnersTrainingRunRequest.time,
                    createRunnersTrainingRunRequest.distance,
                    createRunnersTrainingRunRequest.avgPace,
                    createRunnersTrainingRunRequest.uuid,
                    createRunnersTrainingRunRequest.notes,
                    createRunnersTrainingRunRequest.warmUpTime,
                    createRunnersTrainingRunRequest.warmUpDistance,
                    createRunnersTrainingRunRequest.warmUpPace,
                    createRunnersTrainingRunRequest.coachNotes
            )
            runnersTrainingRunRepository.save(insertMe)

            return RunnersTrainingRunResponse(listOf(RunnerTrainingRunDTO(
                    runner.get(), insertMe.uuid, insertMe.trainingRunUuid, insertMe.time, insertMe.distance,
                    insertMe.avgPace, insertMe.notes, insertMe.warmUpTime, insertMe.warmUpDistance, insertMe.warmUpPace,
                    insertMe.coachNotes
            )))

        } else {
            runnerRecord.avgPace = createRunnersTrainingRunRequest.avgPace
            runnerRecord.distance = createRunnersTrainingRunRequest.distance
            runnerRecord.time = createRunnersTrainingRunRequest.time
            runnerRecord.notes = createRunnersTrainingRunRequest.notes
            runnerRecord.coachNotes = createRunnersTrainingRunRequest.coachNotes

            runnersTrainingRunRepository.save(runnerRecord)

            return RunnersTrainingRunResponse(listOf(RunnerTrainingRunDTO(
                    runner.get(), runnerRecord.uuid, runnerRecord.trainingRunUuid, runnerRecord.time,
                    runnerRecord.distance, runnerRecord.avgPace, runnerRecord.notes,
                    runnerRecord.warmUpTime, runnerRecord.warmUpDistance, runnerRecord.warmUpPace,
                    runnerRecord.coachNotes
            )))

        }
    }

    fun deleteRunnersTrainingRun(uuid: String): RunnersTrainingRunResponse {

        val foundTrainingRun = runnersTrainingRunRepository.findByUuid(uuid).firstOrNull()

        if (foundTrainingRun == null) {
            return RunnersTrainingRunResponse(emptyList())
        } else {
            val runnerRecords = runnersTrainingRunRepository.findByUuid(uuid)

            if (runnerRecords.isEmpty()) {
                // nothing to delete, empty response
                return RunnersTrainingRunResponse(emptyList())
            } else {
                val runner = runnerRepository.findById(runnerRecords.first().runnerId).get()
                runnersTrainingRunRepository.deleteByUuid(uuid)
                return RunnersTrainingRunResponse(runnerRecords.map {
                    RunnerTrainingRunDTO(
                            runner, it.uuid, it.trainingRunUuid, it.time, it.distance, it.avgPace, it.notes,
                            it.warmUpTime, it.warmUpDistance, it.warmUpPace, it.coachNotes
                    )
                })
            }

        }
    }

    fun getAllTrainingMilesRunByRunner(season: String, type: String): List<RankedRunnerDistanceRunDTO> {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        var startDate = TimeUtilities.getFirstDayOfGivenYear(season)
        var endDateTraining = Date(System.currentTimeMillis())
        var endDateWorkouts = TimeUtilities.getLastDayOfGivenYear(season)

        if (type == "track") {
            startDate = TimeUtilities.getFirstDayOfGivenYear(season).subtractDays(90)
            endDateTraining = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150)
            endDateWorkouts = TimeUtilities.getLastDayOfGivenYear(season).subtractDays(150)
        }

        val allTrainingRuns = trainingRunRepository.findByDateBetweenAndSeason(startDate, endDateTraining, type)

        val runnersToDistance: MutableMap<Runner, Double> = mutableMapOf()

        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuid(it.uuid)
                    .forEach { loggedRun ->
                        val entry = runnersToDistance[runners[loggedRun.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = loggedRun.getTotalDistance()
                        } else {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = entry + loggedRun.getTotalDistance()
                        }

                    }

        }

        val workouts = workoutRepository.findByDateBetweenAndSeason(startDate, endDateWorkouts, type)

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuid(it.uuid)
                    .forEach { distance ->
                        val entry = runnersToDistance[runners[distance.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[distance.runnerId]!!] = distance.getTotalDistance()
                        } else {
                            runnersToDistance[runners[distance.runnerId]!!] = entry + distance.getTotalDistance()
                        }
                    }

        }

        return runnersToDistance.toList().sortedByDescending { it.second }
                .mapIndexed { index, it ->
                    RankedRunnerDistanceRunDTO(it.first, it.second, index + 1)
                }
    }

    fun getAllTrainingMilesRunByRunnerForCareer(): List<RankedRunnerDistanceRunDTO> {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val allTrainingRuns = trainingRunRepository.findAll()
        val runnersToDistance: MutableMap<Runner, Double> = mutableMapOf()

        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuid(it.uuid)
                    .forEach { loggedRun ->
                        val entry = runnersToDistance[runners[loggedRun.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = loggedRun.getTotalDistance()
                        } else {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = entry + loggedRun.getTotalDistance()
                        }
                    }
        }

        val workouts = workoutRepository.findAll()

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuid(it.uuid)
                    .forEach { distance ->
                        val entry = runnersToDistance[runners[distance.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[distance.runnerId]!!] = distance.getTotalDistance()
                        } else {
                            runnersToDistance[runners[distance.runnerId]!!] = entry + distance.getTotalDistance()
                        }
                    }

        }

        return runnersToDistance.toList().sortedByDescending { it.second }
                .mapIndexed { index, it ->
                    RankedRunnerDistanceRunDTO(it.first, it.second, index + 1)
                }

    }

    fun getRankedRunnersTrainingRunCountForCareer(): List<RankedAchievementDTO> {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val trainingCount = runnersTrainingRunRepository.findAll().groupBy { it.runnerId }
                .map { runners[it.key]!! to it.value.size }.toMap().toMutableMap()


        val workoutCount = workoutDistanceRepository.findAll().groupBy { it.runnerId }
                .map { runners[it.key]!! to it.value.size }.toMap().toMutableMap()

        trainingCount.forEach { (k, v) -> workoutCount.merge(k, v, Int::plus)  }

       return workoutCount
            .map { it.key to it.value }
                .sortedByDescending { it.second }
                .mapIndexed {index, it ->
                    RankedAchievementDTO(it.first, index + 1, it.second)
                }

    }

    fun getRankedRunnersTrainingRunCountForSeason(season: String): List<RankedAchievementDTO> {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val allTrainingRuns = trainingRunRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        val runnersToCount: MutableMap<Runner, Int> = mutableMapOf()

        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuid(it.uuid)
                    .forEach { loggedRun ->
                        val entry = runnersToCount[runners[loggedRun.runnerId]]
                        if (entry == null) {
                            runnersToCount[runners[loggedRun.runnerId]!!] = 1
                        } else {
                            runnersToCount[runners[loggedRun.runnerId]!!] = entry + 1
                        }
                    }
        }

        val workouts = workoutRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuid(it.uuid)
                    .forEach { distance ->
                        val entry = runnersToCount[runners[distance.runnerId]]
                        if (entry == null) {
                            runnersToCount[runners[distance.runnerId]!!] = 1
                        } else {
                            runnersToCount[runners[distance.runnerId]!!] = entry + 1
                        }
                    }

        }

        return runnersToCount.map { it.key to it.value }
                .sortedByDescending { it.second }
                .mapIndexed { index, pair ->
                    RankedAchievementDTO(pair.first, index + 1, pair.second)
                }
    }

    // pair of training distance to logged run count
    fun getAllTrainingMilesRunForARunnerCareer(runnerId: Int): Pair<Double, Int> {

        val runners = runnerRepository.findById(runnerId).get()

        val trainingRuns = runnersTrainingRunRepository.findByRunnerId(runnerId)
        var trainingRunCount = trainingRuns.size
        var trainingDistance = trainingRuns.sumOf { it.getTotalDistance()}

        val workoutDistances = workoutDistanceRepository.findByRunnerId(runnerId)
        trainingRunCount += workoutDistances.size
        trainingDistance += workoutDistances.sumOf { it.getTotalDistance() }

        return Pair(trainingDistance.round(2), trainingRunCount)

    }

    fun getAllTrainingMilesRunForARunner(runnerId: Int, season: String): List<RankedRunnerDistanceRunDTO> {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val allTrainingRuns = trainingRunRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), Date(System.currentTimeMillis()))

        val runnersToDistance: MutableMap<Runner, Double> = mutableMapOf()

        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { loggedRun ->
                        val entry = runnersToDistance[runners[loggedRun.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = loggedRun.getTotalDistance()
                        } else {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = entry + loggedRun.getTotalDistance()
                        }

                    }
        }

        val workouts = workoutRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { distance ->
                        val entry = runnersToDistance[runners[distance.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[distance.runnerId]!!] = distance.getTotalDistance()
                        } else {
                            runnersToDistance[runners[distance.runnerId]!!] = entry + distance.getTotalDistance()
                        }
                    }

        }

        return runnersToDistance.toList().sortedByDescending { it.second }
                .mapIndexed { index, it ->
                    RankedRunnerDistanceRunDTO(it.first, it.second, index + 1)
                }

    }

    fun getTotalDistancePerDay(startDate: Date, endDate: Date, runnerId: Int, includeWarmUps: Boolean, type: String): List<DateRangeRunSummaryDTO> {

        val allTrainingRuns = trainingRunRepository.findByDateBetweenAndSeason(startDate, endDate, type)

        val workouts = workoutRepository.findByDateBetweenAndSeason(startDate, endDate, type)

        val meetLogDatePairs: List<Pair<MeetLogResponse, Date>> = meetLogService.getAllMeetLogsForRunnerInSeason(runnerId, startDate, endDate, type)
        val trackMeetLogDatePairs: List<Pair<TrackMeetLogResponse, Date>> = meetLogService.getAllTrackMeetLogsForRunnerInSeason(runnerId, startDate, endDate, type)

        val dates = workouts.map { it.date }
                .plus(allTrainingRuns.map { it.date })
                .plus(meetLogDatePairs.map { it.second })
                .plus(trackMeetLogDatePairs.map { it.second })
                .sorted()

        if (dates.isEmpty()) {
            return emptyList()
        }

        val trainingSummaryDates: Map<Int, TrainingRunDistanceSummaryDTO> = TimeUtilities.getDatesBetween(dates.first().toLocalDate(), dates.last().toLocalDate())
                .groupBy { it.dayOfYear }
                .toList().sortedBy { it.first }
                .map { it.first to TrainingRunDistanceSummaryDTO(0.0, 0, 0, 0.0, 0.0, it.second.first()) }
                .toMap()


        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { loggedRun ->
                        trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.count + 1
                        trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.trainingCount = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.trainingCount + 1
                        if (includeWarmUps) {
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.totalDistance + loggedRun.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.trainingRunDistance = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.trainingRunDistance + loggedRun.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.avgSecondsPerMile + loggedRun.getTotalTimeSeconds()
                        } else {
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.totalDistance + loggedRun.distance
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.trainingRunDistance = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.trainingRunDistance + loggedRun.distance
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.avgSecondsPerMile + loggedRun.time.calculateSecondsFrom()
                        }

                    }
        }

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { distance ->
                        trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.trainingCount = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.trainingCount + 1
                        if (includeWarmUps) {
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.count + 1
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.totalDistance + distance.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.avgSecondsPerMile + distance.getTotalTimeSeconds()
                        } else {
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.count + 1
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.totalDistance + distance.distance
                            trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().dayOfYear]!!.avgSecondsPerMile + distance.getTimeSeconds()
                        }

                    }
        }


        if (type == "xc") {
            meetLogDatePairs.forEach {
                trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.trainingCount = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.trainingCount + 1
                if (includeWarmUps) {
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance + it.first.meetLog!!.getTotalDistance()
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile + it.first.meetLog!!.getTotalTimeSeconds()
                } else {
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance + it.first.meetLog!!.getOnlyMeetDistance()
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile + it.first.meetLog!!.time.calculateSecondsFrom()
                }
            }
        }


        if (type == "track") {
            trackMeetLogDatePairs.forEach {
                trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.trainingCount = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.trainingCount + 1
                if (includeWarmUps) {
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance + it.first.trackMeetLogs.sumOf { it.getTotalDistance() }
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile + it.first.trackMeetLogs.sumOf{ it.getTotalTimeSeconds() }
                } else {
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance + it.first.trackMeetLogs.sumOf { it.getRaceOnlyDistance() }
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile + it.first.trackMeetLogs.sumOf{it.time.calculateSecondsFrom()}
                }
            }
        }


        return trainingSummaryDates.map {

            val start = LocalDate.ofYearDay(it.value.startDate.getYearString().toInt(), it.key)

            val avgPace =
                    if (it.value.trainingCount != 0) {
                        (it.value.avgSecondsPerMile / it.value.totalDistance)
                    } else {
                        0.0
                    }
            DateRangeRunSummaryDTO(start, start, it.value.totalDistance.round(2), it.value.count, avgPace.toMinuteSecondString())
        }.sortedBy { it.startDate }
    }

    fun getTotalDistancePerWeek(startDate: Date, endDate: Date, runnerId: Int, includeWarmUps: Boolean, type: String): List<DateRangeRunSummaryDTO> {

        val weekOfYear = WeekFields.of(Locale.getDefault()).weekOfYear()

        val allTrainingRuns = trainingRunRepository.findByDateBetweenAndSeason(startDate, endDate, type)

        val workouts = workoutRepository.findByDateBetweenAndSeason(startDate, endDate, type)

        val meetLogDatePairs: List<Pair<MeetLogResponse, Date>> = meetLogService.getAllMeetLogsForRunnerInSeason(runnerId, startDate, endDate, type)
        val trackMeetLogDatePairs: List<Pair<TrackMeetLogResponse, Date>> = meetLogService.getAllTrackMeetLogsForRunnerInSeason(runnerId, startDate, endDate, type)

        val dates = workouts.map { it.date }
                .plus(allTrainingRuns.map { it.date })
                .plus(meetLogDatePairs.map { it.second })
                .plus(trackMeetLogDatePairs.map { it.second })
                .sorted()

        if (dates.isEmpty()) {
            return emptyList()
        }

        val trainingSummaryDates: Map<Int, TrainingRunDistanceSummaryDTO> = TimeUtilities.getDatesBetween(dates.first().toLocalDate(), dates.last().toLocalDate())
                .groupBy { it.get(weekOfYear) }
                .toList().sortedBy { it.first }
                .map { it.first to TrainingRunDistanceSummaryDTO(0.0, 0, 0, 0.0, 0.0, it.second.first()) }
                .toMap()


        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { loggedRun ->
                        trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.count = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.count + 1
                        trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.trainingCount = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.trainingCount + 1
                        if (includeWarmUps) {
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.totalDistance + loggedRun.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.trainingRunDistance = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.trainingRunDistance + loggedRun.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile + loggedRun.getTotalTimeSeconds()
                        } else {
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.totalDistance + loggedRun.distance
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.trainingRunDistance = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.trainingRunDistance + loggedRun.distance
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile + loggedRun.time.calculateSecondsFrom()
                        }
                    }
        }

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { distance ->
                        trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.trainingCount = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.trainingCount + 1
                        if (includeWarmUps) {
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.count = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.count + 1
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.totalDistance + distance.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile + distance.getTotalTimeSeconds()
                        } else {
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.count = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.count + 1
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.totalDistance + distance.distance
                            trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile + distance.getTimeSeconds()
                        }
                    }
        }

        if (type == "xc") {
            meetLogDatePairs.forEach {
                trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.trainingCount = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.trainingCount + 1
                if (includeWarmUps) {
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.count = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.totalDistance + it.first.meetLog!!.getTotalDistance()
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile + it.first.meetLog!!.getTotalTimeSeconds()
                } else {
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.count = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.totalDistance + it.first.meetLog!!.getOnlyMeetDistance()
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile + it.first.meetLog!!.time.calculateSecondsFrom()
                }
            }
        }


        if (type == "track") {
            trackMeetLogDatePairs.forEach {
                trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.trainingCount = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.trainingCount + 1
                if (includeWarmUps) {
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.count = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.totalDistance + it.first.trackMeetLogs.sumOf { it.getTotalDistance() }
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile + it.first.trackMeetLogs.sumOf{ it.getTotalTimeSeconds() }
                } else {
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.count = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.totalDistance + it.first.trackMeetLogs.sumOf { it.getRaceOnlyDistance() }
                    trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().get(weekOfYear)]!!.avgSecondsPerMile + it.first.trackMeetLogs.sumOf{it.time.calculateSecondsFrom()}
                }
            }
        }

        return trainingSummaryDates.map {

            val calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(Calendar.WEEK_OF_YEAR, it.key);
            calendar.set(Calendar.YEAR, it.value.startDate.getYearString().toInt());

            // Now get the first day of week.
            val calendarStart = calendar.time;
            val startDate: LocalDate = LocalDateTime.ofInstant(calendarStart.toInstant(), ZoneId.systemDefault()).toLocalDate()
            // grabbed the start day of the week, now set the end
            calendar.set(Calendar.DAY_OF_WEEK, 7)
            var calendarEnd = calendar.time
            val endDate: LocalDate = LocalDateTime.ofInstant(calendarEnd.toInstant(), ZoneId.systemDefault()).toLocalDate()

            val avgPace =
                    if (it.value.trainingCount != 0) {
                        (it.value.avgSecondsPerMile / it.value.totalDistance)
                    } else {
                        0.0
                    }
            DateRangeRunSummaryDTO(startDate, endDate, it.value.totalDistance.round(2), it.value.count, avgPace.toMinuteSecondString(), it.value.avgSecondsPerMile)
        }.groupBy { it.startDate }
                .map {
                    var trainingCount = it.value.map{it.totalCount}.sum()
                    var totalDistance = it.value.map { it.totalDistance }.sum()
                    var totalSeconds = it.value.map {it.totalSeconds}.sum()

                    val avgPace =
                            if (trainingCount != 0) {
                                (totalSeconds / totalDistance)
                            } else {
                                0.0
                            }
                    DateRangeRunSummaryDTO(it.key, it.value.first().endDate, totalDistance.round(2), trainingCount, avgPace.toMinuteSecondString())
                }.sortedBy { it.startDate }
    }

    fun getTotalDistancePerMonth(startDate: Date, endDate: Date, runnerId: Int, includeWarmUps: Boolean, type: String): List<DateRangeRunSummaryDTO> {

        val allTrainingRuns = trainingRunRepository.findByDateBetweenAndSeason(startDate, endDate, type)

        val workouts = workoutRepository.findByDateBetweenAndSeason(startDate, endDate, type)

        val meetLogDatePairs: List<Pair<MeetLogResponse, Date>> = meetLogService.getAllMeetLogsForRunnerInSeason(runnerId, startDate, endDate, type)
        val trackMeetLogDatePairs: List<Pair<TrackMeetLogResponse, Date>> = meetLogService.getAllTrackMeetLogsForRunnerInSeason(runnerId, startDate, endDate, type)

        val dates = workouts.map { it.date }
                .plus(allTrainingRuns.map { it.date })
                .plus(meetLogDatePairs.map { it.second })
                .plus(trackMeetLogDatePairs.map { it.second })
                .sorted()

        if (dates.isEmpty()) {
            // return emp
            return emptyList()
        }

        val trainingSummaryDates: Map<Int, TrainingRunDistanceSummaryDTO> = TimeUtilities.getDatesBetween(dates.first().toLocalDate(), dates.last().toLocalDate())
                .groupBy { it.monthValue }
                .toList().sortedBy { it.first }
                .map { it.first to TrainingRunDistanceSummaryDTO(0.0, 0, 0, 0.0, 0.0, it.second.first()) }
                .toMap()


        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { loggedRun ->
                        trainingSummaryDates[it.date.toLocalDate().monthValue]!!.count = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.count + 1
                        trainingSummaryDates[it.date.toLocalDate().monthValue]!!.trainingCount = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.trainingCount + 1
                        if (includeWarmUps) {
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.totalDistance + loggedRun.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.trainingRunDistance = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.trainingRunDistance + loggedRun.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.avgSecondsPerMile + loggedRun.getTotalTimeSeconds()
                        } else {
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.totalDistance + loggedRun.distance
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.trainingRunDistance = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.trainingRunDistance + loggedRun.distance
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.avgSecondsPerMile + loggedRun.time.calculateSecondsFrom()
                        }
                    }
        }

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { distance ->
                        trainingSummaryDates[it.date.toLocalDate().monthValue]!!.trainingCount = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.trainingCount + 1
                        if (includeWarmUps) {
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.count = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.count + 1
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.totalDistance + distance.getTotalDistance()
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.avgSecondsPerMile + distance.getTotalTimeSeconds()
                        } else {
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.count = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.count + 1
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.totalDistance = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.totalDistance + distance.distance
                            trainingSummaryDates[it.date.toLocalDate().monthValue]!!.avgSecondsPerMile = trainingSummaryDates[it.date.toLocalDate().monthValue]!!.avgSecondsPerMile + distance.getTimeSeconds()
                        }
                    }
        }

        if (type == "xc") {
            meetLogDatePairs.forEach {
                trainingSummaryDates[it.second.toLocalDate().monthValue]!!.trainingCount = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.trainingCount + 1
                if (includeWarmUps) {
                    trainingSummaryDates[it.second.toLocalDate().monthValue]!!.count = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().monthValue]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.totalDistance + it.first.meetLog!!.getTotalDistance()
                    trainingSummaryDates[it.second.toLocalDate().monthValue]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.avgSecondsPerMile + it.first.meetLog!!.getTotalTimeSeconds()
                } else {
                    trainingSummaryDates[it.second.toLocalDate().monthValue]!!.count = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().monthValue]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.totalDistance + it.first.meetLog!!.getOnlyMeetDistance()
                    trainingSummaryDates[it.second.toLocalDate().monthValue]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.avgSecondsPerMile + it.first.meetLog!!.time.calculateSecondsFrom()
                }
            }
        }


        if (type == "track") {
            trackMeetLogDatePairs.forEach {
                trainingSummaryDates[it.second.toLocalDate().monthValue]!!.trainingCount = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.trainingCount + 1
                if (includeWarmUps) {
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.totalDistance + it.first.trackMeetLogs.sumOf { it.getTotalDistance() }
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.avgSecondsPerMile + it.first.trackMeetLogs.sumOf{ it.getTotalTimeSeconds() }
                } else {
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.count = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.count + 1
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.totalDistance = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.totalDistance + it.first.trackMeetLogs.sumOf { it.getRaceOnlyDistance() }
                    trainingSummaryDates[it.second.toLocalDate().dayOfYear]!!.avgSecondsPerMile = trainingSummaryDates[it.second.toLocalDate().monthValue]!!.avgSecondsPerMile + it.first.trackMeetLogs.sumOf{it.time.calculateSecondsFrom()}
                }
            }
        }



        return trainingSummaryDates.map {


            val start = LocalDate.of(it.value.startDate.getYearString().toInt(), it.key, 1)
                    .with(firstDayOfMonth())

            val end = start.with(lastDayOfMonth())


            val avgPace =
                    if (it.value.trainingCount != 0) {
                        (it.value.avgSecondsPerMile / it.value.totalDistance)
                    } else {
                        0.0
                    }
            DateRangeRunSummaryDTO(start, end, it.value.totalDistance.round(2), it.value.count, avgPace.toMinuteSecondString())
        }.sortedBy { it.startDate }
    }
}