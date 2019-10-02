package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.exception.UnsupportedAPIOperationException
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.PRResponse
import com.terkula.uaxctf.statistics.service.PersonalRecordService
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
class PRController(@field:Autowired
                           internal var personalRecordService: PersonalRecordService) {


    @ApiOperation("Returns PRs for all runners in the the given date constraints")
    @RequestMapping(value = ["xc/PRs/forAll"], method = [RequestMethod.GET])
    fun getAllPRsByYear(
            @ApiParam("Filters results for athletes whose grad class is after the input year. E.g if one " +
                    "wants all prs for everyone on the team, enter the current year. Cannot be used with filter.gradClass")
            @RequestParam(value = "filter.gradClassAfter", required = false, defaultValue = "") sinceGradClass: String,
            @ApiParam("filters results for athletes whose grad class matches the input year. Cannot be used with filter.gradClassAfter")
            @RequestParam(value = "filter.gradClass", required = false, defaultValue = "") filterClass: String,
            @ApiParam("Sorts the results based on 'latest' pr, fastest pr with 'time' and oldest pr with 'oldest'")
            @Pattern(
                    regexp = "latest|oldest|time",
                    message = "The value provided for sort.method. Valid values are 'latest', 'oldest' or 'time' ")
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String): PRResponse {

        if (sinceGradClass.isNotEmpty() && filterClass.isNotEmpty()) {
            throw UnsupportedAPIOperationException("Only use one of the filter parameters on this endpoint at a time")
        }

        var after = sinceGradClass

        if(sinceGradClass.isEmpty()) {
            after = MeetPerformanceController.CURRENT_YEAR
        }

        val prs = personalRecordService.getAllPRs(after, filterClass, getSortingMethod(sortMethod))

        return PRResponse(prs.size, prs)

    }

    @ApiOperation("Returns a singular Runner's PR")
    @RequestMapping(value = ["xc/PRs/forRunner"], method = [RequestMethod.GET])
    fun getRunnerPRsByName(
            @ApiParam("Filters results for athletes whose name matches or partially matches the input name")
            @RequestParam(value = "filter.name") partialName: String): PRResponse {

        val prs = personalRecordService.getPRsByName(partialName)

        return PRResponse(prs.size, prs)

    }

    @ApiOperation("Returns PRs for all runners in the last meet")
    @RequestMapping(value = ["xc/PRs/lastMeet"], method = [RequestMethod.GET])
    fun getSeasonBestsAtLastMeet(): PRResponse {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        val prs = personalRecordService.getPRsAtLastMeet(startDate, endDate)
        return PRResponse(prs.count(), prs)

    }

    private fun getSortingMethod(sortMethod: String) =
            when (sortMethod) {
                "oldest" -> SortingMethodContainer.OLDER_DATE
                "latest" -> SortingMethodContainer.RECENT_DATE
                "time" -> SortingMethodContainer.TIME
                else -> SortingMethodContainer.NOSORT
            }

}