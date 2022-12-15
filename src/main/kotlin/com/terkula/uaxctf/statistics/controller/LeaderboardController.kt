package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.RankedPRDTO
import com.terkula.uaxctf.statistics.dto.RankedRunnerConsistencyDTO
import com.terkula.uaxctf.statistics.dto.RankedSeasonBestDTO
import com.terkula.uaxctf.statistics.service.LeaderBoardService
import com.terkula.uaxctf.training.response.RankedRunnerDistanceRunDTO
import io.swagger.annotations.ApiOperation
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class LeaderboardController(val leaderBoardService: LeaderBoardService) {

    @ApiOperation("Get PR leaderboard")
    @RequestMapping(value = ["xc/leaderboard/prs"], method = [RequestMethod.GET])
    fun getPRLeaderBoard(
        @RequestParam(name = "page.size") pageSize: Int
    ): List<RankedPRDTO> {

        return leaderBoardService.getPRLeaderBoard(pageSize)
    }

    @ApiOperation("Get SB leaderboard")
    @RequestMapping(value = ["xc/leaderboard/sbs"], method = [RequestMethod.GET])
    fun getSBLeaderBoard(
        @RequestParam(name = "season") season: String
    ): List<RankedSeasonBestDTO> {
        return leaderBoardService.getSeasonBestLeaderBoard(season)
    }

    @ApiOperation("Get Consistency rank leaderboard")
    @RequestMapping(value = ["xc/leaderboard/race-consistency"], method = [RequestMethod.GET])
    fun getConsistentRankLeaderBoard(
            @RequestParam(name = "season") season: String
    ): List<RankedRunnerConsistencyDTO> {
        return leaderBoardService.getRaceConsistentRankLeaderBoard(season)
    }

    @ApiOperation("Get Consistency rank leaderboard")
    @RequestMapping(value = ["xc/leaderboard/distance-run"], method = [RequestMethod.GET])
    fun getDistanceRunLeaderBoard(
            @RequestParam(name = "season") season: String
    ): List<RankedRunnerDistanceRunDTO> {
        return leaderBoardService.getDistanceRunRankLeaderBoard(season)
    }


}