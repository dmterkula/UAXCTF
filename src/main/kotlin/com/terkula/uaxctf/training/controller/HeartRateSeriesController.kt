package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.request.heartrate.SaveHeartRateSeriesRequest
import com.terkula.uaxctf.training.response.heartrate.HeartRateSeriesResponse
import com.terkula.uaxctf.training.service.HeartRateSeriesService
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@Validated
class HeartRateSeriesController(
    val heartRateSeriesService: HeartRateSeriesService
) {

    /** Upsert: re-uploading for the same activity replaces the stored series. */
    @RequestMapping(value = ["heart-rate-series/save"], method = [RequestMethod.POST])
    fun save(@RequestBody request: SaveHeartRateSeriesRequest): HeartRateSeriesResponse {
        return heartRateSeriesService.save(request)
    }

    /** 404 when no series exists, so clients can cleanly fall back to avg/max only. */
    @RequestMapping(value = ["heart-rate-series/get"], method = [RequestMethod.GET])
    fun get(
        @RequestParam(value = "activityType", required = true) activityType: String,
        @RequestParam(value = "activityUuid", required = true) activityUuid: String,
        @RequestParam(value = "runnerId", required = true) runnerId: Int,
    ): HeartRateSeriesResponse {
        return heartRateSeriesService.get(activityType, activityUuid, runnerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No heart rate series")
    }

    @RequestMapping(value = ["heart-rate-series/delete"], method = [RequestMethod.DELETE])
    fun delete(
        @RequestParam(value = "activityType", required = true) activityType: String,
        @RequestParam(value = "activityUuid", required = true) activityUuid: String,
        @RequestParam(value = "runnerId", required = true) runnerId: Int,
    ) {
        heartRateSeriesService.delete(activityType, activityUuid, runnerId)
    }
}
