package com.terkula.uaxctf.statistics.controller.track

import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO
import com.terkula.uaxctf.statistics.exception.UnsupportedAPIOperationException
import com.terkula.uaxctf.statistics.response.track.TrackSortingMethodContainer
import com.terkula.uaxctf.statistics.response.track.TrackPRResponse
import com.terkula.uaxctf.statistics.service.track.TrackPRService
import com.terkula.uaxctf.statistics.service.track.TrackSBService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.getYearString
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
class TrackSBController(var trackSBService: TrackSBService) {


//    @RequestMapping(value = ["track/PRs/forAll"], method = [RequestMethod.GET])
//    fun getAllPRsByYear(
//            @RequestParam(value = "filter.lastIncludedGradClass", required = false, defaultValue = "") lastIncludedGradClass: String,
//            @RequestParam(value = "filter.gradClass", required = false, defaultValue = "") filterClass: String,
//            @Pattern(
//                    regexp = "latest|oldest|time",
//                    message = "The value provided for sort.method. Valid values are 'latest', 'oldest' or 'time' ")
//            @RequestParam(value = "sort.method", required = false, defaultValue = "time") sortMethod: String): TrackPRResponse {
//
//        if (lastIncludedGradClass.isNotEmpty() && filterClass.isNotEmpty()) {
//            throw UnsupportedAPIOperationException("Only use one of the filter parameters on this endpoint at a time")
//        }
//
//        var after = lastIncludedGradClass
//
//        if (lastIncludedGradClass.isEmpty()) {
//            after = TimeUtilities.getFirstDayOfYear().getYearString()
//        }
//
////        val prs = personalRecordService.getAllPRs(after, filterClass, getSortingMethod(sortMethod))
//
//        // return TrackPRResponse(prs.size, prs)
//        return TrackPRResponse(1, emptyList())
//
//    }

//    @RequestMapping(value = ["track/PRs/forRunner"], method = [RequestMethod.GET])
//    fun getRunnerPRsByName(
//            @RequestParam(value = "filter.name") partialName: String): TrackPRResponse {
//
////        val prs = personalRecordService.getPRsByName(partialName)
////
////        return TrackPRResponse(prs.size, prs)
//        return TrackPRResponse(1, emptyList())
//
//    }

    @RequestMapping(value = ["track/SBs/forRunnerById"], method = [RequestMethod.GET])
    fun getRunnerPRsByName(
            @RequestParam(value = "filter.runnerId") runnerId: Int,
            @RequestParam(value = "filter.includeSplits") includeSplits: Boolean,
            @RequestParam(value = "filter.event", required = false) filterEvent: String? = "",
            @RequestParam(value = "filter.season", required = false) season: String): TrackPerformancesDTO {

        val eventFilter = filterEvent ?: ""

        return trackSBService.getARunnersSBs(runnerId, includeSplits, eventFilter, season, false)
    }

    @RequestMapping(value = ["track/SBs/forAll"], method = [RequestMethod.GET])
    fun getRunnerPRsByName(
            @RequestParam(value = "filter.includeSplits") includeSplits: Boolean,
            @RequestParam(value = "filter.event", required = false) filterEvent: String? = "",
            @RequestParam(value = "filter.season", required = false) season: String): List<TrackPerformancesDTO> {

        val eventFilter = filterEvent ?: ""

        return trackSBService.getAllSBs(includeSplits, eventFilter, season)
    }
//
//    @RequestMapping(value = ["track/SBs/lastMeet"], method = [RequestMethod.GET])
//    fun getPRsAtLastMeet(): TrackPRResponse {
//
//        val startDate = Date.valueOf("${TimeUtilities.getFirstDayOfYear().getYearString()}-01-01")
//        val endDate = Date.valueOf((TimeUtilities.getFirstDayOfYear().getYearString()) + "-12-31")
//
////        val prs = personalRecordService.getPRsAtLastMeet(startDate, endDate)
////        return TrackPRResponse(prs.count(), prs)
//        return TrackPRResponse(1, emptyList())
//
//    }

    private fun getSortingMethod(sortMethod: String) =
            when (sortMethod) {
//                "oldest" -> TrackSortingMethodContainer.OLDER_DATE
//                "latest" -> TrackSortingMethodContainer.RECENT_DATE
//                "time" -> TrackSortingMethodContainer.TIME
                else -> TrackSortingMethodContainer.NOSORT
            }

}