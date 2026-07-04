package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statistics.dto.PRCountDTO
import com.terkula.uaxctf.statistics.dto.PRDTO
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


    @RequestMapping(value = ["xc/PRs/forAll"], method = [RequestMethod.GET])
    fun getAllPRsByYear(
            @RequestParam(value = "filter.gradClassAfter", required = false, defaultValue = "") sinceGradClass: String,
            @RequestParam(value = "filter.gradClass", required = false, defaultValue = "") filterClass: String,
            @Pattern(
                    regexp = "latest|oldest|time",
                    message = "The value provided for sort.method. Valid values are 'latest', 'oldest' or 'time' ")
            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String,
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): PRResponse {

        if (sinceGradClass.isNotEmpty() && filterClass.isNotEmpty()) {
            throw UnsupportedAPIOperationException("Only use one of the filter parameters on this endpoint at a time")
        }

        var after = sinceGradClass

        if(sinceGradClass.isEmpty()) {
            after = MeetPerformanceController.CURRENT_YEAR
        }

        val prs = personalRecordService.getAllPRs(after, filterClass, getSortingMethod(sortMethod), adjustForDistance)

        return PRResponse(prs.size, prs)

    }

    @RequestMapping(value = ["xc/PRs/forRunner"], method = [RequestMethod.GET])
    fun getRunnerPRsByName(
            @RequestParam(value = "filter.name") partialName: String,
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false): PRResponse {

        val prs = personalRecordService.getPRsByName(partialName, adjustForDistance)

        return PRResponse(prs.size, prs)

    }

    @RequestMapping(value = ["xc/PRs/lastMeet"], method = [RequestMethod.GET])
    fun getPRsAtLastMeet(
            @RequestParam(value = "adjust.forDistance", required = false, defaultValue = "false") adjustForDistance: Boolean = false
    ): PRResponse {

        val startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        val endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        val prs = personalRecordService.getPRsAtLastMeet(startDate, endDate, false)
        return PRResponse(prs.count(), prs)

    }

    @RequestMapping(value = ["xc/PRs/distribution"], method = [RequestMethod.GET])
    fun getPRDistributionByYear(
            @RequestParam(value = "startSeason", required = false, defaultValue = "2017") startSeason: String = "2017",
            @RequestParam(value = "lastSeason", required = false, defaultValue = "") lastSeason: String = ""
    ): Map<String, Map<Meet, PRCountDTO>> {

        val startDate = Date.valueOf("$startSeason-01-01")
        var endDate =
                if (lastSeason == "") {
                    Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")
                } else {
                    Date.valueOf("$lastSeason-12-31")
                }


        val prs = personalRecordService.getPRDistributionByYear(startDate, endDate)
        return prs

    }


    private fun getSortingMethod(sortMethod: String) =
            when (sortMethod) {
                "oldest" -> SortingMethodContainer.OLDER_DATE
                "latest" -> SortingMethodContainer.RECENT_DATE
                "time" -> SortingMethodContainer.TIME
                else -> SortingMethodContainer.NOSORT
            }

}