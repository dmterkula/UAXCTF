package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.PointTransaction
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.request.*
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.PointsService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@RestController
@Validated
@RequestMapping("/api/v1/points")
class PointsController(
        val pointsService: PointsService
) {

    @RequestMapping(value = ["/balance"], method = [RequestMethod.GET])
    fun getPointBalance(
            @RequestParam(value = "runnerId", required = true) runnerId: Int
    ): ResponseEntity<PointBalanceResponse> {
        val balance = pointsService.getPointBalance(runnerId)
        return ResponseEntity.ok(balance)
    }

    @RequestMapping(value = ["/validate"], method = [RequestMethod.POST])
    fun validateSufficientPoints(
            @RequestBody request: ValidatePointsRequest
    ): ResponseEntity<ValidatePointsResponse> {
        val validation = pointsService.validatePoints(request)
        return ResponseEntity.ok(validation)
    }

    @RequestMapping(value = ["/spend"], method = [RequestMethod.POST])
    fun spendPoints(
            @RequestBody request: SpendPointsRequest
    ): ResponseEntity<SpendPointsResponse> {
        val result = pointsService.spendPoints(request)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @RequestMapping(value = ["/refund"], method = [RequestMethod.POST])
    fun refundPoints(
            @RequestBody request: RefundPointsRequest
    ): ResponseEntity<RefundPointsResponse> {
        val result = pointsService.refundPoints(request)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @RequestMapping(value = ["/transactions"], method = [RequestMethod.GET])
    fun getTransactionHistory(
            @RequestParam(value = "runnerId", required = true) runnerId: Int,
            @RequestParam(value = "limit", required = false, defaultValue = "50") limit: Int
    ): ResponseEntity<List<PointTransaction>> {
        val transactions = pointsService.getTransactionHistory(runnerId, limit)
        return ResponseEntity.ok(transactions)
    }

    @RequestMapping(value = ["/config"], method = [RequestMethod.GET])
    fun getPointsConfiguration(): ResponseEntity<PointsConfigResponse> {
        val config = pointsService.getConfiguration()
        return ResponseEntity.ok(PointsConfigResponse(config))
    }

    @RequestMapping(value = ["/config"], method = [RequestMethod.PUT])
    fun updatePointsConfiguration(
            @RequestBody request: UpdatePointsConfigRequest
    ): ResponseEntity<Void> {
        pointsService.updateConfiguration(request)
        return ResponseEntity.ok().build()
    }

    @RequestMapping(value = ["/leaderboard"], method = [RequestMethod.GET])
    fun getLeaderboard(
            @RequestParam(value = "limit", required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<Runner>> {
        val leaderboard = pointsService.getLeaderboard(limit)
        return ResponseEntity.ok(leaderboard)
    }

    @RequestMapping(value = ["/earn"], method = [RequestMethod.POST])
    fun earnPoints(
            @RequestBody request: EarnPointsRequest
    ): ResponseEntity<EarnPointsResponse> {
        val result = pointsService.earnPoints(request)
        return ResponseEntity.ok(result)
    }

    @RequestMapping(value = ["/summer-conditioning"], method = [RequestMethod.GET])
    fun getSummerConditioningPoints(
            @RequestParam(value = "year", required = true) year: String
    ): ResponseEntity<SummerConditioningResponse> {
        return try {
            ResponseEntity.ok(pointsService.getSummerConditioningPoints(year))
        } catch (e: RuntimeException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No summer conditioning config for year $year")
        }
    }

    @RequestMapping(value = ["/summer-conditioning/breakdown"], method = [RequestMethod.GET])
    fun getSummerConditioningBreakdown(
            @RequestParam(value = "runnerId", required = true) runnerId: Int,
            @RequestParam(value = "year", required = true) year: String
    ): ResponseEntity<SummerConditioningBreakdownResponse> {
        return try {
            ResponseEntity.ok(pointsService.getSummerConditioningBreakdown(runnerId, year))
        } catch (e: RuntimeException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
        }
    }

    @RequestMapping(value = ["/summer-conditioning/config"], method = [RequestMethod.GET])
    fun getSummerConditioningConfig(
            @RequestParam(value = "year", required = true) year: String
    ): ResponseEntity<SummerConditioningConfigDTO> {
        val config = try {
            pointsService.getSummerConditioningConfig(year)
        } catch (e: RuntimeException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No summer conditioning config for year $year")
        }
        return ResponseEntity.ok(SummerConditioningConfigDTO(year = config.year, startDate = config.startDate.toString(), endDate = config.endDate.toString(), pointsThreshold = config.pointsThreshold))
    }

    @RequestMapping(value = ["/summer-conditioning/config"], method = [RequestMethod.POST])
    fun upsertSummerConditioningConfig(
            @RequestBody request: UpsertSummerConditioningConfigRequest
    ): ResponseEntity<SummerConditioningConfigDTO> {
        val config = pointsService.upsertSummerConditioningConfig(request)
        return ResponseEntity.ok(SummerConditioningConfigDTO(year = config.year, startDate = config.startDate.toString(), endDate = config.endDate.toString(), pointsThreshold = config.pointsThreshold))
    }
}