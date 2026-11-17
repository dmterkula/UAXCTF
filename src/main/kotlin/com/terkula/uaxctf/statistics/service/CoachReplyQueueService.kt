package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.CoachReplyDismissal
import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.TrainingRun
import com.terkula.uaxctf.training.response.WorkoutResponseDTO
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingDTO
import com.terkula.uaxctf.training.response.lifting.LiftingActivityDTO
import com.terkula.uaxctf.training.service.CrossTrainingService
import com.terkula.uaxctf.training.service.LiftingService
import com.terkula.uaxctf.training.service.WorkoutService
import com.terkula.uaxctf.statistics.repository.AuthenticationRepository
import com.terkula.uaxctf.statistics.repository.CoachReplyDismissalRepository
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetLogRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.request.DismissReplyQueueBeforeDateRequest
import com.terkula.uaxctf.statistics.request.DismissReplyQueueItemRequest
import com.terkula.uaxctf.statistics.response.CoachReplyQueueActionResponse
import com.terkula.uaxctf.statistics.response.CoachReplyQueueItem
import com.terkula.uaxctf.statistics.response.CoachReplyQueueResponse
import com.terkula.uaxctf.statistics.response.ReplyQueuePriority
import com.terkula.uaxctf.statistics.response.ReplyQueueType
import com.terkula.uaxctf.training.model.TrainingComment
import com.terkula.uaxctf.training.repository.CrossTrainingRecordRepository
import com.terkula.uaxctf.training.repository.CrossTrainingRepository
import com.terkula.uaxctf.training.repository.LiftingActivityRepository
import com.terkula.uaxctf.training.repository.LiftingRecordRepository
import com.terkula.uaxctf.training.repository.MeetLogRepository
import com.terkula.uaxctf.training.repository.RunnerWorkoutDistanceRepository
import com.terkula.uaxctf.training.repository.RunnersTrainingRunRepository
import com.terkula.uaxctf.training.repository.TrainingCommentRepository
import com.terkula.uaxctf.training.repository.TrainingRunRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.repository.XcPreMeetLogRepository
import com.terkula.uaxctf.util.getYearString
import org.springframework.stereotype.Service
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Pain at or above this on the athlete's 0-10 scale pushes a log to the top of the queue. */
private const val PAIN_PRIORITY_THRESHOLD = 3.0

/** Max entity keys per comment lookup, to keep the generated SQL IN clause reasonable. */
private const val COMMENT_LOOKUP_CHUNK_SIZE = 500

/**
 * Text athletes type into a free-text field to mean "nothing here". Treated as blank so a
 * pre-meet log whose question reads "n/a" doesn't get flagged as a question needing an answer.
 */
private val NON_ANSWERS = setOf(
    "n/a", "na", "nan", "none", "nope", "no", "nothing", "idk", "x", "/",
    "no questions", "no question", "none right now", "none at the moment",
    "nothing right now", "no questions right now", "nothing really", "not really",
    "all good", "im good", "i'm good", "good", "nothing to add", "no thanks"
)

/**
 * Whether a free-text field actually says something. Compared after stripping surrounding
 * punctuation so "NONE!" and "n/a." land on the same entry as "none". Matching is exact rather
 * than prefix-based on purpose -- a real question like "no idea how to pace this, help?" starts
 * with "no" and must not be swallowed.
 */
private fun String?.isMeaningful(): Boolean {
    if (this == null) return false
    val normalized = this.trim()
        .lowercase()
        .trim { !it.isLetterOrDigit() }
    if (normalized.isEmpty()) return false
    return normalized !in NON_ANSWERS
}

private fun String?.meaningfulOrNull(): String? = if (this.isMeaningful()) this!!.trim() else null

@Service
class CoachReplyQueueService(
    val runnerService: RunnerService,
    val trainingCommentRepository: TrainingCommentRepository,
    val dismissalRepository: CoachReplyDismissalRepository,
    val authenticationRepository: AuthenticationRepository,
    val trainingRunRepository: TrainingRunRepository,
    val runnersTrainingRunRepository: RunnersTrainingRunRepository,
    val workoutService: WorkoutService,
    val runnerWorkoutDistanceRepository: RunnerWorkoutDistanceRepository,
    val crossTrainingService: CrossTrainingService,
    val crossTrainingRecordRepository: CrossTrainingRecordRepository,
    val meetRepository: MeetRepository,
    val meetLogRepository: MeetLogRepository,
    val xcPreMeetLogRepository: XcPreMeetLogRepository,
    val trackMeetRepository: TrackMeetRepository,
    val trackMeetLogRepository: TrackMeetLogRepository,
    val liftingService: LiftingService,
    val liftingRecordRepository: LiftingRecordRepository
) {

    /**
     * A log before the reply-state checks have been applied. Gathering every log type into this
     * one shape first means the "has a coach answered this?" and "did the athlete actually say
     * anything?" rules live in exactly one place instead of once per log type.
     */
    private data class Candidate(
        val entityUuid: String,
        val type: String,
        val runner: Runner,
        val date: Date,
        val title: String,
        val athleteNote: String?,
        val athleteQuestion: String? = null,
        val effortLevel: Double? = null,
        val painLevel: Double? = null,
        val painNotes: String? = null,
        /** Coach notes column on the log, if the type has one. Non-blank counts as answered. */
        val coachNotes: String? = null,
        val canReplyInline: Boolean = true,
        /** Extra athlete-authored text that counts as "said something" (meet reflections). */
        val extraAthleteContent: List<String?> = emptyList(),
        /** Parent activity, passed through so the client can open the record's real screen. */
        val trainingRun: TrainingRun? = null,
        val crossTraining: CrossTrainingDTO? = null,
        val liftingActivity: LiftingActivityDTO? = null,
        val workout: WorkoutResponseDTO? = null,
        val meet: Meet? = null
    )

    /**
     * Every log in the window is read with a handful of batched queries rather than by walking
     * the training dashboard per runner -- the dashboard costs several queries per athlete, which
     * put this endpoint into the minutes for a full roster.
     */
    fun getReplyQueue(
        coachUsername: String,
        season: String,
        startDate: Date,
        endDate: Date,
        team: String?
    ): CoachReplyQueueResponse {

        val roster = loadRoster(season, startDate, team)
        val rosterById: Map<Int, Runner> = roster.associateBy { it.id }

        if (rosterById.isEmpty()) {
            return CoachReplyQueueResponse(emptyList(), 0, emptyMap(), 0)
        }

        val candidates = mutableListOf<Candidate>()
        candidates += gatherTrainingRunCandidates(startDate, endDate, season, team, rosterById)
        candidates += gatherWorkoutCandidates(startDate, endDate, season, team, rosterById)
        candidates += gatherCrossTrainingCandidates(startDate, endDate, season, team, rosterById)
        candidates += gatherLiftingCandidates(startDate, endDate, team, rosterById)
        candidates += gatherMeetCandidates(startDate, endDate, team, rosterById)

        if (candidates.isEmpty()) {
            return CoachReplyQueueResponse(emptyList(), 0, emptyMap(), 0)
        }

        val commentsByEntity: Map<String, List<TrainingComment>> = loadComments(
            candidates.map { it.entityUuid }.distinct()
        )

        val coachUsernames: Set<String> = authenticationRepository.findByRole("coach")
            .map { it.username }
            .toSet()

        val dismissedUuids: Set<String> = dismissalRepository.findByCoachUsername(coachUsername)
            .map { it.entityUuid }
            .toSet()

        val today = LocalDate.now()

        val items = candidates
            .filter { it.entityUuid !in dismissedUuids }
            .filter { !hasCoachResponded(it, commentsByEntity[it.entityUuid].orEmpty(), coachUsernames) }
            .filter { hasAthleteContent(it, commentsByEntity[it.entityUuid].orEmpty(), coachUsernames) }
            .map { candidate ->
                val comments = commentsByEntity[candidate.entityUuid].orEmpty()
                val athleteComments = comments.filter { it.madeBy !in coachUsernames }
                val lastActivity = athleteComments.maxByOrNull { it.timestamp }?.timestamp

                CoachReplyQueueItem(
                    entityUuid = candidate.entityUuid,
                    type = candidate.type,
                    runner = candidate.runner,
                    date = candidate.date,
                    title = candidate.title,
                    athleteNote = candidate.athleteNote.meaningfulOrNull(),
                    athleteQuestion = candidate.athleteQuestion.meaningfulOrNull(),
                    effortLevel = candidate.effortLevel,
                    painLevel = candidate.painLevel,
                    painNotes = candidate.painNotes.meaningfulOrNull(),
                    athleteCommentCount = athleteComments.size,
                    lastAthleteActivity = lastActivity,
                    daysWaiting = daysBetween(candidate.date, lastActivity, today),
                    canReplyInline = candidate.canReplyInline,
                    priority = priorityOf(candidate),
                    trainingRun = candidate.trainingRun,
                    crossTraining = candidate.crossTraining,
                    liftingActivity = candidate.liftingActivity,
                    workout = candidate.workout,
                    meet = candidate.meet
                )
            }
            // Most urgent first, then oldest-waiting, so the backlog surfaces above today's logs.
            .sortedWith(
                compareByDescending<CoachReplyQueueItem> { it.priority == ReplyQueuePriority.HIGH }
                    .thenByDescending { it.daysWaiting }
                    .thenBy { it.runner.name }
            )

        return CoachReplyQueueResponse(
            items = items,
            totalCount = items.size,
            countsByType = items.groupingBy { it.type }.eachCount(),
            highPriorityCount = items.count { it.priority == ReplyQueuePriority.HIGH }
        )
    }

    /**
     * Fetches comments for every candidate, chunked so the generated IN clause stays a sane size.
     * A wide window (the year-long one behind "clear the backlog") can produce tens of thousands
     * of keys, which is more than one statement should carry.
     */
    private fun loadComments(entityUuids: List<String>): Map<String, List<TrainingComment>> {
        return entityUuids.chunked(COMMENT_LOOKUP_CHUNK_SIZE)
            .flatMap { trainingCommentRepository.findByTrainingEntityUuidIn(it) }
            .groupBy { it.trainingEntityUuid }
    }

    /** Mirrors the roster selection the training dashboard uses, so both views agree on who counts. */
    private fun loadRoster(season: String, startDate: Date, team: String?): List<Runner> {
        return if (team != null && !team.equals("UA", ignoreCase = true)) {
            runnerService.getRosterByTeam(true, startDate.getYearString(), team)
        } else if (season == "track") {
            if (startDate.toLocalDate().monthValue >= 11) {
                runnerService.getTrackRoster(true, (startDate.getYearString().toInt() + 1).toString())
            } else {
                runnerService.getTrackRoster(true, startDate.getYearString())
            }.filter { it.team.equals("UA", ignoreCase = true) }
        } else {
            runnerService.getXcRoster(true, startDate.getYearString())
                .filter { it.team.equals("UA", ignoreCase = true) }
        }
    }

    // ---------------------------------------------------------------- candidate gathering

    private fun gatherTrainingRunCandidates(
        startDate: Date,
        endDate: Date,
        season: String,
        team: String?,
        rosterById: Map<Int, Runner>
    ): List<Candidate> {

        val runs = if (team != null) {
            trainingRunRepository.findByDateBetweenAndSeasonAndTeam(startDate, endDate, season, team)
        } else {
            trainingRunRepository.findByDateBetweenAndSeason(startDate, endDate, season)
        }
        if (runs.isEmpty()) return emptyList()

        val runsByUuid = runs.associateBy { it.uuid }

        return runnersTrainingRunRepository.findByTrainingRunUuidIn(runs.map { it.uuid })
            .mapNotNull { record ->
                val runner = rosterById[record.runnerId] ?: return@mapNotNull null
                val run = runsByUuid[record.trainingRunUuid] ?: return@mapNotNull null

                Candidate(
                    entityUuid = record.uuid,
                    type = ReplyQueueType.TRAINING_RUN,
                    runner = runner,
                    date = run.date,
                    title = run.name,
                    athleteNote = record.notes,
                    effortLevel = record.effortLevel,
                    painLevel = record.painLevel,
                    painNotes = record.painNotes,
                    coachNotes = record.coachNotes,
                    trainingRun = run
                )
            }
    }

    private fun gatherWorkoutCandidates(
        startDate: Date,
        endDate: Date,
        season: String,
        team: String?,
        rosterById: Map<Int, Runner>
    ): List<Candidate> {

        // Built through WorkoutService so each item carries the same fully-assembled DTO
        // (components, venue, visibility) the app's own workout screens consume.
        val workouts = workoutService.getWorkoutsV2(startDate, endDate)
            .filter { it.season.equals(season, ignoreCase = true) }
            .filter { team == null || it.team.equals(team, ignoreCase = true) }

        if (workouts.isEmpty()) return emptyList()

        val workoutsByUuid = workouts.associateBy { it.uuid }

        return runnerWorkoutDistanceRepository.findByWorkoutUuidIn(workouts.map { it.uuid })
            .mapNotNull { record ->
                val runner = rosterById[record.runnerId] ?: return@mapNotNull null
                val workout = workoutsByUuid[record.workoutUuid] ?: return@mapNotNull null

                Candidate(
                    // Matches the key the workout comment form already writes: uuid + runnerId,
                    // concatenated with no separator.
                    entityUuid = record.workoutUuid + record.runnerId.toString(),
                    type = ReplyQueueType.WORKOUT,
                    runner = runner,
                    date = workout.date,
                    title = workout.title,
                    athleteNote = record.notes,
                    effortLevel = record.effortLevel,
                    painLevel = record.painLevel,
                    painNotes = record.painNotes,
                    coachNotes = record.coachNotes,
                    workout = workout
                )
            }
    }

    private fun gatherCrossTrainingCandidates(
        startDate: Date,
        endDate: Date,
        season: String,
        team: String?,
        rosterById: Map<Int, Runner>
    ): List<Candidate> {

        // Via CrossTrainingService so each item carries the same DTO the app's own
        // cross-training screens consume (venue and visibility resolved).
        val events = crossTrainingService.getCrossTrainingActivities(startDate, endDate)
            .crossTrainingActivities
            .filter { it.season.equals(season, ignoreCase = true) }
            .filter { team == null || it.team.equals(team, ignoreCase = true) }

        if (events.isEmpty()) return emptyList()

        val eventsByUuid = events.associateBy { it.uuid }

        return crossTrainingRecordRepository.findByCrossTrainingUuidIn(events.map { it.uuid })
            .mapNotNull { record ->
                val runner = rosterById[record.runnerId] ?: return@mapNotNull null
                val event = eventsByUuid[record.crossTrainingUuid] ?: return@mapNotNull null

                Candidate(
                    entityUuid = record.uuid,
                    type = ReplyQueueType.CROSS_TRAINING,
                    runner = runner,
                    date = event.date,
                    title = event.name,
                    athleteNote = record.notes,
                    effortLevel = record.effortLevel,
                    coachNotes = record.coachesNotes,
                    crossTraining = event
                )
            }
    }

    private fun gatherLiftingCandidates(
        startDate: Date,
        endDate: Date,
        team: String?,
        rosterById: Map<Int, Runner>
    ): List<Candidate> {

        // Via LiftingService so suggestedExercises arrives as a parsed list rather than the
        // raw JSON string the entity stores, matching what the app's lifting screens decode.
        val activities = liftingService.getLiftingActivities(startDate, endDate, team).activities
        if (activities.isEmpty()) return emptyList()

        val activitiesByUuid = activities.associateBy { it.uuid }

        return liftingRecordRepository.findByLiftingActivityUuidIn(activities.map { it.uuid })
            .mapNotNull { record ->
                val runner = rosterById[record.runnerId] ?: return@mapNotNull null
                val activity = activitiesByUuid[record.liftingActivityUuid] ?: return@mapNotNull null

                Candidate(
                    entityUuid = record.uuid,
                    type = ReplyQueueType.LIFTING,
                    runner = runner,
                    // LiftingActivityDTO carries its date as an ISO string.
                    date = Date.valueOf(activity.date),
                    title = activity.name,
                    athleteNote = record.notes,
                    effortLevel = record.effortLevel,
                    coachNotes = record.coachesNotes,
                    liftingActivity = activity
                )
            }
    }

    /**
     * Meet logs are read straight from their repositories rather than through MeetLogService,
     * which assembles a response per meet per runner. Reading the tables directly also keeps
     * pre-meet logs whose meet hasn't happened yet -- exactly the ones most worth answering.
     */
    private fun gatherMeetCandidates(
        startDate: Date,
        endDate: Date,
        team: String?,
        rosterById: Map<Int, Runner>
    ): List<Candidate> {

        val candidates = mutableListOf<Candidate>()

        val meets = meetRepository.findByDateBetween(startDate, endDate)
            .filter { team == null || it.team.equals(team, ignoreCase = true) }

        if (meets.isNotEmpty()) {
            val meetsByUuid = meets.associateBy { it.uuid }
            val meetIds = meets.map { it.uuid }

            xcPreMeetLogRepository.findByMeetIdIn(meetIds).forEach { log ->
                val runner = rosterById[log.runnerId] ?: return@forEach
                val meet = meetsByUuid[log.meetId] ?: return@forEach

                candidates += Candidate(
                    entityUuid = log.uuid,
                    type = ReplyQueueType.PRE_MEET,
                    runner = runner,
                    date = meet.date,
                    title = meet.name,
                    athleteNote = log.goals.meaningfulOrNull() ?: log.plan,
                    athleteQuestion = log.questions,
                    extraAthleteContent = listOf(
                        log.goals, log.plan, log.confidence,
                        log.preparation, log.whenItsHard, log.questions, log.notes
                    ),
                    meet = meet
                )
            }

            meetLogRepository.findByMeetIdIn(meetIds).forEach { log ->
                val runner = rosterById[log.runnerId] ?: return@forEach
                val meet = meetsByUuid[log.meetId] ?: return@forEach

                candidates += Candidate(
                    // Matches the key MeetLogService uses for post-meet comments.
                    entityUuid = log.meetId + "-" + log.runnerId,
                    type = ReplyQueueType.POST_MEET,
                    runner = runner,
                    date = meet.date,
                    title = meet.name,
                    athleteNote = log.notes,
                    coachNotes = log.coachNotes,
                    extraAthleteContent = listOf(log.notes, log.happyWith, log.notHappyWith),
                    meet = meet
                )
            }
        }

        val trackMeets = trackMeetRepository.findByDateBetween(startDate, endDate)
            .filter { team == null || it.team.equals(team, ignoreCase = true) }

        if (trackMeets.isNotEmpty()) {
            val trackMeetsByUuid = trackMeets.associateBy { it.uuid }

            trackMeetLogRepository.findByMeetIdIn(trackMeets.map { it.uuid }).forEach { log ->
                val runner = rosterById[log.runnerId] ?: return@forEach
                val meet = trackMeetsByUuid[log.meetId] ?: return@forEach

                candidates += Candidate(
                    // An athlete can log several events at one track meet, so the event is part
                    // of the key. No comment thread exists for these -- coach notes on the log
                    // itself is the only reply channel.
                    entityUuid = log.meetId + "-" + log.runnerId + "-" + log.event,
                    type = ReplyQueueType.TRACK_POST_MEET,
                    runner = runner,
                    date = meet.date,
                    title = meet.name + " (" + log.event + ")",
                    athleteNote = log.notes,
                    coachNotes = log.coachNotes,
                    canReplyInline = false,
                    // Projected to a Meet the same way MeetLogService does, so the client has
                    // one meet shape to open regardless of which sport the log came from.
                    meet = Meet(meet.name, meet.date, meet.uuid, meet.icon, meet.team)
                )
            }
        }

        return candidates
    }

    // ---------------------------------------------------------------- reply-state rules

    /**
     * Answered means a coach has either commented on the log or written into its coach notes
     * column. Coach comments are told apart from athlete ones by author: comments store the
     * coach's AppUser username, while an athlete's store their display name.
     */
    private fun hasCoachResponded(
        candidate: Candidate,
        comments: List<TrainingComment>,
        coachUsernames: Set<String>
    ): Boolean {
        if (!candidate.coachNotes.isNullOrBlank()) return true
        return comments.any { it.madeBy in coachUsernames }
    }

    /**
     * Keeps the queue to logs that actually contain something to respond to. Without this, every
     * logged run -- including the hundreds with no note, no pain flag and nothing asked -- would
     * land in the queue and bury the handful that matter.
     */
    private fun hasAthleteContent(
        candidate: Candidate,
        comments: List<TrainingComment>,
        coachUsernames: Set<String>
    ): Boolean {
        if (candidate.athleteNote.isMeaningful()) return true
        if (candidate.athleteQuestion.isMeaningful()) return true
        if (candidate.painNotes.isMeaningful()) return true
        if (candidate.painLevel != null && candidate.painLevel >= PAIN_PRIORITY_THRESHOLD) return true
        if (candidate.extraAthleteContent.any { it.isMeaningful() }) return true
        return comments.any { it.madeBy !in coachUsernames }
    }

    private fun priorityOf(candidate: Candidate): String {
        val askedSomething = candidate.athleteQuestion.isMeaningful()
        val flaggedPain = candidate.painNotes.isMeaningful() ||
                (candidate.painLevel != null && candidate.painLevel >= PAIN_PRIORITY_THRESHOLD)

        return if (askedSomething || flaggedPain) ReplyQueuePriority.HIGH else ReplyQueuePriority.NORMAL
    }

    private fun daysBetween(logDate: Date, lastActivity: Timestamp?, today: LocalDate): Int {
        val from = lastActivity?.toLocalDateTime()?.toLocalDate() ?: logDate.toLocalDate()
        return ChronoUnit.DAYS.between(from, today).toInt().coerceAtLeast(0)
    }

    // ---------------------------------------------------------------- dismissals

    fun dismiss(request: DismissReplyQueueItemRequest): CoachReplyQueueActionResponse {
        val existing = dismissalRepository
            .findByCoachUsernameAndEntityUuid(request.coachUsername, request.entityUuid)

        if (existing != null) {
            return CoachReplyQueueActionResponse(0, "Already dismissed")
        }

        dismissalRepository.save(
            CoachReplyDismissal(
                coachUsername = request.coachUsername,
                entityUuid = request.entityUuid,
                entityType = request.entityType,
                logDate = request.logDate
            )
        )

        return CoachReplyQueueActionResponse(1, "Dismissed")
    }

    fun undismiss(coachUsername: String, entityUuid: String): CoachReplyQueueActionResponse {
        val removed = dismissalRepository.deleteByCoachUsernameAndEntityUuid(coachUsername, entityUuid)
        return CoachReplyQueueActionResponse(removed, if (removed > 0) "Restored" else "Nothing to restore")
    }

    /**
     * Clears the backlog: dismisses every item currently outstanding before [beforeDate]. Works by
     * running the real queue for that window, so it only ever dismisses things the coach would
     * actually have seen -- never a log that was already answered or doesn't qualify.
     */
    fun dismissBeforeDate(request: DismissReplyQueueBeforeDateRequest): CoachReplyQueueActionResponse {
        val windowEnd = Date.valueOf(request.beforeDate.toLocalDate().minusDays(1))
        // Wide enough to cover any realistic backlog within a season.
        val windowStart = Date.valueOf(request.beforeDate.toLocalDate().minusYears(1))

        val outstanding = getReplyQueue(
            coachUsername = request.coachUsername,
            season = request.season,
            startDate = windowStart,
            endDate = windowEnd,
            team = request.team
        ).items

        val dismissals = outstanding.map {
            CoachReplyDismissal(
                coachUsername = request.coachUsername,
                entityUuid = it.entityUuid,
                entityType = it.type,
                logDate = it.date
            )
        }

        dismissalRepository.saveAll(dismissals)

        return CoachReplyQueueActionResponse(
            dismissals.size,
            "Cleared " + dismissals.size + " item(s) before " + request.beforeDate
        )
    }
}
