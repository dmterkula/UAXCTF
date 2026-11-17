package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.DismissReplyQueueBeforeDateRequest
import com.terkula.uaxctf.statistics.request.DismissReplyQueueItemRequest
import com.terkula.uaxctf.statistics.response.CoachReplyQueueActionResponse
import com.terkula.uaxctf.statistics.response.CoachReplyQueueResponse
import com.terkula.uaxctf.statistics.service.CoachReplyQueueService
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Date
import java.time.LocalDate
import javax.validation.Valid

@RestController
class CoachReplyQueueController(val coachReplyQueueService: CoachReplyQueueService) {

    @RequestMapping(value = ["xc/coach/reply-queue"], method = [RequestMethod.GET])
    fun getReplyQueue(
        @RequestParam(value = "coachUsername", required = true) coachUsername: String,
        @RequestParam(value = "season", required = true) season: String,
        @RequestParam(value = "startDate", required = false) startDate: Date?,
        @RequestParam(value = "endDate", required = false) endDate: Date?,
        @RequestParam(value = "team", required = false) team: String?
    ): CoachReplyQueueResponse {
        val end = endDate ?: Date.valueOf(LocalDate.now())
        val start = startDate ?: Date.valueOf(end.toLocalDate().minusDays(14))

        return coachReplyQueueService.getReplyQueue(coachUsername, season, start, end, team)
    }

    @RequestMapping(value = ["xc/coach/reply-queue/dismiss"], method = [RequestMethod.POST])
    fun dismiss(
        @RequestBody @Valid request: DismissReplyQueueItemRequest
    ): CoachReplyQueueActionResponse {
        return coachReplyQueueService.dismiss(request)
    }

    @RequestMapping(value = ["xc/coach/reply-queue/undismiss"], method = [RequestMethod.POST])
    fun undismiss(
        @RequestParam(value = "coachUsername", required = true) coachUsername: String,
        @RequestParam(value = "entityUuid", required = true) entityUuid: String
    ): CoachReplyQueueActionResponse {
        return coachReplyQueueService.undismiss(coachUsername, entityUuid)
    }

    @RequestMapping(value = ["xc/coach/reply-queue/dismiss-before"], method = [RequestMethod.POST])
    fun dismissBeforeDate(
        @RequestBody @Valid request: DismissReplyQueueBeforeDateRequest
    ): CoachReplyQueueActionResponse {
        return coachReplyQueueService.dismissBeforeDate(request)
    }
}
