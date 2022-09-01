package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.AggregateStatsResponse
import com.terkula.uaxctf.statistics.service.AggregateStatsService
import io.swagger.annotations.ApiOperation

import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController


@RestController
@Validated
class AggregateStatsController(
        var aggregateStatsService: AggregateStatsService
) {

    @ApiOperation("Returns aggregate information about the team, total number of runners tracked, total pr count, etc")
    @RequestMapping(value = ["xc/aggregateStats"], method = [RequestMethod.GET])
    fun getEntityCounts(): AggregateStatsResponse {

       return aggregateStatsService.getAggregateStats()

    }
}