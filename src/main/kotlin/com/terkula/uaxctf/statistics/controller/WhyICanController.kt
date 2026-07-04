package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.request.*
import com.terkula.uaxctf.statistics.response.*
import com.terkula.uaxctf.statistics.service.WhyICanService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid


@RestController
@Validated
@RequestMapping("/api/v1/why-i-can")
class WhyICanController(
    val whyICanService: WhyICanService
) {

    @RequestMapping(value = ["/{runnerId}"], method = [RequestMethod.GET])
    fun getWhyICansForRunner(
        @PathVariable runnerId: Int
    ): ResponseEntity<WhyICanResponse> {
        val response = whyICanService.getFullResponse(runnerId)
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = [""], method = [RequestMethod.POST])
    fun createEntry(
        @RequestBody @Valid request: CreateWhyICanRequest
    ): ResponseEntity<WhyICanEntryDTO> {
        val entry = whyICanService.createEntry(request)
        return ResponseEntity.ok(entry)
    }

    @RequestMapping(value = [""], method = [RequestMethod.PUT])
    fun updateEntry(
        @RequestBody @Valid request: UpdateWhyICanRequest
    ): ResponseEntity<WhyICanEntryDTO> {
        val entry = whyICanService.updateEntry(request)
        return ResponseEntity.ok(entry)
    }

    @RequestMapping(value = ["/{uuid}"], method = [RequestMethod.DELETE])
    fun deleteEntry(
        @PathVariable uuid: String
    ): ResponseEntity<Void> {
        whyICanService.deleteEntry(uuid)
        return ResponseEntity.ok().build()
    }

    @RequestMapping(value = ["/comments"], method = [RequestMethod.POST])
    fun createComment(
        @RequestBody @Valid request: CreateWhyICanCommentRequest
    ): ResponseEntity<WhyICanNestedComment> {
        val comment = whyICanService.createComment(request)
        return ResponseEntity.ok(comment)
    }

    @RequestMapping(value = ["/activity-links"], method = [RequestMethod.POST])
    fun createActivityLink(
        @RequestBody @Valid request: CreateWhyICanActivityLinkRequest
    ): ResponseEntity<WhyICanActivityLinkDTO> {
        val link = whyICanService.createActivityLink(request)
        return ResponseEntity.ok(link)
    }

    @RequestMapping(value = ["/activity-links/check"], method = [RequestMethod.GET])
    fun checkActivityLink(
        @RequestParam runnerId: Int,
        @RequestParam activityType: String,
        @RequestParam activityUuid: String
    ): ResponseEntity<Map<String, Any?>> {
        val link = whyICanService.findActivityLink(runnerId, activityType, activityUuid)
        return ResponseEntity.ok(mapOf("isStarred" to (link != null), "uuid" to link?.uuid))
    }

    @RequestMapping(value = ["/activity-links/{uuid}"], method = [RequestMethod.DELETE])
    fun deleteActivityLink(
        @PathVariable uuid: String
    ): ResponseEntity<Void> {
        whyICanService.deleteActivityLink(uuid)
        return ResponseEntity.ok().build()
    }

    @RequestMapping(value = ["/activity-links/reorder"], method = [RequestMethod.PUT])
    fun reorderActivityLinks(
        @RequestBody @Valid request: ReorderWhyICanActivityLinksRequest
    ): ResponseEntity<Void> {
        whyICanService.reorderActivityLinks(request)
        return ResponseEntity.ok().build()
    }
}
