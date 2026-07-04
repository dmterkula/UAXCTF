package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.WhyICan
import com.terkula.uaxctf.statisitcs.model.WhyICanActivityLink
import com.terkula.uaxctf.statisitcs.model.WhyICanComment
import com.terkula.uaxctf.statistics.repository.WhyICanActivityLinkRepository
import com.terkula.uaxctf.statistics.repository.WhyICanCommentRepository
import com.terkula.uaxctf.statistics.repository.WhyICanRepository
import com.terkula.uaxctf.statistics.request.CreateWhyICanActivityLinkRequest
import com.terkula.uaxctf.statistics.request.CreateWhyICanCommentRequest
import com.terkula.uaxctf.statistics.request.CreateWhyICanRequest
import com.terkula.uaxctf.statistics.request.ReorderWhyICanActivityLinksRequest
import com.terkula.uaxctf.statistics.request.UpdateWhyICanRequest
import com.terkula.uaxctf.statistics.response.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*

@Service
class WhyICanService(
    val whyICanRepository: WhyICanRepository,
    val whyICanCommentRepository: WhyICanCommentRepository,
    val whyICanActivityLinkRepository: WhyICanActivityLinkRepository
) {

    fun createEntry(request: CreateWhyICanRequest): WhyICanEntryDTO {
        val entry = WhyICan(
            uuid = request.uuid,
            runnerId = request.runnerId,
            text = request.text,
            size = request.size
        )
        whyICanRepository.save(entry)
        return toDTO(entry)
    }

    fun getEntriesForRunner(runnerId: Int): List<WhyICanEntryDTO> {
        return whyICanRepository.findByRunnerIdOrderByCreatedAtDesc(runnerId).map { toDTO(it) }
    }

    fun updateEntry(request: UpdateWhyICanRequest): WhyICanEntryDTO {
        val entry = whyICanRepository.findByUuid(request.uuid)
            ?: throw RuntimeException("Why I Can entry not found: ${request.uuid}")

        entry.text = request.text
        entry.size = request.size
        whyICanRepository.save(entry)
        return toDTO(entry)
    }

    @Transactional
    fun deleteEntry(uuid: String) {
        whyICanRepository.deleteByUuid(uuid)
    }

    fun createActivityLink(request: CreateWhyICanActivityLinkRequest): WhyICanActivityLinkDTO {
        val existing = whyICanActivityLinkRepository.findByRunnerIdAndActivityTypeAndActivityUuid(
            request.runnerId, request.activityType, request.activityUuid
        )
        if (existing != null) {
            existing.displayTitle = request.displayTitle
            whyICanActivityLinkRepository.save(existing)
            return toLinkDTO(existing)
        }

        val currentCount = whyICanActivityLinkRepository.findByRunnerIdOrderByDisplayOrderAsc(request.runnerId).size
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val link = WhyICanActivityLink(
            uuid = request.uuid,
            runnerId = request.runnerId,
            activityType = request.activityType,
            activityUuid = request.activityUuid,
            displayTitle = request.displayTitle,
            activityDate = Date(dateFormat.parse(request.activityDate).time),
            displayOrder = currentCount
        )
        whyICanActivityLinkRepository.save(link)
        return toLinkDTO(link)
    }

    @Transactional
    fun deleteActivityLink(uuid: String) {
        whyICanActivityLinkRepository.deleteByUuid(uuid)
    }

    fun getActivityLinksForRunner(runnerId: Int): List<WhyICanActivityLinkDTO> {
        return whyICanActivityLinkRepository.findByRunnerIdOrderByDisplayOrderAsc(runnerId).map { toLinkDTO(it) }
    }

    fun findActivityLink(runnerId: Int, activityType: String, activityUuid: String): WhyICanActivityLinkDTO? {
        return whyICanActivityLinkRepository.findByRunnerIdAndActivityTypeAndActivityUuid(
            runnerId, activityType, activityUuid
        )?.let { toLinkDTO(it) }
    }

    @Transactional
    fun reorderActivityLinks(request: ReorderWhyICanActivityLinksRequest) {
        val links = whyICanActivityLinkRepository.findByRunnerIdOrderByDisplayOrderAsc(request.runnerId)
            .associateBy { it.uuid }

        request.orderedUuids.forEachIndexed { index, uuid ->
            links[uuid]?.let { link ->
                link.displayOrder = index
                whyICanActivityLinkRepository.save(link)
            }
        }
    }

    fun createComment(request: CreateWhyICanCommentRequest): WhyICanNestedComment {
        val existingComment = whyICanCommentRepository.findByUuid(request.uuid)

        val comment = if (existingComment != null) {
            existingComment.message = request.message
            if (request.deviceId != null) {
                existingComment.deviceId = request.deviceId
            }
            whyICanCommentRepository.save(existingComment)
            existingComment
        } else {
            val newComment = WhyICanComment(
                uuid = request.uuid,
                whyICanRunnerId = request.whyICanRunnerId,
                parentCommentUuid = request.parentCommentUuid,
                username = request.username,
                displayName = request.displayName,
                deviceId = request.deviceId,
                message = request.message
            )
            whyICanCommentRepository.save(newComment)
            newComment
        }

        return toNestedComment(comment, emptyMap())
    }

    fun getFullResponse(runnerId: Int): WhyICanResponse {
        val entries = getEntriesForRunner(runnerId)
        val comments = getNestedComments(runnerId)
        val totalCount = countAllComments(comments)
        val activityLinks = getActivityLinksForRunner(runnerId)

        return WhyICanResponse(
            entries = entries,
            comments = comments,
            totalCommentCount = totalCount,
            activityLinks = activityLinks
        )
    }

    private fun getNestedComments(runnerId: Int): List<WhyICanNestedComment> {
        val allComments = whyICanCommentRepository.findByWhyICanRunnerIdOrderByCreatedAtAsc(runnerId)
        val commentsByParent = allComments.groupBy { it.parentCommentUuid }
        val topLevelComments = commentsByParent[null] ?: emptyList()
        return topLevelComments.map { toNestedComment(it, commentsByParent) }
    }

    private fun toNestedComment(
        comment: WhyICanComment,
        commentsByParent: Map<String?, List<WhyICanComment>>
    ): WhyICanNestedComment {
        val replies = commentsByParent[comment.uuid] ?: emptyList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return WhyICanNestedComment(
            uuid = comment.uuid,
            whyICanRunnerId = comment.whyICanRunnerId,
            username = comment.username,
            displayName = comment.displayName,
            deviceId = comment.deviceId,
            message = comment.message,
            createdAt = dateFormat.format(comment.createdAt),
            updatedAt = dateFormat.format(comment.updatedAt),
            replies = replies.map { toNestedComment(it, commentsByParent) }
        )
    }

    private fun countAllComments(comments: List<WhyICanNestedComment>): Int {
        return comments.size + comments.sumOf { countAllComments(it.replies) }
    }

    private fun toDTO(entry: WhyICan): WhyICanEntryDTO {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return WhyICanEntryDTO(
            uuid = entry.uuid,
            runnerId = entry.runnerId,
            text = entry.text,
            size = entry.size,
            createdAt = dateFormat.format(entry.createdAt),
            updatedAt = dateFormat.format(entry.updatedAt)
        )
    }

    private fun toLinkDTO(link: WhyICanActivityLink): WhyICanActivityLinkDTO {
        val tsFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        tsFormat.timeZone = TimeZone.getTimeZone("UTC")

        return WhyICanActivityLinkDTO(
            uuid = link.uuid,
            runnerId = link.runnerId,
            activityType = link.activityType,
            activityUuid = link.activityUuid,
            displayTitle = link.displayTitle,
            activityDate = link.activityDate.toString(),
            displayOrder = link.displayOrder,
            createdAt = tsFormat.format(link.createdAt),
            updatedAt = tsFormat.format(link.updatedAt)
        )
    }
}
