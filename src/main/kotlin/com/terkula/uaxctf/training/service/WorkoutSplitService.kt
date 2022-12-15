package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.model.RunnerWorkoutDistance
import com.terkula.uaxctf.training.model.WorkoutSplitV2
import com.terkula.uaxctf.training.repository.RunnerWorkoutDistanceRepository
import com.terkula.uaxctf.training.repository.WorkoutComponentRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.repository.WorkoutSplitV2Repository
import com.terkula.uaxctf.training.request.CreateSplitsRequest
import com.terkula.uaxctf.training.request.LogWorkoutResultsRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.getYearString
import com.terkula.uaxctf.util.round
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class WorkoutSplitService(
        var workoutRepositoryV2: WorkoutRepository,
        var workoutComponentRepository: WorkoutComponentRepository,
        var workoutSplitV2Repository: WorkoutSplitV2Repository,
        var runnerService: RunnerService,
        var workoutDistanceRepository: RunnerWorkoutDistanceRepository
) {

    fun createSplits(createSplitsRequest: CreateSplitsRequest): SplitsResponse {

        val existingSplits = workoutSplitV2Repository.findByComponentUUIDAndRunnerId(createSplitsRequest.componentUUID, createSplitsRequest.runnerId)
        val splitNumberToSplit = existingSplits.map{ it.number to it }.toMap()

        val splits = createSplitsRequest.splits.map {

            if (splitNumberToSplit[it.number] != null) {
                val split = splitNumberToSplit[it.number]!!
                split.value = it.value
                return@map split
            } else {
                return@map WorkoutSplitV2(
                        createSplitsRequest.componentUUID,
                        createSplitsRequest.runnerId,
                        it.uuid,
                        it.number,
                        it.value
                )
            }
        }

        workoutSplitV2Repository.saveAll(splits)

        if (splits.isEmpty()) {
            throw RuntimeException("No splits created")
        }

        return SplitsResponse(splits.first().componentUUID, splits.first().runnerId, splits.map{ SplitResponse(it.uuid, it.number, it.value) })
    }

    fun delete(uuid: String): SplitsResponse {

        val deletedSplit = workoutSplitV2Repository.deleteByUuid(uuid).firstOrNull()
        if (deletedSplit == null) {
            throw RuntimeException("No split found")
        }

         var splits =  workoutSplitV2Repository.findByComponentUUIDAndRunnerId(deletedSplit.componentUUID, deletedSplit.runnerId).sortedBy { it.number }

         splits.forEachIndexed{ index, workoutSplitV2 ->
            workoutSplitV2.number = index + 1
        }

        workoutSplitV2Repository.saveAll(splits)

        return SplitsResponse(
                deletedSplit.componentUUID,
                deletedSplit.runnerId,
                listOf(SplitResponse(deletedSplit.uuid, deletedSplit.number, deletedSplit.value))
        )
    }

    fun getSplitsForRunnerAndComponent(runnerId: Int, componentUUID: String): SplitsResponse {

        val splits = workoutSplitV2Repository.findByComponentUUIDAndRunnerId(componentUUID, runnerId).sortedBy { it.number }

        return SplitsResponse(componentUUID, runnerId, splits.map { SplitResponse(it.uuid, it.number, it.value)})
    }

    fun getWorkoutSplitsForComponent(componentUUID: String): ComponentSplitsResponse {

        val component = workoutComponentRepository.findByUuid(componentUUID).first()
        val workout = workoutRepositoryV2.findByUuid(component.workoutUuid)


        val runners = runnerService.getRoster(true, workout.first().date.getYearString()).map {
            it.id to it
        }.toMap()

        val splits: List<RunnerWorkoutSplits> = workoutSplitV2Repository.findByComponentUUID(componentUUID).groupBy { it.runnerId }
                .map{ runners[it.key]!! to it.value.sortedBy { split -> split.number } }.toMap()
                .map {
                    RunnerWorkoutSplits(it.key, it.value.map { split -> SplitResponse(split.uuid, split.number, split.value) }, it.value.map{it.value.calculateSecondsFrom()}.average().round(1).toMinuteSecondString()) }

        return ComponentSplitsResponse(component, splits)

    }

    fun getRunnerWorkoutResults(workoutUuid: String, runnerId: Int): RunnerWorkoutResultResponse {

        val workout = workoutRepositoryV2.findByUuid(workoutUuid).first()

        val components = workoutComponentRepository.findByWorkoutUuid(workoutUuid)

        val runner = runnerService.getRoster(true, workout.date.getYearString()).first { it.id == runnerId }

        val workoutDistance = workoutDistanceRepository.findByWorkoutUuidAndRunnerId(workoutUuid, runnerId)

        var totalDistance = 0.0
        if (workoutDistance.isNotEmpty()) {
            totalDistance = workoutDistance.sumOf { it.distance }
        }

        val splitsResponses = components.map {
            getSplitsForRunnerAndComponent(runnerId, it.uuid)
        }

        return RunnerWorkoutResultResponse(runner, workout, splitsResponses, totalDistance)

    }

    fun logRunnersWorkout(logWorkoutResultsRequest: LogWorkoutResultsRequest): RunnerWorkoutResultResponse{


        val workout = workoutRepositoryV2.findByUuid(logWorkoutResultsRequest.workoutUuid).first()
        val runner = runnerService.getRoster(true, workout.date.getYearString()).first { it.id == logWorkoutResultsRequest.runnerId }

        val splitsResponse = logWorkoutResultsRequest.componentsSplits.map {
            createSplits(CreateSplitsRequest(it.componentUUID, logWorkoutResultsRequest.runnerId, it.splits))
        }

        val workoutDistance = workoutDistanceRepository.findByWorkoutUuidAndRunnerId(logWorkoutResultsRequest.workoutUuid, logWorkoutResultsRequest.runnerId)

        var distance = 0.0

        if (workoutDistance.isEmpty()) {
            val runnerWorkoutDistance = RunnerWorkoutDistance(logWorkoutResultsRequest.workoutUuid, logWorkoutResultsRequest.runnerId, logWorkoutResultsRequest.totalDistance)
            workoutDistanceRepository.save(runnerWorkoutDistance)
            distance = runnerWorkoutDistance.distance

        } else {
            val existingRecord = workoutDistance.first()

            existingRecord.distance = logWorkoutResultsRequest.totalDistance
            distance = existingRecord.distance
            workoutDistanceRepository.save(existingRecord)
        }

        return RunnerWorkoutResultResponse(runner, workout, splitsResponse, distance)

    }


}