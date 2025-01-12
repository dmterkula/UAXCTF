package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.model.RunnerWorkoutDistance
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.model.WorkoutSplitV2
import com.terkula.uaxctf.training.repository.*
import com.terkula.uaxctf.training.request.CreateSplitsRequest
import com.terkula.uaxctf.training.request.LogWorkoutResultsRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.util.*
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.sql.Date
import java.time.Month

@Service
class WorkoutSplitService(
        var workoutRepositoryV2: WorkoutRepository,
        var workoutComponentRepository: WorkoutComponentRepository,
        var workoutSplitV2Repository: WorkoutSplitV2Repository,
        var runnerService: RunnerService,
        var workoutDistanceRepository: RunnerWorkoutDistanceRepository,
        var commentRepository: TrainingCommentRepository
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

        return SplitsResponse(createSplitsRequest.componentUUID, createSplitsRequest.runnerId, splits.map{ SplitResponse(it.uuid, it.number, it.value) })
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

        val runner = runnerService.runnerRepository.findById(runnerId).get()

        val workoutDistance = workoutDistanceRepository.findByWorkoutUuidAndRunnerId(workoutUuid, runnerId)

        var comments: List<TrainingComment> = emptyList()
        if (workoutDistance.firstOrNull() != null) {
            comments = commentRepository.findByTrainingEntityUuid(workoutDistance.first().workoutUuid + workoutDistance.first().runnerId)
                    .sortedBy { it.timestamp }
        }

        val notes: String? = workoutDistance.firstOrNull()?.notes
        val coachNotes: String? = workoutDistance.firstOrNull()?.coachNotes

        var totalDistance = 0.0
        if (workoutDistance.isNotEmpty()) {
            totalDistance = workoutDistance.sumOf { it.distance }
        }
        var workoutTime = "00:00"
        var workoutPace = "00:00"
        if (workoutDistance.isNotEmpty()) {
            workoutTime = workoutDistance.sumOf { it.getTimeSeconds() }.toMinuteSecondString()
            workoutPace = workoutDistance.first().pace
        }

        val splitsResponses = components.map {
            getSplitsForRunnerAndComponent(runnerId, it.uuid)
        }

        return RunnerWorkoutResultResponse(runner, WorkoutResponseDTO(workout.date, workout.description, workout.title, workout.icon, workout.uuid, components, workout.season, workout.team),
                splitsResponses, totalDistance,
                workoutTime, workoutPace, workoutDistance.firstOrNull()?.warmUpDistance,
                workoutDistance.firstOrNull()?.warmUpTime, workoutDistance.firstOrNull()?.warmUpPace,
                workoutDistance.firstOrNull()?.coolDownDistance, workoutDistance.firstOrNull()?.coolDownTime, workoutDistance.firstOrNull()?.coolDownPace,
                notes, coachNotes, comments)

    }

    fun getAllResultsForWorkout(workoutUuid: String): List<RunnerWorkoutResultResponse> {

        val workout = workoutRepositoryV2.findByUuid(workoutUuid).firstOrNull() ?: return emptyList()

        val components = workoutComponentRepository.findByWorkoutUuid(workoutUuid)

        val runners =

                if (workout.team.equals("NU", true)) {
                    runnerService.getRunnersByTeam(workout.team)
                } else {
                    if (workout.season == "track") {
                        var season = workout.date.getYearString()

                        if(workout.date.toLocalDate().month >= Month.NOVEMBER) {
                            season = (workout.date.toLocalDate().year + 1).toString()
                        }

                        runnerService.getTrackRoster(true, season)
                    } else {
                        runnerService.getTrackRoster(true, workout.date.getYearString())
                    }
                }
                .map {
            it.id to it
        }.toMap()

        return workoutDistanceRepository.findByWorkoutUuid(workoutUuid)
                .map {
                    runners[it.runnerId] to it
                }.filter {
                    it.first != null
                }.
                map {


            val notes: String? = it.second.notes
            val coachNotes: String? = it.second.coachNotes

            var totalDistance = 0.0
            if (it.second.distance != 0.0) {
                totalDistance = it.second.distance
            }
            var workoutTime = "00:00"
            var workoutPace = "00:00"
            if (it.second.distance != 0.0) {
                workoutTime = it.second.getTimeSeconds().toMinuteSecondString()
                workoutPace = it.second.pace
            }

            val splitsResponses = components.map { component ->
                getSplitsForRunnerAndComponent(it.first!!.id, component.uuid)
            }
            var comments: List<TrainingComment> = emptyList()
            comments = commentRepository.findByTrainingEntityUuid(it.second.workoutUuid + it.second.runnerId)
                    .sortedBy { it.timestamp }


            return@map RunnerWorkoutResultResponse(it.first!!, WorkoutResponseDTO(workout.date, workout.description, workout.title, workout.icon, workout.uuid, components, workout.season, workout.team),
                    splitsResponses, totalDistance,
                    workoutTime, workoutPace, it.second.warmUpDistance,
                    it.second.warmUpTime, it.second.warmUpPace,
                    it.second.coolDownDistance, it.second.coolDownTime, it.second.coolDownPace,
                    notes, coachNotes, comments)
        }

    }

    fun getAllARunnersWorkoutResults(runnerId: Int, season: String): List<RunnerWorkoutResultResponse> {

        val workouts = workoutRepositoryV2.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))
        val runner = runnerService.runnerRepository.findById(runnerId).get()

        val results = workouts.map {
            val components = workoutComponentRepository.findByWorkoutUuid(it.uuid)
            val workoutDistance = workoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId)
            var comments: List<TrainingComment> = emptyList()
            if (workoutDistance.firstOrNull() != null) {
                comments = commentRepository.findByTrainingEntityUuid(workoutDistance.first().workoutUuid + workoutDistance.first().runnerId)
                        .sortedBy { it.timestamp }
            }
            var totalDistance = 0.0
            if (workoutDistance.isNotEmpty()) {
                totalDistance = workoutDistance.sumOf {dist-> dist.distance }
            }

            var workoutTime = "00:00"
            var workoutPace = "00:00"
            if (workoutDistance.isNotEmpty()) {
                workoutTime = workoutDistance.sumOf { it.getTimeSeconds() }.toMinuteSecondString()
                workoutPace = workoutDistance.first().pace
            }

            val notes: String? = workoutDistance.firstOrNull()?.notes
            val coachNotes: String? = workoutDistance.firstOrNull()?.coachNotes

            val splitsResponses = components.map { comp->
                getSplitsForRunnerAndComponent(runnerId, comp.uuid)
            }
                    .filter { splits -> splits.splits.isNotEmpty() }

            return@map RunnerWorkoutResultResponse(runner, WorkoutResponseDTO(it.date, it.description, it.title, it.icon, it.uuid, components, it.season, it.team), splitsResponses, totalDistance,
                    workoutTime, workoutPace, workoutDistance.firstOrNull()?.warmUpDistance,
                    workoutDistance.firstOrNull()?.warmUpTime, workoutDistance.firstOrNull()?.warmUpPace,
                    workoutDistance.firstOrNull()?.coolDownDistance, workoutDistance.firstOrNull()?.coolDownTime, workoutDistance.firstOrNull()?.coolDownPace,
                    notes, coachNotes, comments)
        }

        return results.filter { it.componentResults.isNotEmpty()  }
    }

    fun getAllARunnersWorkoutResultsBySeason(runnerId: Int, season: String, startDate: Date, endDate: Date, type: String): List<RunnerWorkoutResultResponse> {

        val workouts = workoutRepositoryV2.findByDateBetweenAndSeason(startDate, endDate, type)
        val runner = runnerService.runnerRepository.findById(runnerId).get()

        val results = workouts.map {
            val components = workoutComponentRepository.findByWorkoutUuid(it.uuid)
            val workoutDistance = workoutDistanceRepository.findByWorkoutUuidAndRunnerId(it.uuid, runnerId)
            var comments: List<TrainingComment> = emptyList()
            if (workoutDistance.firstOrNull() != null) {
                comments = commentRepository.findByTrainingEntityUuid(workoutDistance.first().workoutUuid + workoutDistance.first().runnerId)
                        .sortedBy { it.timestamp }
            }
            var totalDistance = 0.0
            if (workoutDistance.isNotEmpty()) {
                totalDistance = workoutDistance.sumOf {dist-> dist.distance }
            }

            var workoutTime = "00:00"
            var workoutPace = "00:00"
            if (workoutDistance.isNotEmpty()) {
                workoutTime = workoutDistance.sumOf { it.getTimeSeconds() }.toMinuteSecondString()
                workoutPace = workoutDistance.first().pace
            }

            val notes: String? = workoutDistance.firstOrNull()?.notes
            val coachNotes: String? = workoutDistance.firstOrNull()?.coachNotes

            val splitsResponses = components.map { comp->
                getSplitsForRunnerAndComponent(runnerId, comp.uuid)
            }
                    .filter { splits -> splits.splits.isNotEmpty() }

            return@map RunnerWorkoutResultResponse(runner, WorkoutResponseDTO(it.date, it.description, it.title, it.icon, it.uuid, components, it.season, it.team), splitsResponses, totalDistance,
                    workoutTime, workoutPace, workoutDistance.firstOrNull()?.warmUpDistance,
                    workoutDistance.firstOrNull()?.warmUpTime, workoutDistance.firstOrNull()?.warmUpPace,
                    workoutDistance.firstOrNull()?.coolDownDistance, workoutDistance.firstOrNull()?.coolDownTime, workoutDistance.firstOrNull()?.coolDownPace,
                    notes, coachNotes, comments)
        }

        return results.filter { it.componentResults.isNotEmpty()  }
    }

    fun logRunnersWorkout(logWorkoutResultsRequest: LogWorkoutResultsRequest): RunnerWorkoutResultResponse{


        val workout = workoutRepositoryV2.findByUuid(logWorkoutResultsRequest.workoutUuid).first()
        val runner = runnerService.runnerRepository.findById(logWorkoutResultsRequest.runnerId).get()

        val components = workoutComponentRepository.findByWorkoutUuid(logWorkoutResultsRequest.workoutUuid)

        val splitsResponse = logWorkoutResultsRequest.componentsSplits.map {
            createSplits(CreateSplitsRequest(it.componentUUID, logWorkoutResultsRequest.runnerId, it.splits))
        }

        val workoutDistance = workoutDistanceRepository.findByWorkoutUuidAndRunnerId(logWorkoutResultsRequest.workoutUuid, logWorkoutResultsRequest.runnerId)

        var comments: List<TrainingComment> = emptyList()
        if (workoutDistance.firstOrNull() != null) {
            comments = commentRepository.findByTrainingEntityUuid(workoutDistance.first().workoutUuid + workoutDistance.first().runnerId)
                    .sortedBy { it.timestamp }
        }

        var distance = 0.0

        if (workoutDistance.isEmpty()) {
            val runnerWorkoutDistance = RunnerWorkoutDistance(logWorkoutResultsRequest.workoutUuid, logWorkoutResultsRequest.runnerId, logWorkoutResultsRequest.totalDistance,
                    logWorkoutResultsRequest.time, logWorkoutResultsRequest.pace, logWorkoutResultsRequest.warmUpDistance, logWorkoutResultsRequest.warmUpTime, logWorkoutResultsRequest.warmUpPace,
                    logWorkoutResultsRequest.coolDownDistance, logWorkoutResultsRequest.coolDownTime, logWorkoutResultsRequest.coolDownPace,
                    logWorkoutResultsRequest.notes, logWorkoutResultsRequest.coachNotes
            )
            workoutDistanceRepository.save(runnerWorkoutDistance)
            distance = runnerWorkoutDistance.distance
            runnerWorkoutDistance.notes = logWorkoutResultsRequest.notes
            runnerWorkoutDistance.coachNotes = logWorkoutResultsRequest.coachNotes

        } else {
            val existingRecord = workoutDistance.first()

            existingRecord.distance = logWorkoutResultsRequest.totalDistance
            existingRecord.notes = logWorkoutResultsRequest.notes
            existingRecord.time = logWorkoutResultsRequest.time
            existingRecord.pace = logWorkoutResultsRequest.pace
            existingRecord.warmUpDistance = logWorkoutResultsRequest.warmUpDistance
            existingRecord.warmUpTime = logWorkoutResultsRequest.warmUpTime
            existingRecord.warmUpPace = logWorkoutResultsRequest.warmUpPace
            existingRecord.coolDownDistance = logWorkoutResultsRequest.coolDownDistance
            existingRecord.coolDownTime = logWorkoutResultsRequest.coolDownTime
            existingRecord.coolDownPace = logWorkoutResultsRequest.coolDownPace
            existingRecord.coachNotes = logWorkoutResultsRequest.coachNotes

            distance = existingRecord.distance

            existingRecord.notes = logWorkoutResultsRequest.notes
            existingRecord.coachNotes = logWorkoutResultsRequest.coachNotes
            workoutDistanceRepository.save(existingRecord)
        }

        return RunnerWorkoutResultResponse(runner, WorkoutResponseDTO(workout.date, workout.description, workout.title, workout.icon, workout.uuid, components, workout.season, workout.team), splitsResponse, distance,
                logWorkoutResultsRequest.time, logWorkoutResultsRequest.pace, logWorkoutResultsRequest.warmUpDistance, logWorkoutResultsRequest.warmUpTime, logWorkoutResultsRequest.warmUpPace,
                logWorkoutResultsRequest.coolDownDistance, logWorkoutResultsRequest.coolDownTime, logWorkoutResultsRequest.coolDownPace,
                logWorkoutResultsRequest.notes, logWorkoutResultsRequest.coachNotes, comments)

    }


}