package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.model.WorkoutSplitV2
import com.terkula.uaxctf.training.repository.WorkoutComponentRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.repository.WorkoutSplitV2Repository
import com.terkula.uaxctf.training.request.CreateSplitsRequest
import com.terkula.uaxctf.training.response.SplitResponse
import com.terkula.uaxctf.training.response.SplitsResponse
import org.springframework.stereotype.Service
import java.lang.RuntimeException

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

    fun delete(uuid: String): List<WorkoutSplitV2> {

        return workoutSplitV2Repository.deleteByUuid(uuid)

    }

    fun getSplitsForRunnerAndComponent(runnerId: Int, componentUUID: String): SplitsResponse {

        val splits = workoutSplitV2Repository.findByComponentUUIDAndRunnerId(componentUUID, runnerId)

        return SplitsResponse(componentUUID, runnerId, splits.map { SplitResponse(it.uuid, it.number, it.value)})
    }


}