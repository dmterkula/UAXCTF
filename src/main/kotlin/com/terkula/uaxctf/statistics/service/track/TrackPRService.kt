package com.terkula.uaxctf.statistics.service.track

import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import com.terkula.uaxctf.statisitcs.model.track.getLogicalEvent
import com.terkula.uaxctf.statistics.dto.track.TrackPRPerformance
import com.terkula.uaxctf.statistics.dto.track.TrackPRsDTO
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.toMinuteSecondMillisString
import org.springframework.stereotype.Component

@Component
class TrackPRService(
    private val meetRepository: TrackMeetRepository,
    private val meetPerformanceRepository: TrackMeetPerformanceRepository,
    private val runnerRepository: RunnerRepository
) {

    fun getARunnersPRs(runnerId: Int, includeSplits: Boolean, filterEvent: String): TrackPRsDTO {

        val meetMap = meetRepository.findAll().map { it.uuid to it }.toMap()
        val runner = runnerRepository.findById(runnerId)

        val results = if (filterEvent.isEmpty()) {
            val r = meetPerformanceRepository.findByRunnerId(runnerId)
            if (!includeSplits) {
                r.filter { !it.isSplit }.toMutableList()
            } else {
                r
            }
        } else {
            val r = meetPerformanceRepository.findByRunnerIdAndEvent(runnerId, filterEvent)
            if (!includeSplits) {
                r.filter { !it.isSplit }.toMutableList()
            } else {
                r
            }
        }

        val prs = results
                .groupBy { it.getLogicalEvent() }
                .map { Triple(it.key, it.value, it.value.sortedBy { perf-> perf.time.calculateSecondsFrom() }.firstOrNull())
        }.filter { it.third != null }
                .map {
                    val previousBest = findPreviousBest(it.third, it.second, meetMap)
                    var timeDifference = "0:00.00"
                    if (previousBest != null) {
                        timeDifference = (previousBest.time.calculateSecondsFrom() - it.third!!.time.calculateSecondsFrom()).toMinuteSecondMillisString()
                    }
                TrackPRPerformance(it.first, meetMap[it.third!!.meetId]!!, it.third!!, previousBest, timeDifference)
             }.sortedBy { it.event }

            return TrackPRsDTO(runner.get(), prs)
        }

    fun findPreviousBest(
        pr: TrackMeetPerformance?,
        records: List<TrackMeetPerformance>,
        meets: Map<String, TrackMeet>
    ): TrackMeetPerformance? {

        if (pr == null) {
            return null
        }

        return records.filter { meets[it.meetId]!!.date.before(meets[pr.meetId]!!.date) }.sortedBy { it.time.calculateSecondsFrom() }
                .firstOrNull()
    }


//    fun getAllPRs(startingGradClass: String, filterClass: String, sortingMethodContainer: TrackSortingMethodContainer): List<TrackPRsDTO> {
//
//        // find all runners whose graduating class is > current year | find by given grad class
//        val eligibleRunners = if (filterClass.isNotEmpty()) {
//            runnerRepository.findByGraduatingClass(filterClass)
//        } else {
//            runnerRepository.findByGraduatingClassGreaterThanEqual(startingGradClass)
//        }.map { it.id to it }.toMap()
//
//        // find all performances for those runners this year
//
//        val meets = meetRepository.findAll().toMutableList()
//
//        val meetMap = meets.map { it.id to it }.toMap()
//
//        return eligibleRunners.map { meetPerformanceRepository.findByRunnerId(it.key) }
//                .flatten().groupBy { it.runnerId }
//                .map {
//                    it.key to it.value.filter { perf -> perf.meetId in meetMap }
//                }.toMap()
//                .map { eligibleRunners[it.key]!! to it.value.toTrackMeetPerformanceDTO(meetMap) }
//                .filter { it.second.isNotEmpty() }
//                .groupAndSort(1)
//                .map {
//                    TrackPRsDTO(it.first, it.second)
//                }
//
//    }

//    fun getPRsByName(partialName: String): List<TrackPRsDTO?> {
//
//        val eligibleRunners = runnerRepository.findByNameContaining(partialName)
//                .map { it.id to it }.toMap()
//
//        val meets = meetRepository.findAll().toMutableList()
//
//        val meetMap = meets.map { it.id to it }.toMap()
//
//        val prDTOs = eligibleRunners.map { meetPerformanceRepository.findByRunnerId(it.key) }
//                .flatten().groupBy { it.runnerId }
//                .map {
//                    it.key to it.value.filter { perf -> perf.meetId in meetMap }
//                }.toMap()
//                .map { eligibleRunners[it.key]!! to it.value.toTrackMeetPerformanceDTO(meetMap) }
//                .filter { it.second.isNotEmpty() }
//                .groupAndSort(1)
//                .map {
//                    TrackPRsDTO(it.first, it.second)
//                }
//
//        return prDTOs
//
//    }

//    fun getPRsAtLastMeet(startDate: Date, endDate: Date): List<TrackPRsDTO> {
//
//        return getAllPRs(startDate.getYearString(), "", TrackSortingMethodContainer.TIME)
//                .map {
//                    val filteredMeetPerformances = it.pr.filter { mapEntry -> mapEntry.value.first().meetDate == endDate }
//                    TrackPRsDTO(it.runner, filteredMeetPerformances)
//                }.filter {
//                    it.pr.values.isNotEmpty()
//                }
//
//    }
}