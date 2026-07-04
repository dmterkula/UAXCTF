package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.training.dto.LiveWorkoutDTO
import com.terkula.uaxctf.training.request.liveworkout.CreateLiveWorkoutEntryRequest
import com.terkula.uaxctf.training.service.LiveWorkoutService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@Validated
class LiveWorkoutController(
        val liveWorkoutService: LiveWorkoutService
) {

    @RequestMapping(value = ["liveWorkout"], method = [RequestMethod.POST])
    fun createLiveWorkoutEntry(
            @RequestBody @Valid createLiveWorkoutEntry: CreateLiveWorkoutEntryRequest
    ): LiveWorkoutDTO {

        return liveWorkoutService.createLiveWorkoutEntry(createLiveWorkoutEntry)
    }

    @RequestMapping(value = ["liveWorkout/getLatest"], method = [RequestMethod.GET])
    fun getLiveWorkoutEntry(
            @RequestParam username: String
    ): LiveWorkoutDTO? {
        return liveWorkoutService.getLatestLiveWorkout(username)
    }

}