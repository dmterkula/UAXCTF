package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.training.request.ComponentCreationElement
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Server-side guard for workout components, mirroring what the iOS workout form always sends
 * (WorkoutComponentFormViewModel / WorkoutFormView). Requests built by other clients (scripts, older
 * builds) can omit fields the app force-unwraps; e.g. an Active recovery with a null recoveryDistance
 * crashed the workout plan screen. So:
 *   - missing form-defaulted fields are filled with the form's defaults (never stored as null), and
 *   - values the form could never produce are rejected with a 400 and a readable message.
 * `descriptionOnly` components are API-only/legacy: the app renders them with null fields, so only
 * minimal checks apply.
 */
object WorkoutComponentRules {

    const val DESCRIPTION_ONLY = "descriptionOnly"
    const val MAX_DESCRIPTION = 255          // workout_component.description is varchar(255)
    const val MAX_PACE_ADJUSTMENT = 18       // workout_component.target_pace_adjustment is varchar(18)
    val TARGET_EVENTS = setOf(400, 800, 1600, 3200, 5000)
    val RECOVERY_TYPES = setOf("Standing", "Active")
    val PERCENT_RANGE = 50..120
    private val TIME = Regex("^\\d{1,2}:\\d{2}(\\.\\d+)?$")

    /** Fill form defaults in place, then validate. Throws 400 listing every problem found. */
    fun normalizeAndValidate(components: List<ComponentCreationElement>) {
        val errors = mutableListOf<String>()
        if (components.isEmpty()) errors.add("workout needs at least one component")
        components.forEachIndexed { i, c ->
            normalize(c)
            validate(c).forEach { errors.add("component ${i + 1}: $it") }
        }
        if (errors.isNotEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, errors.joinToString("; "))
        }
    }

    fun normalize(c: ComponentCreationElement) {
        if (c.type == DESCRIPTION_ONLY) return
        if (c.duration == null) c.duration = "00:00"
        if (c.recovery == null) c.recovery = "00:00"
        if (c.setRecovery == null) c.setRecovery = "00:00"
        if (c.recoveryType == null) c.recoveryType = "Standing"
        if (c.recoveryDistance == null) c.recoveryDistance = 0   // app force-unwraps this for Active recovery
        if (c.percent == null) c.percent = 100
        if (c.sets == null) c.sets = 1
    }

    fun validate(c: ComponentCreationElement): List<String> {
        val errors = mutableListOf<String>()
        if (c.uuid.isBlank()) errors.add("uuid is required")
        // Blank descriptions exist on ~20 legacy components; the form already blocks new ones, and rejecting here
        // would stop a coach from re-saving an old workout, so only the length limit is enforced.
        if (c.description.length > MAX_DESCRIPTION) errors.add("description is ${c.description.length} chars (max $MAX_DESCRIPTION)")
        if (c.targetPaceAdjustment.length > MAX_PACE_ADJUSTMENT) errors.add("targetPaceAdjustment is longer than $MAX_PACE_ADJUSTMENT chars")
        if (c.targetEvent !in TARGET_EVENTS) errors.add("targetEvent ${c.targetEvent} must be one of ${TARGET_EVENTS.sorted()}")
        if (c.type == DESCRIPTION_ONLY) return errors

        if (c.targetDistance <= 0) errors.add("targetDistance must be > 0 (the form requires it, even for timed reps)")
        if (c.targetCount < 0) errors.add("targetCount must be >= 0")
        for ((name, value) in listOf("duration" to c.duration, "recovery" to c.recovery, "setRecovery" to c.setRecovery)) {
            if (value != null && !TIME.matches(value)) errors.add("$name '$value' must look like mm:ss")
        }
        if (c.recoveryType !in RECOVERY_TYPES) errors.add("recoveryType '${c.recoveryType}' must be Standing or Active")
        if ((c.recoveryDistance ?: 0) < 0) errors.add("recoveryDistance must be >= 0")
        if (c.percent != null && c.percent !in PERCENT_RANGE) errors.add("percent ${c.percent} must be within $PERCENT_RANGE")
        return errors
    }
}
