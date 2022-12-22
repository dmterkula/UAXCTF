package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.training.model.RunnersTrainingRun
import com.terkula.uaxctf.training.model.TrainingRun
import com.terkula.uaxctf.training.model.TrainingRunResult
import com.terkula.uaxctf.training.model.TrainingRunResults
import com.terkula.uaxctf.training.repository.RunnerWorkoutDistanceRepository
import com.terkula.uaxctf.training.repository.RunnersTrainingRunRepository
import com.terkula.uaxctf.training.repository.TrainingRunRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.request.CreateRunnersTrainingRunRequest
import com.terkula.uaxctf.training.request.CreateTrainingRunRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.util.TimeUtilities
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.sql.Date
import java.util.*

@Service
class TrainingRunsService(
    val trainingRunRepository: TrainingRunRepository,
    val runnersTrainingRunRepository: RunnersTrainingRunRepository,
    val runnerRepository: RunnerRepository,
    val workoutRepository: WorkoutRepository,
    val workoutDistanceRepository: RunnerWorkoutDistanceRepository
) {

    fun getTrainingRuns(startDate: Date, endDate: Date): TrainingRunResponse {
        return TrainingRunResponse(
                trainingRunRepository.findByDateBetween(startDate, endDate).map {
                    TrainingRunDTO(it.date, it.distance, it.time, it.icon, it.uuid, it.name)
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
                createTrainingRunRequest.name
            )

            trainingRunRepository.save(
                    trainingRun
            )

            return TrainingRunResponse(listOf(
                    TrainingRunDTO(trainingRun.date, trainingRun.distance, trainingRun.time, trainingRun.icon, trainingRun.uuid, trainingRun.name)
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
                    createTrainingRunRequest.name
            )

            trainingRunRepository.save(
                    trainingRun
            )

            return TrainingRunResponse(listOf(
                    TrainingRunDTO(trainingRun.date, trainingRun.distance, trainingRun.time, trainingRun.icon, trainingRun.uuid, trainingRun.name)
            ))

        } else {

            foundTrainingRun.date = createTrainingRunRequest.date
            foundTrainingRun.icon = createTrainingRunRequest.icon
            foundTrainingRun.time = createTrainingRunRequest.time
            foundTrainingRun.distance = createTrainingRunRequest.distance
            foundTrainingRun.name = createTrainingRunRequest.name

            trainingRunRepository.save(foundTrainingRun)

            return TrainingRunResponse(listOf(
                    TrainingRunDTO(foundTrainingRun.date, foundTrainingRun.distance, foundTrainingRun.time, foundTrainingRun.icon, foundTrainingRun.uuid, foundTrainingRun.name)
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
                        foundTrainingRun.date, foundTrainingRun.distance,foundTrainingRun.time, foundTrainingRun.icon, foundTrainingRun.uuid, foundTrainingRun.name
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
                    runner.get(), it.uuid, it.trainingRunUuid, it.time, it.distance, it.avgPace
            )
        })

    }

    fun getARunnersTrainingRunsWithinDates(runnerId: Int, trainingRunUUID: String, startDate: Date, endDate: Date): TrainingRunResults {

        val runner: Optional<Runner> = runnerRepository.findById(runnerId)

        if (!runner.isPresent) {
            return TrainingRunResults(emptyList())
        }

        val trainingRuns = trainingRunRepository.findByDateBetween(startDate, endDate).sortedBy { it.date }

        val runnersTrainingRuns = trainingRuns.map {
            TrainingRunResult(it, runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runner.get().id)
                    .map { result -> RunnerTrainingRunDTO(runner.get(), result.uuid, result.trainingRunUuid, result.time, result.distance, result.avgPace) })
        }

        return TrainingRunResults(runnersTrainingRuns)

    }

    fun getAllRunnersTrainingRun(trainingRunUUID: String): RunnersTrainingRunResponse {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val results = runnersTrainingRunRepository.findByTrainingRunUuid(trainingRunUUID)

        return RunnersTrainingRunResponse(results.map {
            RunnerTrainingRunDTO(
                    runners[it.runnerId]!!, it.uuid, it.trainingRunUuid, it.time, it.distance, it.avgPace
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
                    createRunnersTrainingRunRequest.uuid
            )
            runnersTrainingRunRepository.save(insertMe)

            return RunnersTrainingRunResponse(listOf(RunnerTrainingRunDTO(
                    runner.get(), insertMe.uuid, insertMe.trainingRunUuid, insertMe.time, insertMe.distance, insertMe.avgPace
            )))

        } else {

            runnerRecord.distance = createRunnersTrainingRunRequest.distance
            runnerRecord.time = createRunnersTrainingRunRequest.time
            runnerRecord.avgPace = createRunnersTrainingRunRequest.avgPace

            runnersTrainingRunRepository.save(runnerRecord)

            return RunnersTrainingRunResponse(listOf(RunnerTrainingRunDTO(
                    runner.get(), runnerRecord.uuid, runnerRecord.trainingRunUuid, runnerRecord.time, runnerRecord.distance, runnerRecord.avgPace
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
                    createRunnersTrainingRunRequest.uuid
            )
            runnersTrainingRunRepository.save(insertMe)

            return RunnersTrainingRunResponse(listOf(RunnerTrainingRunDTO(
                    runner.get(), insertMe.uuid, insertMe.trainingRunUuid, insertMe.time, insertMe.distance, insertMe.avgPace
            )))

        } else {
            runnerRecord.avgPace = createRunnersTrainingRunRequest.avgPace
            runnerRecord.distance = createRunnersTrainingRunRequest.distance
            runnerRecord.time = createRunnersTrainingRunRequest.time

            runnersTrainingRunRepository.save(runnerRecord)

            return RunnersTrainingRunResponse(listOf(RunnerTrainingRunDTO(
                    runner.get(), runnerRecord.uuid, runnerRecord.trainingRunUuid, runnerRecord.time,
                    runnerRecord.distance, runnerRecord.avgPace
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
                            runner, it.uuid, it.trainingRunUuid, it.time, it.distance, it.avgPace
                    )
                })
            }

        }
    }

    fun getAllTrainingMilesRunByRunner(season: String): List<RankedRunnerDistanceRunDTO> {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val allTrainingRuns = trainingRunRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), Date(System.currentTimeMillis()))

        val runnersToDistance: MutableMap<Runner, Double> = mutableMapOf()

        allTrainingRuns.forEach {
             runnersTrainingRunRepository.findByTrainingRunUuid(it.uuid)
                    .forEach{ loggedRun ->
                        val entry = runnersToDistance[runners[loggedRun.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = loggedRun.distance
                        } else {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = entry + loggedRun.distance
                        }

                    }

        }

        val workouts = workoutRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuid(it.uuid)
                    .forEach { distance ->
                        val entry = runnersToDistance[runners[distance.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[distance.runnerId]!!] = distance.distance
                        } else {
                            runnersToDistance[runners[distance.runnerId]!!] = entry + distance.distance
                        }
                    }

        }

        return runnersToDistance.toList().sortedByDescending { it.second }
                .mapIndexed { index, it ->
                    RankedRunnerDistanceRunDTO(it.first, it.second, index + 1)
                }

    }

    fun getAllTrainingMilesRunForARunner(runnerId: Int, season: String): List<RankedRunnerDistanceRunDTO> {

        val runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val allTrainingRuns = trainingRunRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), Date(System.currentTimeMillis()))

        val runnersToDistance: MutableMap<Runner, Double> = mutableMapOf()

        allTrainingRuns.forEach {
            runnersTrainingRunRepository.findByTrainingRunUuidAndRunnerId(it.uuid, runnerId)
                    .forEach{ loggedRun ->
                        val entry = runnersToDistance[runners[loggedRun.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = loggedRun.distance
                        } else {
                            runnersToDistance[runners[loggedRun.runnerId]!!] = entry + loggedRun.distance
                        }

                    }
        }

        val workouts = workoutRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))

        workouts.forEach {
            workoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId)
                    .forEach { distance ->
                        val entry = runnersToDistance[runners[distance.runnerId]]
                        if (entry == null) {
                            runnersToDistance[runners[distance.runnerId]!!] = distance.distance
                        } else {
                            runnersToDistance[runners[distance.runnerId]!!] = entry + distance.distance
                        }
                    }

        }

        return runnersToDistance.toList().sortedByDescending { it.second }
                .mapIndexed { index, it ->
                    RankedRunnerDistanceRunDTO(it.first, it.second, index + 1)
                }

    }
}