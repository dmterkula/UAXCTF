package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.RankedPRDTO
import com.terkula.uaxctf.statistics.dto.RankedRunnerConsistencyDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedSeasonBestDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedAchievementDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedMeetResultDTO
import com.terkula.uaxctf.statistics.dto.leaderboard.RankedTryoutDTO
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

    @ApiOperation("Get Tryout leaderboard")
    @RequestMapping(value = ["xc/leaderboard/tryouts"], method = [RequestMethod.GET])
    fun getTryoutLeaderBoard(
            @RequestParam(name = "page.size") pageSize: Int,
            @RequestParam(name = "adjustTo5k", required = false) adjustTo5k: Boolean = false
    ): List<RankedTryoutDTO> {

        return leaderBoardService.getTryoutLeaderBoard(pageSize, adjustTo5k)
    }

    @ApiOperation("Get SB leaderboard")
    @RequestMapping(value = ["xc/leaderboard/sbs"], method = [RequestMethod.GET])
    fun getSBLeaderBoard(
        @RequestParam(name = "season") season: String
    ): List<RankedSeasonBestDTO> {
        return leaderBoardService.getSeasonBestLeaderBoard(season)
    }

    @ApiOperation("Get Meet Time leaderboard")
    @RequestMapping(value = ["xc/leaderboard/meetTimes"], method = [RequestMethod.GET])
    fun getMeetTimeLeaderBoard(
            @RequestParam(name = "meetName") meetName: String,
            @RequestParam(name = "count") count: Int
    ): List<RankedMeetResultDTO> {
        return leaderBoardService.getMeetTimeLeaderBoard(meetName, count)
    }

    @ApiOperation("Get Consistency rank leaderboard")
    @RequestMapping(value = ["xc/leaderboard/race-consistency"], method = [RequestMethod.GET])
    fun getConsistentRankLeaderBoard(
            @RequestParam(name = "season") season: String
    ): List<RankedRunnerConsistencyDTO> {
        return leaderBoardService.getRaceConsistentRankLeaderBoard(season)
    }

    @ApiOperation("Get Consistency rank leaderboard")
    @RequestMapping(value = ["xc/leaderboard/consistent-race-splits-achievement"], method = [RequestMethod.GET])
    fun getRaceSplitConsistencyAchievementRankLeaderBoard(
            @RequestParam(name = "season", required = false) season: String?,
            @RequestParam(name = "page.size", required = false) pageSize: Int?
    ): List<RankedAchievementDTO> {
        return if (season == null) {
            if (pageSize == null) {
                leaderBoardService.getRaceConsistentRankAchievementLeaderBoardCareer()
            } else {
                leaderBoardService.getRaceConsistentRankAchievementLeaderBoardCareer().take(pageSize)
            }

        } else {
            if (pageSize == null) {
                leaderBoardService.getRaceConsistentRankAchievementLeaderBoardSeason(season)
            } else {
                leaderBoardService.getRaceConsistentRankAchievementLeaderBoardSeason(season).take(pageSize)
            }
        }
    }

    @ApiOperation("Get Distance Run")
    @RequestMapping(value = ["xc/leaderboard/distance-run"], method = [RequestMethod.GET])
    fun getDistanceRunLeaderBoard(
            @RequestParam(name = "season", required = false) season: String?,
            @RequestParam(name = "page.size", required = false) pageSize: Int?
    ): List<RankedRunnerDistanceRunDTO> {

        return if (season != null) {
            if (pageSize == null) {
                leaderBoardService.getDistanceRunRankLeaderBoard(season!!)
            } else {
                leaderBoardService.getDistanceRunRankLeaderBoard(season!!).take(pageSize!!)
            }

        } else {
            if (pageSize == null) {
                leaderBoardService.getDistanceRunRankCareerLeaderBoard()
            } else {
                leaderBoardService.getDistanceRunRankCareerLeaderBoard().take(pageSize)
            }

        }
    }

    @ApiOperation("Get Logged Run Count Leaderboard")
    @RequestMapping(value = ["xc/leaderboard/logged-run-count"], method = [RequestMethod.GET])
    fun getLoggedRunCountLeaderBoard(
            @RequestParam(name = "season", required = false) season: String?,
            @RequestParam(name = "page.size", required = false) pageSize: Int?
    ): List<RankedAchievementDTO> {

        return if (season != null) {
            if (pageSize == null) {
                leaderBoardService.getRunLoggedCountRankLeaderBoardSeason(season!!)
            } else {
                leaderBoardService.getRunLoggedCountRankLeaderBoardSeason(season!!).take(pageSize)
            }

        } else {

            if(pageSize == null) {
                leaderBoardService.getRunLoggedCountRankLeaderBoardCareer()
            } else {
                leaderBoardService.getRunLoggedCountRankLeaderBoardCareer().take(pageSize)
            }

        }
    }

    @ApiOperation("Get Total Skulls Leaderboard")
    @RequestMapping(value = ["xc/leaderboard/total-skulls"], method = [RequestMethod.GET])
    fun getTotalSkullLeaderBoard(
            @RequestParam(name = "season", required = false) season: String?,
            @RequestParam(name = "page.size", required = false) pageSize: Int?
    ): List<RankedAchievementDTO> {
        return if (season == null) {
            if (pageSize == null) {
                leaderBoardService.getSkullsTotalLeaderboard()
            } else {
                leaderBoardService.getSkullsTotalLeaderboard().take(pageSize)
            }

        } else {
            if (pageSize == null) {
                leaderBoardService.getSkullsTotalSeasonLeaderboard(season)
            } else {
                leaderBoardService.getSkullsTotalSeasonLeaderboard(season).take(pageSize)
            }

        }
    }

    @ApiOperation("Get Skull Streak Leaderboard")
    @RequestMapping(value = ["xc/leaderboard/skull-streak"], method = [RequestMethod.GET])
    fun getSkullStreakLeaderBoard(
            @RequestParam(name = "season", required = false) season: String?,
            @RequestParam(name = "active", required = false) active: Boolean?,
            @RequestParam(name = "page.size", required = false) pageSize: Int?
    ): List<RankedAchievementDTO> {
        return if (season == null) {
            if (pageSize == null) {
                leaderBoardService.getSkullsStreakCareerLeaderboard(active)
            } else {
                leaderBoardService.getSkullsStreakCareerLeaderboard(active).take(pageSize)
            }

        } else {
            if (pageSize == null) {
                leaderBoardService.getSkullsStreakSeasonLeaderboard(season, active)
            } else {
                leaderBoardService.getSkullsStreakSeasonLeaderboard(season, active).take(pageSize)
            }

        }
    }

    @ApiOperation("Get Skull Streak Leaderboard")
    @RequestMapping(value = ["xc/leaderboard/passes-last-mile"], method = [RequestMethod.GET])
    fun getPassesLastMileLeaderBoard(
            @RequestParam(name = "season", required = false) season: String?,
            @RequestParam(name = "page.size", required = false) pageSize: Int?
    ): List<RankedAchievementDTO> {
        return if (season == null) {
            if (pageSize == null) {
                leaderBoardService.getPassesLastMileLeaderboardCareer()
            } else {
                leaderBoardService.getPassesLastMileLeaderboardCareer().take(pageSize)
            }

        } else {
            if (pageSize == null) {
                leaderBoardService.getPassesLastMileLeaderboardSeason(season)
            } else {
                leaderBoardService.getPassesLastMileLeaderboardSeason(season).take(pageSize)
            }

        }
    }




}