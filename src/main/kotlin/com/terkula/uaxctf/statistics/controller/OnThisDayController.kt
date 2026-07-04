package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.OnThisDayResponse
import com.terkula.uaxctf.statistics.service.OnThisDayService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
class OnThisDayController(val onThisDayService: OnThisDayService) {

    @RequestMapping(value = ["xc/onThisDay"], method = [RequestMethod.GET])
    fun getOnThisDay(
        @RequestParam(value = "runnerId", required = true) runnerId: Int,
        @RequestParam(value = "month", required = true) month: Int,
        @RequestParam(value = "day", required = true) day: Int
    ): OnThisDayResponse {
        return onThisDayService.getOnThisDay(runnerId, month, day)
    }
}
