package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.request.crosstraining.CreateCommentRequest
import com.terkula.uaxctf.training.request.crosstraining.CreateCrossTrainingRecordRequest
import com.terkula.uaxctf.training.request.crosstraining.CreateCrossTrainingRequest
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingRecordResponse
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingResponse
import com.terkula.uaxctf.training.service.CrossTrainingService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.sql.Date
import javax.validation.Valid

@RestController
@Validated
class CrossTrainingController(
        val crossTrainingService: CrossTrainingService
) {

    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["cross-training-activity/get"], method = [RequestMethod.GET])
    fun getCrossTrainingActivities(

            @ApiParam("Earliest Date to look for")
            @RequestParam(value = "startDate", required = true) startDate: Date,

            @ApiParam("Latest date to look for")
            @RequestParam(value = "endDate", required = true) endDate: Date,

            ): CrossTrainingResponse {

        return crossTrainingService.getCrossTrainingActivities(startDate, endDate)
    }

    @ApiOperation("Returns a planned training run")
    @RequestMapping(value = ["cross-training-activity/create"], method = [RequestMethod.POST])
    fun createCrossTrainingRun(
            @RequestBody @Valid createCrossTrainingRequest: CreateCrossTrainingRequest
    ): CrossTrainingResponse {
        return crossTrainingService.createCrossTraining(createCrossTrainingRequest)
    }

    @ApiOperation("Updates or creates a training-run")
    @RequestMapping(value = ["cross-training-activity/update"], method = [RequestMethod.PUT])
    fun updateCrossTraining(
            @RequestBody @Valid createCrossTrainingRequest: CreateCrossTrainingRequest
    ): CrossTrainingResponse {
        return crossTrainingService.updateCrossTrainingActivity(createCrossTrainingRequest)
    }


    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["cross-training-activity/delete"], method = [RequestMethod.DELETE])
    fun deleteTrainingRun(
            @ApiParam("Training run uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,

            ): CrossTrainingResponse {

        return crossTrainingService.deleteCrossTraining(uuid)
    }

    @ApiOperation("Creates a cross training record")
    @RequestMapping(value = ["cross-training-record/create"], method = [RequestMethod.POST])
    fun createCrossTrainingRecord(
            @RequestBody @Valid createCrossTrainingRecordRequest: CreateCrossTrainingRecordRequest
    ): CrossTrainingRecordResponse {
        return crossTrainingService.createCrossTrainingRecord(createCrossTrainingRecordRequest)
    }

    @ApiOperation("Returns a runners cross training record")
    @RequestMapping(value = ["cross-training-record/for-runner"], method = [RequestMethod.GET])
    fun getRunnersCrossTrainingRecord(
            @ApiParam("crossTrainingUuid")
            @RequestParam(value = "crossTrainingUuid", required = true) crossTrainingUuid: String,
            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,
    ): CrossTrainingRecordResponse? {
        return crossTrainingService.getRunnersCrossTrainingRecord(runnerId, crossTrainingUuid)
    }

    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["cross-training-record"], method = [RequestMethod.GET])
    fun getCrossTrainingRecord(
            @ApiParam("uuid")
            @RequestParam(value = "uuid", required = true) uuid: String,
            ): CrossTrainingRecordResponse? {

        return crossTrainingService.getCrossTrainingRecord(uuid)
    }

    @ApiOperation("Creates a cross training record")
    @RequestMapping(value = ["cross-training-record-comment/create"], method = [RequestMethod.POST])
    fun createCrossTrainingRecordComment(
            @RequestBody @Valid createCommentRequest: CreateCommentRequest
    ): TrainingComment {
        return crossTrainingService.createComment(createCommentRequest)
    }

    @ApiOperation("Returns the planned training run between the given dates")
    @RequestMapping(value = ["cross-training-records/all"], method = [RequestMethod.GET])
    fun getAllCrossTrainingRecordsForActivity(
            @ApiParam("uuid")
            @RequestParam(value = "crossTrainingUuid", required = true) crossTrainingUuid: String,
    ): List<CrossTrainingRecordResponse> {

        return crossTrainingService.getAllCrossTrainingRecordsForActivity(crossTrainingUuid)
    }

}