package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.exception.UnsupportedAPIOperationException
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.response.PRResponse
import com.terkula.uaxctf.statistics.service.PersonalRecordService
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


    @RequestMapping(value = ["/PRs/forAll"], method = [RequestMethod.GET])
    fun getAllPRsByYear(
            @RequestParam(value = "filter.sinceGradClass", required = false, defaultValue = "") sinceGradClass: String,
            @RequestParam(value = "filter.gradClass", required = false, defaultValue = "") filterClass: String,
            @Pattern(
                    regexp = "latest|furthest|time",
                    message = "The value provided for sort.method. Valid values are 'latest', 'furthest' or 'time')")
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String): PRResponse {

        if (sinceGradClass.isNotEmpty() && filterClass.isNotEmpty()) {
            throw UnsupportedAPIOperationException("only use one of the filter parameters on this endpoint at a time")
        }

        var after = sinceGradClass

        if(sinceGradClass.isEmpty()) {
            after = MeetPerformanceController.CURRENT_YEAR
        }

        val prs = personalRecordService.getAllPRs(after, filterClass, getSortingMethod(sortMethod))

        return PRResponse(prs.size, prs)

    }

    @RequestMapping(value = ["/PRs/forRunner"], method = [RequestMethod.GET])
    fun getRunnerPRsByName(
            @RequestParam(value = "filter.name") partialName: String): PRResponse {

        val prs = personalRecordService.getPRsByName(partialName)

        return PRResponse(prs.size, prs)

    }

    @RequestMapping(value = ["/PRs/lastMeet"], method = [RequestMethod.GET])
    fun getSeasonBestsAtLastMeet(): PRResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        val prs = personalRecordService.getPRsAtLastMeet(startDate, endDate)
        return PRResponse(prs.count(), prs)

    }

    private fun getSortingMethod(sortMethod: String) =
            when (sortMethod) {
                "furthest" -> SortingMethodContainer.OLDER_DATE
                "latest" -> SortingMethodContainer.RECENT_DATE
                "time" -> SortingMethodContainer.TIME
                else -> SortingMethodContainer.NOSORT
            }

}