package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.dto.personalizedsplits.PersonalizedSplitDTO
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.MeetMileSplitService
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

@RestController
@Validated
class MeetSplitsController(@field:Autowired
                           internal var meetMileSplitService: MeetMileSplitService) {

    @ApiOperation("Returns the splits per mile for all runners in the given season(s)")
    @RequestMapping(value = ["/xc/meetMileSplits/forRunner"], method = [RequestMethod.GET])
    fun getAllMileSplitsBySeasonForRunner(
            @ApiParam("Filters results for runner matching the given name in the given season")
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

    @ApiOperation("All runners meet splits info at input meet")
    @RequestMapping(value = ["/xc/meetMileSplits/comparisonToPaceAtMeet"], method = [RequestMethod.GET])
    fun getMeetSplitComparison(
            @ApiParam("Filters results for the meet matching the given name in the given season")
            @RequestParam(value = "filter.meet", required = true) meetName: String,
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
            @ApiParam("Sorts the runners by their pace compared to PR for the input split number, 1,2, or 3")
            @RequestParam(value = "sort.split", required = false, defaultValue = "1") sort: String): List<MeetSplitsToComparisonPaceDTO> {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

         return meetMileSplitService.getMeetSplitsComparedToInputPace(meetName, startDate, endDate, "pr", sort)
    }


    @ApiOperation("Gives the input runner's meet splits info at meets within the date range")
    @RequestMapping(value = ["/xc/meetMileSplits/comparisonToPaceAtMeetForRunner"], method = [RequestMethod.GET])
    fun getMeetSplitComparisonForRunner(
            @ApiParam("Filters results for runner matching the given name in the given season")
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

    @ApiOperation("All runners meet splits info at meets within the date range")
    @RequestMapping(value = ["/xc/meetMileSplits/comparisonToPaceAtMeetsForAllRunners"], method = [RequestMethod.GET])
    fun getMeetSplitComparisonsForAllRunner(
            @ApiParam("Filters results for runner matching the given name in the given season")
            @RequestParam(value = "filter.season", required = false, defaultValue = "") season: String,
            @ApiParam("Sorts the runners by their pace compared to PR for the input split number, 1,2, or 3")
            @RequestParam(value = "sort.split", required = false, defaultValue = "1") sort: String): List<RunnerSplitComparisonAveragesDTO> {

        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")

        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        return meetMileSplitService.getMeetSplitComparisonsAveragesForAllRunners(startDate, endDate, "pr", sort)
    }

    @ApiOperation("Returns runners who on average slowed down the most or sped up between certain miles in races")
    @RequestMapping(value = ["/xc/avgMeetMileSplits/"], method = [RequestMethod.GET])
    fun getAverageSlowDownPerMile(
            @ApiParam("Filters results the given meet, averages for all meets there is data for if none provided")
            @RequestParam(value = "filter.meet", required = false, defaultValue = "") filterMeet: String = "",
            @ApiParam("Filters results for runner matching the given name in the given season")
            @RequestParam(value = "filter.season") season: String,
            @ApiParam("Filters results for splits in the given mile of the race. Defaults to spread" +
                    "Valid values are 'spread', 'firstToSecond', 'secondToThird' or 'combined' ")
            @Pattern(
                    regexp = "firstToSecond|secondToThird|combined|spread",
                    message = "The value provided for filter.split changes what split differences are calculated." +
                            " Valid values are 'spread', 'firstToSecond', 'secondToThird' or 'combined'")
            @RequestParam(value = "filter.split", required = false, defaultValue = "spread") split: String = "spread",
            @ApiParam("The value provided sort.method changes what order the results are returned in." +
                    "The value provided sort.method changes what order the results are returned in. Valid values are 'lowest', 'highest'")
            @Pattern(
                    regexp = "lowest|highest|",
                    message = "Valid values are 'lowest' to return" +
                            " runners with lower split differences,  or 'higher' to return those with the highest differences")
            @RequestParam(value = "sort.method", required = false, defaultValue = "lowest") sort: String = "lowest",
            @ApiParam("Limits the number of results to the desired counts")
            @RequestParam(value = "page.size", required = false, defaultValue = "50") limit: Int = 50): RunnerAvgSplitDifferenceResponse {

        val meetSplitsOption = getSplitOption(split)

        val startDate = Date.valueOf("$season-01-01")
        val endDate = Date.valueOf("$season-12-31")

       return RunnerAvgSplitDifferenceResponse ( meetMileSplitService.getMeetSplitInfo(filterMeet, meetSplitsOption, startDate, endDate, sort, limit))

    }

    @ApiOperation("Returns the meet in the given season which featured the largest/smallest average slowdown or speed up for runners in a given mile of the race")
    @RequestMapping(value = ["/xc/meetMileSplitsStatistics/"], method = [RequestMethod.GET])
    fun getMeetsByLargestSecondMileSlowdown(
            @ApiParam("Filters results for the given season")
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

    @ApiOperation("Returns statistical distribution and t test for each mile split to runners SB or PR pace" +
            " at a given meet in the two input years")
    @RequestMapping(value = ["/xc/meetSplit/YearComparisonTTest"], method = [RequestMethod.GET])
    fun getStatisticalComparisionBySplitNumberTTest(
            @ApiParam("First year of meet you want to compare")
            @RequestParam(value = "filter.baseSeason") baseSeason: String,
            @ApiParam("Second year of meet you want to compare")
            @RequestParam(value = "filter.comparisonSeason") comparisonSeason: String,
            @ApiParam("Filters results the given meet, averages for all meets there is data for if none provided")
            @RequestParam(value = "filter.meet", required = true) filterMeet: String = "",
            @ApiParam("Filters results the given meet, averages for all meets there is data for if none provided")
            @RequestParam(value = "comparisonPace", required = false, defaultValue = "PR") comparisonPace: String = "PR"): TTestResponse {

        val startDate1 = Date.valueOf("$baseSeason-01-01")
        val endDate1 = Date.valueOf("$baseSeason-12-31")

        val startDate2 = Date.valueOf("$comparisonSeason-01-01")
        val endDate2 = Date.valueOf("$comparisonSeason-12-31")

        return meetMileSplitService.runTwoSampleTTestForMileSplits(filterMeet, startDate1, endDate1, startDate2, endDate2, comparisonPace)
    }

    @ApiOperation("Returns statistical distribution and t test for each mile split to runners SB or PR pace" +
            " at all commin meets in the two input years")
    @RequestMapping(value = ["/xc/meetSplit/YearComparisonTTest/allMeets"], method = [RequestMethod.GET])
    fun getStatisticalComparisonsBySplitNumberTTestForAllMeets(
            @ApiParam("Base year")
            @RequestParam(value = "filter.baseSeason") baseSeason: String,
            @ApiParam("Comparison year")
            @RequestParam(value = "filter.comparisonSeason") comparisonSeason: String,
            @ApiParam("Filters results the given meet, averages for all meets there is data for if none provided")
            @RequestParam(value = "comparisonPace", required = false, defaultValue = "PR") comparisonPace: String = "PR"): List<TTestResponse> {

        return meetMileSplitService.runTwoSampleTTestForMileSplitsForSameMeetsInSeasons(baseSeason, comparisonSeason, comparisonPace)
    }

    @ApiOperation("Returns statistical distribution of each mile split to runners SB or PR pace")
    @RequestMapping(value = ["/xc/meetSplit/statDistribution"], method = [RequestMethod.GET])
    fun getStatisticalComparisionBySplitNumberTo(
            @ApiParam("First year of meet you want to compare")
            @RequestParam(value = "filter.season") season: String,
            @ApiParam("Filters results the given meet, averages for all meets there is data for if none provided")
            @RequestParam(value = "filter.meet", required = true) filterMeet: String = "",
            @ApiParam("Filters results the given meet, averages for all meets there is data for if none provided")
            @RequestParam(value = "comparisonPace", required = false, defaultValue = "PR") comparisonPace: String = "PR"): List<StatisticalComparisonDTO> {

        val startDate = Date.valueOf("$season-01-01")
        val endDate = Date.valueOf("$season-12-31")

        return meetMileSplitService.compareMileSplitTimesToComparisonPaceAtMeet(filterMeet, startDate, endDate, comparisonPace)
    }

    @ApiOperation("Returns a ranked statistical distribution of each mile split to runners SB or PR pace across meets in the same year")
    @RequestMapping(value = ["/xc/meetSplit/rankedStatDistribution"], method = [RequestMethod.GET])
    fun getStatisticalComparisonBySplitNumberTo(
            @ApiParam("First year of meet you want to compare")
            @RequestParam(value = "filter.season") season: String,
            @ApiParam("Filters results the given meet, averages for all meets there is data for if none provided")
            @RequestParam(value = "comparisonPace", required = false, defaultValue = "PR") comparisonPace: String = "PR"): List<List<MeetSplitStatisticalComparisonDTO>> {

        val startDate = Date.valueOf("$season-01-01")
        val endDate = Date.valueOf("$season-12-31")

        return meetMileSplitService.buildRankedMileSplitComparisonsAcrossMeets(startDate, endDate, comparisonPace)
    }

    @ApiOperation("Returns personalized mile splits for a runner to run a given time based given how they run their meets on average")
    @RequestMapping(value = ["/xc/meetSplit/personalized"], method = [RequestMethod.GET])
    fun getPersonalizedMileSplitsForInputTime(
            @ApiParam("Runner Id")
            @RequestParam(value = "runnerId") runnerId: Int,
            @ApiParam("Input time")
            @RequestParam(value = "time") time: String,
            @ApiParam("last n races")
            @RequestParam(value = "lastNRaces", required = false) lastNRaces: Int?,
            @RequestParam(value = "meetName", required = false) meetName: String?
    ): PersonalizedSplitDTO {
        return meetMileSplitService.getPersonalizedSplitsToHitCertainTime(runnerId, time, lastNRaces, meetName)
    }

    fun getSplitOption(option: String): MeetSplitsOption {
        return MeetSplitsOption.fromString(option)!!
    }

}