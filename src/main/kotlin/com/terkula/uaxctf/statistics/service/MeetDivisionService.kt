package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.MeetDivision
import com.terkula.uaxctf.statistics.repository.MeetDivisionRepository
import org.springframework.stereotype.Service

@Service
class MeetDivisionService(val meetDivisionRepository: MeetDivisionRepository) {

    fun getDivisionsForMeet(meetId: Int): List<MeetDivision> =
        meetDivisionRepository.findByMeetIdOrderBySequence(meetId)

    fun createDivision(meetId: Int, name: String, startTime: java.sql.Time?, distanceMeters: Int?): MeetDivision {
        val existing = getDivisionsForMeet(meetId)
        val nextSequence = (existing.maxOfOrNull { it.sequence } ?: 0) + 1
        return meetDivisionRepository.save(MeetDivision(meetId, name, nextSequence, startTime, distanceMeters))
    }

    fun updateDivision(id: Int, name: String?, startTime: java.sql.Time?, distanceMeters: Int?): MeetDivision? {
        val division = meetDivisionRepository.findById(id).orElse(null) ?: return null
        name?.let { division.name = it }
        division.startTime = startTime
        division.distanceMeters = distanceMeters
        return meetDivisionRepository.save(division)
    }

    /** Reassigns sequence 1..n in the given order — this is the drag-to-reorder entry point. */
    fun reorderDivisions(meetId: Int, orderedDivisionIds: List<Int>): List<MeetDivision> {
        val divisionsById = getDivisionsForMeet(meetId).associateBy { it.id }
        val reordered = orderedDivisionIds.mapIndexedNotNull { index, id ->
            divisionsById[id]?.also { it.sequence = index + 1 }
        }
        meetDivisionRepository.saveAll(reordered)
        return getDivisionsForMeet(meetId)
    }

    fun deleteDivision(id: Int) = meetDivisionRepository.deleteById(id)

    /**
     * Resolves which division a result belongs to, preferring (in order):
     * 1. An explicit manual override (the app's division picker).
     * 2. A start-time match against pre-configured divisions — the most reliable signal, since
     *    race *position* on a timing site's events page counts every race at the meet (including
     *    other genders/schools a team doesn't run), while a coach's own divisions are usually
     *    only added for the races their team actually runs, so raw sequence numbers often don't
     *    line up between the two.
     * 3. A race-order (sequence) match, for meets/scrapers that don't have a start time.
     * 4. A name match.
     * If nothing matches and a label was given (e.g. from the scraper), creates a new division on
     * the fly — using the scraped start time too, when available — so meets with no upfront setup
     * still get divisions from scraped results.
     */
    fun resolveDivisionId(
        meetId: Int,
        explicitDivisionId: Int?,
        raceStartTime: java.time.LocalTime?,
        sequence: Int?,
        label: String?
    ): Int? {
        if (explicitDivisionId != null) return explicitDivisionId
        if (raceStartTime == null && sequence == null && label == null) return null

        val existing = getDivisionsForMeet(meetId)

        if (raceStartTime != null) {
            existing
                .mapNotNull { division -> division.startTime?.toLocalTime()?.let { division to it } }
                .map { (division, startTime) -> division to Math.abs(java.time.Duration.between(startTime, raceStartTime).toMinutes()) }
                .filter { (_, diffMinutes) -> diffMinutes <= TIME_MATCH_TOLERANCE_MINUTES }
                .minByOrNull { (_, diffMinutes) -> diffMinutes }
                ?.let { (division, _) -> return division.id }
        }
        if (sequence != null) {
            existing.firstOrNull { it.sequence == sequence }?.let { return it.id }
        }
        if (label != null) {
            existing.firstOrNull { it.name.equals(label, ignoreCase = true) }?.let { return it.id }
        }

        val name = label ?: "Division ${sequence ?: existing.size + 1}"
        val newSequence = sequence ?: ((existing.maxOfOrNull { it.sequence } ?: 0) + 1)
        val newStartTime = raceStartTime?.let { java.sql.Time.valueOf(it.withSecond(0)) }
        return meetDivisionRepository.save(MeetDivision(meetId, name, newSequence, newStartTime)).id
    }

    companion object {
        private const val TIME_MATCH_TOLERANCE_MINUTES = 90L
    }
}
