package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.request.crosstraining.CreateCommentRequest
import com.terkula.uaxctf.training.request.crosstraining.CreateCrossTrainingRecordRequest
import com.terkula.uaxctf.training.request.crosstraining.CreateCrossTrainingRequest
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingResponse
import com.terkula.uaxctf.training.service.CrossTrainingService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.sql.Date
import javax.validation.Valid

@RestController
@Validated
class CrossTrainingController(
        val crossTrainingService: CrossTrainingService
) {

    @RequestMapping(value = ["cross-training-activity/get"], method = [RequestMethod.GET])
    fun getCrossTrainingActivities(

            @RequestParam(value = "startDate", required = true) startDate: Date,

            @RequestParam(value = "endDate", required = true) endDate: Date,

            ): CrossTrainingResponse {

        return crossTrainingService.getCrossTrainingActivities(startDate, endDate)
    }

    @RequestMapping(value = ["cross-training-activity/create"], method = [RequestMethod.POST])
    fun createCrossTrainingRun(
            @RequestBody @Valid createCrossTrainingRequest: CreateCrossTrainingRequest
    ): CrossTrainingResponse {
        return crossTrainingService.createCrossTraining(createCrossTrainingRequest)
    }

    @RequestMapping(value = ["cross-training-activity/update"], method = [RequestMethod.PUT])
    fun updateCrossTraining(
            @RequestBody @Valid createCrossTrainingRequest: CreateCrossTrainingRequest
    ): CrossTrainingResponse {
        return crossTrainingService.updateCrossTrainingActivity(createCrossTrainingRequest)
    }


    @RequestMapping(value = ["cross-training-activity/delete"], method = [RequestMethod.DELETE])
    fun deleteTrainingRun(
            @RequestParam(value = "uuid", required = true) uuid: String,

            ): CrossTrainingResponse {

        return crossTrainingService.deleteCrossTraining(uuid)
    }

    @RequestMapping(value = ["cross-training-record/create"], method = [RequestMethod.POST])
    fun createCrossTrainingRecord(
            @RequestBody @Valid createCrossTrainingRecordRequest: CreateCrossTrainingRecordRequest
    ): CrossTrainingRecordResponse {
        return crossTrainingService.createCrossTrainingRecord(createCrossTrainingRecordRequest)
    }

    @RequestMapping(value = ["cross-training-record/for-runner"], method = [RequestMethod.GET])
    fun getRunnersCrossTrainingRecord(
            @RequestParam(value = "crossTrainingUuid", required = true) crossTrainingUuid: String,
            @RequestParam(value = "runnerId", required = true) runnerId: Int,
    ): CrossTrainingRecordResponse? {
        return crossTrainingService.getRunnersCrossTrainingRecord(runnerId, crossTrainingUuid)
    }

    @RequestMapping(value = ["cross-training-record"], method = [RequestMethod.GET])
    fun getCrossTrainingRecord(
            @RequestParam(value = "uuid", required = true) uuid: String,
            ): CrossTrainingRecordResponse? {

        return crossTrainingService.getCrossTrainingRecord(uuid)
    }

    @RequestMapping(value = ["cross-training-record-comment/create"], method = [RequestMethod.POST])
    fun createCrossTrainingRecordComment(
            @RequestBody @Valid createCommentRequest: CreateCommentRequest
    ): TrainingComment {
        return crossTrainingService.createComment(createCommentRequest)
    }

    @RequestMapping(value = ["cross-training-records/all"], method = [RequestMethod.GET])
    fun getAllCrossTrainingRecordsForActivity(
            @RequestParam(value = "crossTrainingUuid", required = true) crossTrainingUuid: String,
    ): List<CrossTrainingRecordResponse> {

        return crossTrainingService.getAllCrossTrainingRecordsForActivity(crossTrainingUuid)
    }

    @RequestMapping(value = ["cross-training-activity/reattribute-points"], method = [RequestMethod.POST])
    fun reattributePoints(
            @RequestParam(value = "crossTrainingUuid", required = true) crossTrainingUuid: String,
            @RequestParam(value = "fromSeason", required = true) fromSeason: String,
            @RequestParam(value = "toSeason", required = true) toSeason: String,
    ): String {
        return crossTrainingService.forceReattributePoints(crossTrainingUuid, fromSeason, toSeason)
    }

}