package com.terkula.uaxctf.training.controller

import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.response.MeetSummaryResponse
import com.terkula.uaxctf.statistics.service.MeetSummaryService
import com.terkula.uaxctf.training.response.WorkoutCreationResponse
import com.terkula.uaxctf.training.service.WorkoutCreationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date

@RestController
class WorkoutCreationController(@field:Autowired
                               internal var workoutCreationService: WorkoutCreationService) {

    @RequestMapping(value = ["/workoutCreator"], method = [RequestMethod.GET])
    fun getLastMeetSummary(@RequestParam(value = "type", required = true) workoutType: String,
                           @RequestParam(value = "distance", required = true) distance: Int,
                           @RequestParam(value = "pace", required = true) pace: String
                           ): WorkoutCreationResponse? {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        return workoutCreationService.createWorkout(workoutType, distance, pace)


        //todo create enums for type and pace
         //WorkoutCreationResponse(workoutType, distance, pace)



    }

}