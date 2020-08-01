package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.google.GoogleSheetsClient
import com.terkula.uaxctf.statistics.response.MeetResultDataLoadResponse
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import com.terkula.uaxctf.statistics.service.XcDataLoaderService
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.sql.Date

@RestController
class XcDataLoaderController(
    @field:Autowired
    internal var meetPerformanceService: MeetPerformanceService,
    @field:Autowired
    internal var xcDataLoaderService: XcDataLoaderService,
    @field:Autowired
    internal var googleSheetsClient: GoogleSheetsClient
) {

    @ApiOperation("Loads meet data that has manually been entered in the load table.")
    @RequestMapping(value = ["xc/load/{meetId}"], method = [RequestMethod.GET])
    fun loadPerformances(@PathVariable("meetId") meetId: Int): String {

        meetPerformanceService.loadMeetPerformance(meetId)
        return "loaded"
    }

    @ApiOperation("Loads milesplit data for meet that has been entered in the mile split table.")
    @RequestMapping(value = ["xc/load/mileSplit/{meetId}"], method = [RequestMethod.GET])
    fun loadMileSplits(@PathVariable("meetId") meetId: Int): String {

        meetPerformanceService.loadMileSplits(meetId)
        return "loaded mile splits"
    }

    @ApiOperation("Clean milesplit time formats")
    @RequestMapping(value = ["xc/load/cleanMileSplitData"], method = [RequestMethod.GET])
    fun cleanMileSplits(@RequestParam(value = "meetId", defaultValue = "0") meetId: Int = 0): String {

        meetPerformanceService.cleanSplits(meetId)
        return "cleanedMileSplits"
    }

    @ApiOperation("Load Time Trial Data")
    @RequestMapping(value = ["xc/load/TimeTrialResult"], method = [RequestMethod.GET])
    fun loadWorkoutData(@RequestParam(value = "season") season: String): String {

        meetPerformanceService.loadTimeTrial(season)
        return "loaded time trial"
    }

    @ApiOperation("Read Race Results From Google Sheet")
    @RequestMapping(value = ["xc/readRaceResults/"], method = [RequestMethod.GET])
    fun readSheet(
            @RequestParam(value = "sheetName") sheetName: String
    ): MeetResultDataLoadResponse {

        // List<List<name, time, place>
        val rawSheetData = googleSheetsClient.readSheet("1MNSyRdzS8O7KDpjitOKVoDnJoCr_npe8DrtfhJ2Sm-w", sheetName)
        return xcDataLoaderService.processRaceResults(rawSheetData, sheetName)
    }

    @ApiOperation("Create Meet Info")
    @RequestMapping(value = ["xc/createMeetInfoEntry/"], method = [RequestMethod.POST])
    fun createMeetInfoEntry(
            @RequestParam(value = "meetName") meetName: String,
            @RequestParam(value = "season", required = false, defaultValue = "") season: String = "",
            @RequestParam(value = "distance") distance: Int,
            @RequestParam(value = "elevationChange") elevationChange: Int,
            @RequestParam(value = "humidity") humidity: Double,
            @RequestParam(value = "isRainy", required = false, defaultValue = "false") isRainy: Boolean = false,
            @RequestParam(value = "isSnowy", required = false, defaultValue = "false") isSnowy: Boolean = false,
            @RequestParam(value = "temperature") temperature: Int,
            @RequestParam(value = "windSpeed") windSpeed: Int,
            @RequestParam(value = "cloudCoverRatio") cloudCoverRatio: Double
    ) {
        var startDate = Date.valueOf("${MeetPerformanceController.CURRENT_YEAR}-01-01")
        var endDate = Date.valueOf((MeetPerformanceController.CURRENT_YEAR) + "-12-31")


        if (season.isNotEmpty()) {
            startDate = Date.valueOf("$season-01-01")
            endDate = Date.valueOf("$season-12-31")
        }

        meetPerformanceService.postMeetInfoEntry(meetName, startDate, endDate, distance, elevationChange,
                humidity, isRainy, isSnowy, temperature, windSpeed, cloudCoverRatio)

    }

}
