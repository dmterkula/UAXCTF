package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.dto.LiveWorkoutDTO
import com.terkula.uaxctf.training.request.liveworkout.CreateLiveWorkoutEntryRequest
import com.terkula.uaxctf.training.service.LiveWorkoutService
import io.swagger.annotations.ApiOperation
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@Validated
class LiveWorkoutController(
        val liveWorkoutService: LiveWorkoutService
) {

    @ApiOperation("Creates live workout entry")
    @RequestMapping(value = ["liveWorkout"], method = [RequestMethod.POST])
    fun createLiveWorkoutEntry(
            @RequestBody @Valid createLiveWorkoutEntry: CreateLiveWorkoutEntryRequest
    ): LiveWorkoutDTO {

        return liveWorkoutService.createLiveWorkoutEntry(createLiveWorkoutEntry)
    }

    @ApiOperation("Get latest live entry")
    @RequestMapping(value = ["liveWorkout/getLatest"], method = [RequestMethod.GET])
    fun getLiveWorkoutEntry(
            @RequestParam username: String
    ): LiveWorkoutDTO? {
        return liveWorkoutService.getLatestLiveWorkout(username)
    }

}