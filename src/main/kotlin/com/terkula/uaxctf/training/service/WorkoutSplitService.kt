package com.terkula.uaxctf.training.service

import averageBy
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.dto.WorkoutComponentPlanElement
import com.terkula.uaxctf.training.model.WorkoutSplitV2
import com.terkula.uaxctf.training.repository.WorkoutComponentRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.repository.WorkoutSplitV2Repository
import com.terkula.uaxctf.training.request.CreateSplitsRequest
import com.terkula.uaxctf.training.response.ComponentSplitsResponse
import com.terkula.uaxctf.training.response.RunnerWorkoutSplits
import com.terkula.uaxctf.training.response.SplitResponse
import com.terkula.uaxctf.training.response.SplitsResponse
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.getYearString
import com.terkula.uaxctf.util.round
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import kotlin.math.roundToInt

@Service
class WorkoutSplitService(
        var workoutRepositoryV2: WorkoutRepository,
        var workoutComponentRepository: WorkoutComponentRepository,
        var workoutSplitV2Repository: WorkoutSplitV2Repository,
        var runnerService: RunnerService
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

        val deletedSplits = workoutSplitV2Repository.deleteByUuid(uuid)

        if (deletedSplits.isEmpty()) {
            throw RuntimeException("No split found")
        }

        return SplitsResponse(
                deletedSplits.first().componentUUID,
                deletedSplits.first().runnerId,
                deletedSplits.map { SplitResponse(it.uuid, it.number, it.value) }
        )
    }

    fun getSplitsForRunnerAndComponent(runnerId: Int, componentUUID: String): SplitsResponse {

        val splits = workoutSplitV2Repository.findByComponentUUIDAndRunnerId(componentUUID, runnerId)

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


}