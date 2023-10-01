package com.terkula.uaxctf.statistics.controller.track

import com.terkula.uaxctf.statistics.exception.UnsupportedAPIOperationException
import com.terkula.uaxctf.statistics.response.track.TrackSortingMethodContainer
import com.terkula.uaxctf.statistics.response.track.TrackPRResponse
import com.terkula.uaxctf.statistics.service.track.TrackPRService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.getYearString
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import javax.validation.constraints.Pattern

@Validated
@RestController
class TrackPRController(@field:Autowired
                           internal var personalRecordService: TrackPRService) {


    @ApiOperation("Returns PRs for all runners in the the given date constraints")
    @RequestMapping(value = ["track/PRs/forAll"], method = [RequestMethod.GET])
    fun getAllPRsByYear(
            @ApiParam("Filters results for athletes whose grad class is after the input year. E.g if one " +
                    "wants all prs for everyone on the team, enter the current year. Cannot be used with filter.gradClass")
            @RequestParam(value = "filter.lastIncludedGradClass", required = false, defaultValue = "") lastIncludedGradClass: String,
            @ApiParam("filters results for athletes whose grad class matches the input year. Cannot be used with filter.gradClassAfter")
            @RequestParam(value = "filter.gradClass", required = false, defaultValue = "") filterClass: String,
            @ApiParam("Sorts the results based on 'latest' pr, fastest pr with 'time' and oldest pr with 'oldest'")
            @Pattern(
                    regexp = "latest|oldest|time",
                    message = "The value provided for sort.method. Valid values are 'latest', 'oldest' or 'time' ")
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String): TrackPRResponse {

        if (lastIncludedGradClass.isNotEmpty() && filterClass.isNotEmpty()) {
            throw UnsupportedAPIOperationException("Only use one of the filter parameters on this endpoint at a time")
        }

        var after = lastIncludedGradClass

        if (lastIncludedGradClass.isEmpty()) {
            after = TimeUtilities.getFirstDayOfYear().getYearString()
        }

//        val prs = personalRecordService.getAllPRs(after, filterClass, getSortingMethod(sortMethod))

       // return TrackPRResponse(prs.size, prs)
        return TrackPRResponse(1, emptyList())

    }

    @ApiOperation("Returns a singular Runner's PR")
    @RequestMapping(value = ["track/PRs/forRunner"], method = [RequestMethod.GET])
    fun getRunnerPRsByName(
            @ApiParam("Filters results for athletes whose name matches or partially matches the input name")
            @RequestParam(value = "filter.name") partialName: String): TrackPRResponse {

//        val prs = personalRecordService.getPRsByName(partialName)
//
//        return TrackPRResponse(prs.size, prs)
        return TrackPRResponse(1, emptyList())

    }

    @ApiOperation("Returns PRs for all runners in the last meet")
    @RequestMapping(value = ["track/PRs/lastMeet"], method = [RequestMethod.GET])
    fun getPRsAtLastMeet(): TrackPRResponse {

        val startDate = Date.valueOf("${TimeUtilities.getFirstDayOfYear().getYearString()}-01-01")
        val endDate = Date.valueOf((TimeUtilities.getFirstDayOfYear().getYearString()) + "-12-31")

//        val prs = personalRecordService.getPRsAtLastMeet(startDate, endDate)
//        return TrackPRResponse(prs.count(), prs)
        return TrackPRResponse(1, emptyList())

    }

    private fun getSortingMethod(sortMethod: String) =
            when (sortMethod) {
//                "oldest" -> TrackSortingMethodContainer.OLDER_DATE
//                "latest" -> TrackSortingMethodContainer.RECENT_DATE
//                "time" -> TrackSortingMethodContainer.TIME
                else -> TrackSortingMethodContainer.NOSORT
            }

}