package com.terkula.uaxctf.statistics.service.track

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO
//import com.terkula.uaxctf.statisitcs.model.track.toTrackMeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.track.TrackSBDTO
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class TrackSeasonBestService(@field:Autowired
                        private val meetRepository: TrackMeetRepository, @field:Autowired
                        private val meetPerformanceRepository: TrackMeetPerformanceRepository,
                             @field:Autowired
                        private val runnerRepository: RunnerRepository) {

//    fun getAllSeasonBests(startDate: Date, endDate: Date): List<TrackSBDTO> {
//
//        // get all meets in season/date range
//        val meets = meetRepository.findByDateBetween(startDate, endDate)
//
//        // look up map for meet id to meet
//        val meetMap = meets.map { it.id to it }.toMap()
//
//        // give all performances for the meets
//        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id) }.flatten()
//
//        // look up map for runner id to runner
//        val runners = performances.map {
//            it.runnerId to runnerRepository.findById(it.runnerId).get()
//        }.toMap()
//
//        return performances.groupBy { it.runnerId }
//                .map { runners[it.key]!! to (it.value.toMutableList().sortedBy { performance -> performance.time }.take(2)) }.toMutableList()
//                .map { it.first to it.second.toTrackMeetPerformanceDTO(meetMap) }.toMutableList()
//                .groupAndSort(1)
//                .map {
//                    TrackSBDTO(it.first, it.second)
//                }
//
//    }

//    fun getSeasonBestsByName(partialName: String, startEndDates: List<Pair<Date, Date>>): List<TrackSBDTO?> {
//
//        val runnersSeasonBests: MutableList<TrackSBDTO?> = mutableListOf()
//
//
//        for (startEndDate: Pair<Date, Date> in startEndDates) {
//            val meets = meetRepository.findByDateBetween(startEndDate.first, startEndDate.second)
//
//            // look up map for meet id to meet
//            val meetMap = meets.map { it.id to it }.toMap()
//
//            // give all performances for the meets
//            val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id) }.flatten()
//
//            // look up map for runner id to runner
//
//            lateinit var runners: Map<Int, Runner>
//
//            try {
//                runners = listOf(runnerRepository.findByNameContaining(partialName).first()).map { it.id to it }.toMap()
//            } catch (e: Exception) {
//                throw RunnerNotFoundByPartialNameException("Unable to find results for runner by: $partialName")
//            }
//
//            val seasonBests = runners.map { runner -> performances.filter { it.runnerId == runner.key } }
//                    .flatten()
//                    .groupBy { it.runnerId }
//                    .map {
//                        runners[it.key]!! to it.value.toMutableList()
//                    }.toMap()
//                    .map {
//                        it.key to it.value.toTrackMeetPerformanceDTO(meetMap)
//                    }
//                    .groupAndSort(1)
//                    .map {
//                        TrackSBDTO(it.first, it.second)
//                    }
//
//            runnersSeasonBests.add(seasonBests.firstOrNull())
//
//        }
//
//        return runnersSeasonBests
//
//    }

//    fun getSeasonBestsAtLastMeet(startDate: Date, endDate: Date): List<TrackSBDTO> {
//
//        val latestMeet = meetRepository.findByDateBetween(startDate, endDate).sortedByDescending { it.date }.first()
//
//        return getAllSeasonBests(startDate, endDate)
//                .filter {
//                    it.seasonBests.values.flatten().any { sb -> sb.meetDate == latestMeet.date }
//                }
//    }
//
//    fun findWhoseSeasonBestIsFirstMeet(startDate: Date, endDate: Date): List<TrackSBDTO> {
//
//        // get all meets in season/date range
//        val meets = meetRepository.findByDateBetween(startDate, endDate).sortedBy { it.date }
//
//        return getAllSeasonBests(startDate, endDate).filter {
//            it.seasonBests.values.flatten().any { sb -> sb.meetDate == meets.first().date }
//        }
//
//    }
//}
//
//fun List<Pair<Runner, List<TrackMeetPerformanceDTO>>>.groupAndSort(take: Int): List<Pair<Runner, Map<String, List<TrackMeetPerformanceDTO>>>> {
//
//    return this.map {
//        it.first to it.second.groupBy { performance -> performance.event }
//    }.map {
//                it.first to it.second.map { entry -> mapOf(entry.key to entry.value.toMutableList().sortedBy { perf -> perf.time }.take(take)) }
//                        .fold(mutableMapOf<String, List<TrackMeetPerformanceDTO>>(), { acc, map -> acc.plus(map).toMutableMap() })
//            }
}
