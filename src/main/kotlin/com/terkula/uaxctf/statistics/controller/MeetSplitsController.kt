package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.dto.personalizedsplits.PersonalizedSplitDTO
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.MeetMileSplitService
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
class MeetSplitsController(@field:Autowired
                           internal var meetMileSplitService: MeetMileSplitService) {

    @RequestMapping(value = ["/xc/meetMileSplits/forRunner"], method = [RequestMethod.GET])
    fun getAllMileSplitsBySeasonForRunner(
            @RequestParam(value = "filter.runner", required = true) name: String,
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String): RunnerMeetSplitResponse {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        val mileSplitResponse = meetMileSplitService.getAllMeetMileSplitsForRunner(name, startDate, endDate)

        return mileSplitResponse

    }

    @RequestMapping(value = ["/xc/meetMileSplits/comparisonToPaceAtMeet"], method = [RequestMethod.GET])
    fun getMeetSplitComparison(
            @RequestParam(value = "filter.meet", required = true) meetName: String,
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
            @RequestParam(value = "sort.split", required = false, defaultValue = "1") sort: String): List<MeetSplitsToComparisonPaceDTO> {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

         return meetMileSplitService.getMeetSplitsComparedToInputPace(meetName, startDate, endDate, "pr", sort)
    }


    @RequestMapping(value = ["/xc/meetMileSplits/comparisonToPaceAtMeetForRunner"], method = [RequestMethod.GET])
    fun getMeetSplitComparisonForRunner(
            @RequestParam(value = "filter.runner", required = true) runnerName: String,
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String): RunnerMeetSplitsComparisonWithAveragesDTO {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        return meetMileSplitService.getMeetSplitComparisonsForRunner(runnerName, startDate, endDate, "pr")
    }

    @RequestMapping(value = ["/xc/meetMileSplits/comparisonToPaceAtMeetsForAllRunners"], method = [RequestMethod.GET])
    fun getMeetSplitComparisonsForAllRunner(
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
            @RequestParam(value = "sort.split", required = false, defaultValue = "1") sort: String): List<RunnerSplitComparisonAveragesDTO> {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        return meetMileSplitService.getMeetSplitComparisonsAveragesForAllRunners(startDate, endDate, "pr", sort)
    }

    @RequestMapping(value = ["/xc/avgMeetMileSplits/"], method = [RequestMethod.GET])
    fun getAverageSlowDownPerMile(
            @RequestParam(value = "filter.meet", required = false, defaultValue = "") filterMeet: String = "",
            @RequestParam(value = "filter.season") season: String,
            @Pattern(
                    regexp = "firstToSecond|secondToThird|combined|spread",
                    message = "The value provided for filter.split changes what split differences are calculated." +
                            " Valid values are 'spread', 'firstToSecond', 'secondToThird' or 'combined'")
            @RequestParam(value = "filter.split", required = false, defaultValue = "spread") split: String = "spread",
            @Pattern(
                    regexp = "lowest|highest|",
                    message = "Valid values are 'lowest' to return" +
                            " runners with lower split differences,  or 'higher' to return those with the highest differences")
            @RequestParam(value = "sort.method", required = false, defaultValue = "lowest") sort: String = "lowest",
            @RequestParam(value = "page.size", required = false, defaultValue = "50") limit: Int = 50): RunnerAvgSplitDifferenceResponse {

        val meetSplitsOption = getSplitOption(split)

        val startDate = Date.valueOf("$season-01-01")
        val endDate = Date.valueOf("$season-12-31")

       return RunnerAvgSplitDifferenceResponse ( meetMileSplitService.getMeetSplitInfo(filterMeet, meetSplitsOption, startDate, endDate, sort, limit))

    }

    @RequestMapping(value = ["/xc/meetMileSplitsStatistics/"], method = [RequestMethod.GET])
    fun getMeetsByLargestSecondMileSlowdown(
            @RequestParam(value = "filter.season") season: String,
            @Pattern(
                    regexp = "firstToSecond|secondToThird|combined|spread",
                    message = "The value provided for filter.split changes what split differences are calculated. " +
                            "Valid values are 'spread', 'firstToSecond', 'secondToThird', or 'combined'")
            @RequestParam(value = "filter.split", required = false, defaultValue = "spread") split: String = "spread",
            @Pattern(
                    regexp = "lowest|highest|",
                    message = "Valid values are 'lowest' to return" +
                            " runners with lower split differences,  or 'higher' to return those with the highest differences")
            @RequestParam(value = "sort.method", required = false, defaultValue = "lowest") sort: String = "lowest"): MeetSplitStatisticResponse {

        val meetSplitsOption = getSplitOption(split)

        val startDate = Date.valueOf("$season-01-01")
        val endDate = Date.valueOf("$season-12-31")

        return MeetSplitStatisticResponse(meetMileSplitService.findMeetWithSignificantSplitStat(startDate, endDate, meetSplitsOption, sort))
    }

    @RequestMapping(value = ["/xc/meetSplit/YearComparisonTTest"], method = [RequestMethod.GET])
    fun getStatisticalComparisionBySplitNumberTTest(
            @RequestParam(value = "filter.baseSeason") baseSeason: String,
            @RequestParam(value = "filter.comparisonSeason") comparisonSeason: String,
            @RequestParam(value = "filter.meet", required = true) filterMeet: String = "",
            @RequestParam(value = "comparisonPace", required = false, defaultValue = "PR") comparisonPace: String = "PR"): TTestResponse {

        val startDate1 = Date.valueOf("$baseSeason-01-01")
        val endDate1 = Date.valueOf("$baseSeason-12-31")

        val startDate2 = Date.valueOf("$comparisonSeason-01-01")
        val endDate2 = Date.valueOf("$comparisonSeason-12-31")

        return meetMileSplitService.runTwoSampleTTestForMileSplits(filterMeet, startDate1, endDate1, startDate2, endDate2, comparisonPace)
    }

    @RequestMapping(value = ["/xc/meetSplit/YearComparisonTTest/allMeets"], method = [RequestMethod.GET])
    fun getStatisticalComparisonsBySplitNumberTTestForAllMeets(
            @RequestParam(value = "filter.baseSeason") baseSeason: String,
            @RequestParam(value = "filter.comparisonSeason") comparisonSeason: String,
            @RequestParam(value = "comparisonPace", required = false, defaultValue = "PR") comparisonPace: String = "PR"): List<TTestResponse> {

        return meetMileSplitService.runTwoSampleTTestForMileSplitsForSameMeetsInSeasons(baseSeason, comparisonSeason, comparisonPace)
    }

    @RequestMapping(value = ["/xc/meetSplit/statDistribution"], method = [RequestMethod.GET])
    fun getStatisticalComparisionBySplitNumberTo(
            @RequestParam(value = "filter.season") season: String,
            @RequestParam(value = "filter.meet", required = true) filterMeet: String = "",
            @RequestParam(value = "comparisonPace", required = false, defaultValue = "PR") comparisonPace: String = "PR"): List<StatisticalComparisonDTO> {

        val startDate = Date.valueOf("$season-01-01")
        val endDate = Date.valueOf("$season-12-31")

        return meetMileSplitService.compareMileSplitTimesToComparisonPaceAtMeet(filterMeet, startDate, endDate, comparisonPace)
    }

    @RequestMapping(value = ["/xc/meetSplit/rankedStatDistribution"], method = [RequestMethod.GET])
    fun getStatisticalComparisonBySplitNumberTo(
            @RequestParam(value = "filter.season") season: String,
            @RequestParam(value = "comparisonPace", required = false, defaultValue = "PR") comparisonPace: String = "PR"): List<List<MeetSplitStatisticalComparisonDTO>> {

        val startDate = Date.valueOf("$season-01-01")
        val endDate = Date.valueOf("$season-12-31")

        return meetMileSplitService.buildRankedMileSplitComparisonsAcrossMeets(startDate, endDate, comparisonPace)
    }

    @RequestMapping(value = ["/xc/meetSplit/personalized"], method = [RequestMethod.GET])
    fun getPersonalizedMileSplitsForInputTime(
            @RequestParam(value = "runnerId") runnerId: Int,
            @RequestParam(value = "time") time: String,
            @RequestParam(value = "lastNRaces", required = false) lastNRaces: Int?,
            @RequestParam(value = "meetName", required = false) meetName: String?
    ): PersonalizedSplitDTO {
        return meetMileSplitService.getPersonalizedSplitsToHitCertainTime(runnerId, time, lastNRaces, meetName)
    }

    fun getSplitOption(option: String): MeetSplitsOption {
        return MeetSplitsOption.fromString(option)!!
    }

}