package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.ConsistencyResponse
import com.terkula.uaxctf.statistics.service.ConsistencyRankService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import javax.validation.constraints.Pattern

@RestController
@Validated
class ConsistencyRankController (@field:Autowired
                                 internal val consistencyRankService: ConsistencyRankService) {

    @RequestMapping(value = ["xc/consistency/"], method = [RequestMethod.GET])
    fun getConsistencyRanks(
            @RequestParam("filter.season", defaultValue = "") filterSeason: String,
            @Pattern(
                    regexp = "races|workouts|all|",
                    message = "The value provided for measureBy is invalid. Valid values are 'races', 'workouts' or 'all'")
            @RequestParam("measureBy", defaultValue = "races", required = false) measureBy: String,
            @RequestParam("weight", defaultValue = ".", required = false) weight: Double = 1.0): ConsistencyResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (filterSeason.isNotEmpty()) {
            startDate = Date.valueOf("$filterSeason-01-01")
            endDate = Date.valueOf("$filterSeason-12-31")
        }

        return ConsistencyResponse(if (measureBy == "races") {
            consistencyRankService.getRunnersOrderedByMostConsistentRaces(startDate, endDate)
        } else if( measureBy ==  "workouts"){
            consistencyRankService.getRunnersOrderedByMostConsistentWorkouts(startDate, endDate)
        } else {
            consistencyRankService.getRunnersOrderedMostConsistent(startDate, endDate, weight)
        })


    }

}