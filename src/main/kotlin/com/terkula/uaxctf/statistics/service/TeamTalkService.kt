package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.TeamTalk
import com.terkula.uaxctf.statisitcs.model.TeamTalkComment
import com.terkula.uaxctf.statisitcs.model.TeamTalkReaction
import com.terkula.uaxctf.statistics.repository.AuthenticationRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.TeamTalkCommentRepository
import com.terkula.uaxctf.statistics.repository.TeamTalkReactionRepository
import com.terkula.uaxctf.statistics.repository.TeamTalkRepository
import com.terkula.uaxctf.statistics.request.*
import com.terkula.uaxctf.statistics.response.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.SimpleDateFormat
import java.util.*

@Service
class TeamTalkService(
    val teamTalkRepository: TeamTalkRepository,
    val teamTalkCommentRepository: TeamTalkCommentRepository,
    val teamTalkReactionRepository: TeamTalkReactionRepository,
    val runnerRepository: RunnerRepository,
    val authenticationRepository: AuthenticationRepository,
    val pointsService: PointsService
) {

    // ===== TEAM TALK CRUD =====

    fun createTeamTalk(request: CreateTeamTalkRequest): TeamTalkResponse {
        val teamTalk = TeamTalk(
            uuid = request.uuid,
            author = request.author,
            season = request.season,
            year = request.year,
            title = request.title,
            content = request.content,
            team = request.team
        )

        teamTalkRepository.save(teamTalk)

        return TeamTalkResponse(
            teamTalk = teamTalk,
            reactions = emptyList(),
            comments = emptyList(),
            totalCommentCount = 0
        )
    }

    fun updateTeamTalk(request: UpdateTeamTalkRequest): TeamTalkResponse {
        val teamTalk = teamTalkRepository.findByUuid(request.uuid)
            ?: throw RuntimeException("Team talk not found: ${request.uuid}")

        teamTalk.title = request.title
        teamTalk.content = request.content
        teamTalkRepository.save(teamTalk)

        return getTeamTalk(request.uuid)
    }

    fun getTeamTalk(uuid: String): TeamTalkResponse {
        val teamTalk = teamTalkRepository.findByUuid(uuid)
            ?: throw RuntimeException("Team talk not found: $uuid")

        val reactions = getReactionSummary(uuid)
        val comments = getNestedComments(uuid)
        val totalCount = countAllComments(comments)

        return TeamTalkResponse(
            teamTalk = teamTalk,
            reactions = reactions,
            comments = comments,
            totalCommentCount = totalCount
        )
    }

    fun getTeamTalksBySeason(season: String, year: String, team: String = "UA"): TeamTalkListResponse {
        val teamTalks = teamTalkRepository.findBySeasonAndYearAndTeamOrderByCreatedAtDesc(season, year, team)

        val responses = teamTalks.map { teamTalk ->
            val reactions = getReactionSummary(teamTalk.uuid)
            val comments = getNestedComments(teamTalk.uuid)
            val totalCount = countAllComments(comments)

            TeamTalkResponse(
                teamTalk = teamTalk,
                reactions = reactions,
                comments = comments,
                totalCommentCount = totalCount
            )
        }

        return TeamTalkListResponse(responses)
    }

    fun getAllTeamTalks(): TeamTalkListResponse {
        val teamTalks = teamTalkRepository.findAllByOrderByCreatedAtDesc()

        val responses = teamTalks.map { teamTalk ->
            val reactions = getReactionSummary(teamTalk.uuid)
            val comments = getNestedComments(teamTalk.uuid)
            val totalCount = countAllComments(comments)

            TeamTalkResponse(
                teamTalk = teamTalk,
                reactions = reactions,
                comments = comments,
                totalCommentCount = totalCount
            )
        }

        return TeamTalkListResponse(responses)
    }

    // ===== REACTIONS =====

    @Transactional
    fun addReaction(request: AddTeamTalkReactionRequest): List<ReactionSummary> {
        // Check if reaction already exists
        val existing = teamTalkReactionRepository.findByTeamTalkUuidAndUsernameAndEmoji(
            request.teamTalkUuid, request.username, request.emoji
        )

        if (existing == null) {
            val reaction = TeamTalkReaction(
                teamTalkUuid = request.teamTalkUuid,
                username = request.username,
                displayName = request.displayName,
                emoji = request.emoji
            )
            teamTalkReactionRepository.save(reaction)
        }

        return getReactionSummary(request.teamTalkUuid)
    }

    @Transactional
    fun removeReaction(request: RemoveTeamTalkReactionRequest): List<ReactionSummary> {
        teamTalkReactionRepository.deleteByTeamTalkUuidAndUsernameAndEmoji(
            request.teamTalkUuid, request.username, request.emoji
        )

        return getReactionSummary(request.teamTalkUuid)
    }

    private fun getReactionSummary(teamTalkUuid: String): List<ReactionSummary> {
        val reactions = teamTalkReactionRepository.findByTeamTalkUuid(teamTalkUuid)


        return reactions.groupBy { it.emoji }
            .map { (emoji, reactionList) ->
                ReactionSummary(
                    emoji = emoji,
                    count = reactionList.size,
                    usernames = reactionList.mapNotNull { it.username }.distinct()
                )
            }
            .sortedByDescending { it.count }
    }

    // ===== COMMENTS =====

    @Transactional
    fun createComment(request: CreateTeamTalkCommentRequest): CommentCreatedResponse {
        // Get runner for points
        val user = authenticationRepository.findByUsername(request.username)

        val runner = if (user != null && user.runnerId != null) {
            runnerRepository.findById(user.runnerId!!).get()
        } else {
            null
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Parse the request timestamp string
        val timestamp = java.sql.Timestamp(dateFormat.parse(request.timestamp).time)

        // Check if comment with this UUID already exists (UPDATE scenario)
        val existingComment = teamTalkCommentRepository.findByUuid(request.uuid)

        // Query for OTHER comments by this user (excluding the current comment UUID)
        // This tells us if this is their first comment on this team talk
        val otherUserComments = teamTalkCommentRepository.findByTeamTalkUuidAndUsername(request.teamTalkUuid, request.username)
            .filter { it.uuid != request.uuid }

        val comment = if (existingComment != null) {
            // UPDATE: Comment already exists, update the message and timestamp
            existingComment.message = request.message
            existingComment.updatedAt = timestamp
            // Update deviceId if provided
            if (request.deviceId != null) {
                existingComment.deviceId = request.deviceId
            }
            teamTalkCommentRepository.save(existingComment)
            existingComment
        } else {
            // CREATE: New comment
            val newComment = TeamTalkComment(
                uuid = request.uuid,
                teamTalkUuid = request.teamTalkUuid,
                parentCommentUuid = request.parentCommentUuid,
                username = request.username,
                displayName = request.displayName,
                deviceId = request.deviceId,
                message = request.message,
                createdAt = timestamp,
                updatedAt = timestamp
            )
            teamTalkCommentRepository.save(newComment)
            newComment
        }

        // Only award points for NEW comments (not updates)
        if (existingComment == null && runner != null) {

            // is first comment if there were no OTHER comments by this user on this team talk
            val isFirstComment = otherUserComments.isEmpty()

            if (isFirstComment) {
                // Award points only for the FIRST comment by this user on this team talk
                var pointsEarned = 0
                var newBalance = runner.points

                try {
                    val pointsResponse = pointsService.earnPoints(EarnPointsRequest(
                        runnerId = runner.id,
                        activityType = "TEAM_TALK_COMMENT",
                        activityUuid = "${request.teamTalkUuid}_${request.username}",  // Use team talk + username for idempotency
                        season = request.season,
                        year = request.year,
                        description = "Commented on team talk"
                    ))
                    pointsEarned = pointsResponse.pointsEarned
                    newBalance = pointsResponse.newBalance
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Don't fail comment creation if points fail
                }

                return CommentCreatedResponse(
                    comment = comment,
                    pointsEarned = pointsEarned,
                    newPointBalance = newBalance
                )
            }
        }

        // No points awarded (either update, not first comment, or not a runner)
        return CommentCreatedResponse(
            comment = comment,
            pointsEarned = 0,
            newPointBalance = if (runner != null) runner.points else 0
        )
    }

    /**
     * Builds nested comment structure efficiently
     * Fetches all comments in a single query and builds the tree in memory
     * to avoid N+1 query problems
     */
    private fun getNestedComments(teamTalkUuid: String): List<NestedComment> {
        // Fetch ALL comments at once to avoid N+1 queries
        val allComments = teamTalkCommentRepository.findByTeamTalkUuidOrderByCreatedAtAsc(teamTalkUuid)

        // Build map of parent UUID -> list of replies
        val commentsByParent = allComments.groupBy { it.parentCommentUuid }

        // Get top-level comments (no parent)
        val topLevelComments = commentsByParent[null] ?: emptyList()

        // Recursively build nested structure
        return topLevelComments.map { buildNestedComment(it, commentsByParent) }
    }

    /**
     * Recursively builds a nested comment with all its replies
     * Formats timestamps as ISO 8601 strings for iOS
     */
    private fun buildNestedComment(
        comment: TeamTalkComment,
        commentsByParent: Map<String?, List<TeamTalkComment>>
    ): NestedComment {
        val replies = commentsByParent[comment.uuid] ?: emptyList()

        // Format timestamps as ISO 8601 strings
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return NestedComment(
            uuid = comment.uuid,
            teamTalkUuid = comment.teamTalkUuid,
            username = comment.username,
            displayName = comment.displayName,
            deviceId = comment.deviceId,
            message = comment.message,
            createdAt = dateFormat.format(comment.createdAt),
            updatedAt = dateFormat.format(comment.updatedAt),
            replies = replies.map { buildNestedComment(it, commentsByParent) }
        )
    }

    /**
     * Recursively counts all comments including nested replies
     */
    private fun countAllComments(comments: List<NestedComment>): Int {
        return comments.size + comments.sumOf { countAllComments(it.replies) }
    }
}
