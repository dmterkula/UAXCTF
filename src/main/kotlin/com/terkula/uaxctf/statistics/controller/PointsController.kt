package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.PointTransaction
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.request.*
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.PointsService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/api/v1/points")
class PointsController(
        val pointsService: PointsService
) {

    @ApiOperation("Get current point balance for a runner")
    @RequestMapping(value = ["/balance"], method = [RequestMethod.GET])
    fun getPointBalance(
            @ApiParam("The runner ID")
            @RequestParam(value = "runnerId", required = true) runnerId: Int
    ): ResponseEntity<PointBalanceResponse> {
        val balance = pointsService.getPointBalance(runnerId)
        return ResponseEntity.ok(balance)
    }

    @ApiOperation("Validate if a runner has enough points for a feature")
    @RequestMapping(value = ["/validate"], method = [RequestMethod.POST])
    fun validateSufficientPoints(
            @RequestBody request: ValidatePointsRequest
    ): ResponseEntity<ValidatePointsResponse> {
        val validation = pointsService.validatePoints(request)
        return ResponseEntity.ok(validation)
    }

    @ApiOperation("Spend points for a feature (GIF, pin, animation, etc)")
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

    @ApiOperation("Refund points from a failed transaction")
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

    @ApiOperation("Get transaction history for a runner")
    @RequestMapping(value = ["/transactions"], method = [RequestMethod.GET])
    fun getTransactionHistory(
            @ApiParam("The runner ID")
            @RequestParam(value = "runnerId", required = true) runnerId: Int,
            @ApiParam("Limit number of results (default 50, set to 0 for all)")
            @RequestParam(value = "limit", required = false, defaultValue = "50") limit: Int
    ): ResponseEntity<List<PointTransaction>> {
        val transactions = pointsService.getTransactionHistory(runnerId, limit)
        return ResponseEntity.ok(transactions)
    }

    @ApiOperation("Get all points configuration values")
    @RequestMapping(value = ["/config"], method = [RequestMethod.GET])
    fun getPointsConfiguration(): ResponseEntity<PointsConfigResponse> {
        val config = pointsService.getConfiguration()
        return ResponseEntity.ok(PointsConfigResponse(config))
    }

    @ApiOperation("Update points configuration (admin/coach only)")
    @RequestMapping(value = ["/config"], method = [RequestMethod.PUT])
    fun updatePointsConfiguration(
            @RequestBody request: UpdatePointsConfigRequest
    ): ResponseEntity<Void> {
        pointsService.updateConfiguration(request)
        return ResponseEntity.ok().build()
    }

    @ApiOperation("Get points leaderboard")
    @RequestMapping(value = ["/leaderboard"], method = [RequestMethod.GET])
    fun getLeaderboard(
            @ApiParam("Limit number of results (default 100)")
            @RequestParam(value = "limit", required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<List<Runner>> {
        val leaderboard = pointsService.getLeaderboard(limit)
        return ResponseEntity.ok(leaderboard)
    }

    @ApiOperation("Award points for activity (internal use - called by activity controllers)")
    @RequestMapping(value = ["/earn"], method = [RequestMethod.POST])
    fun earnPoints(
            @RequestBody request: EarnPointsRequest
    ): ResponseEntity<EarnPointsResponse> {
        val result = pointsService.earnPoints(request)
        return ResponseEntity.ok(result)
    }
}