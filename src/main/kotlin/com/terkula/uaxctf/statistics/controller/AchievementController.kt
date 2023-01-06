package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.achievement.RunnerAchievementsDTO
import com.terkula.uaxctf.training.service.AchievementService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@Validated
class AchievementController(val achievementService: AchievementService) {

    @ApiOperation("returns achievements for runner")
    @RequestMapping(value = ["xc/achievements/runner"], method = [RequestMethod.GET])
    fun getAchievements(
            @ApiParam("runnerId")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,
            ): RunnerAchievementsDTO {

        return achievementService.getRunnersAchievements(runnerId)
    }
}
