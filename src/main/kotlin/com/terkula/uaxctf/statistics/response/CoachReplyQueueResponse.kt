package com.terkula.uaxctf.statistics.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.training.model.TrainingRun
import com.terkula.uaxctf.training.response.WorkoutResponseDTO
import com.terkula.uaxctf.training.response.crosstraining.CrossTrainingDTO
import com.terkula.uaxctf.training.response.lifting.LiftingActivityDTO
import java.sql.Date
import java.sql.Timestamp

/** The log kinds the reply queue surfaces. Mirrored by CoachReplyQueueItemType on iOS. */
object ReplyQueueType {
    const val TRAINING_RUN = "TRAINING_RUN"
    const val WORKOUT = "WORKOUT"
    const val CROSS_TRAINING = "CROSS_TRAINING"
    const val LIFTING = "LIFTING"
    const val PRE_MEET = "PRE_MEET"
    const val POST_MEET = "POST_MEET"
    const val TRACK_POST_MEET = "TRACK_POST_MEET"
}

object ReplyQueuePriority {
    /** Something is being asked of the coach, or the athlete flagged pain. Sorts to the top. */
    const val HIGH = "HIGH"
    const val NORMAL = "NORMAL"
}

/**
 * One athlete log awaiting a coach response.
 *
 * [entityUuid] doubles as the reply target: posting a TrainingComment with this as the
 * referenceUuid is exactly what the per-log detail screens already do, so the queue can reply
 * in place without reimplementing each log type's comment flow.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoachReplyQueueItem(
    val entityUuid: String,
    val type: String,
    val runner: Runner,
    val date: Date,
    /** Name of the underlying activity or meet, e.g. "Easy Run" or "Mason Invitational". */
    val title: String,
    /** What the athlete wrote, and the reason this is in the queue at all. */
    val athleteNote: String?,
    /** Pre-meet logs only: the athlete's explicit question, which is why those sort HIGH. */
    val athleteQuestion: String?,
    val effortLevel: Double?,
    val painLevel: Double?,
    val painNotes: String?,
    /** Athlete-authored comments on the log, which a coach has not yet responded to. */
    val athleteCommentCount: Int,
    /** Most recent athlete activity on this log, used for "waiting N days". */
    val lastAthleteActivity: Timestamp?,
    val daysWaiting: Int,
    /**
     * False for track post-meet logs, which have no comment thread -- only a coach_notes column.
     * The client hides inline reply for those and sends the coach to the log itself.
     */
    val canReplyInline: Boolean,
    val priority: String,
    /**
     * The parent activity this log belongs to. Exactly one is populated, matching [type].
     *
     * These are the raw entities on purpose: each has a matching iOS initializer
     * (TrainingRunEvent(trainingRun:), MeetEvent(meet:), and so on), so the client can open the
     * record's real detail screen without a second round trip or a parallel set of DTOs.
     */
    val trainingRun: TrainingRun? = null,
    val crossTraining: CrossTrainingDTO? = null,
    val liftingActivity: LiftingActivityDTO? = null,
    val workout: WorkoutResponseDTO? = null,
    val meet: Meet? = null
)

data class CoachReplyQueueResponse(
    val items: List<CoachReplyQueueItem>,
    val totalCount: Int,
    /** Counts keyed by [ReplyQueueType], for the filter chips' badges. */
    val countsByType: Map<String, Int>,
    val highPriorityCount: Int
)

data class CoachReplyQueueActionResponse(
    val dismissedCount: Int,
    val message: String
)
