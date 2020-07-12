package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.google.GoogleSheetsClient
import com.terkula.uaxctf.statistics.response.MeetResultDataLoadResponse
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import com.terkula.uaxctf.statistics.service.XcDataLoaderService
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

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

}
