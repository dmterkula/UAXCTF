package com.terkula.uaxctf.statistics.service.track

import com.fasterxml.jackson.databind.ObjectMapper
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformance
import com.terkula.uaxctf.statisitcs.model.track.TrackMeetPerformanceDTO
import com.terkula.uaxctf.statistics.repository.*
import com.terkula.uaxctf.statistics.repository.track.TrackMeetPerformanceRepository
import com.terkula.uaxctf.statistics.repository.track.TrackMeetRepository
import com.terkula.uaxctf.statistics.request.track.CreateTrackMeetResultRequest
import com.terkula.uaxctf.util.getYearString
import org.springframework.stereotype.Component

@Component
class TrackMeetPerformanceService(
    private val meetRepository: TrackMeetRepository,
    private val trackMeetPerformanceRepository: TrackMeetPerformanceRepository,
    private val runnerRepository: RunnerRepository
) {

//
//    fun getResultsForMeet(meetId: String): List<TrackMeetPerformanceDTO> {
//
//        val meets = meetRepository.findAll().map {it.id to it}.toMap()
//
//
//        return trackMeetPerformanceRepository.findByMeetId(meetId)
//                .map {
//                    TrackMeetPerformanceDTO(meets[it.id]!!.name, meets[it.id]!!.date, it.event, it.place, it.time)
//                }
//    }
//
//    fun getMeetPerformancesForRunner(
//            runnerId: Int,
//            startDate: Date,
//            endDate: Date,
//            sortingMethodContainer: SortingMethodContainer,
//            count: Int,
//            adjustForDistance: Boolean): List<RunnerPerformanceDTO> {
//        // find runners matching partial name
//        val runner = runnerRepository.findById(runnerId).get()
//
//        // get meets within date range
//        val meets = meetRepository.findByDateBetween(startDate, endDate)
//
//        // construct map for meet id to meet
//        val meetIdToMeetInfo = meets.map { it.id to it }.toMap()
//
//        // construct all performances for the meets only for meets in date range, and containing an id of a matching runner
//        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)
//                .filter { record -> record.runnerId == runnerId }
//        }.flatten()
//
//        // construct map for runner id to runner over the selected performances
//        val runners = performances.map {
//            it.runnerId to runnerRepository.findById(it.runnerId).get()
//        }.toMap()
//
//        // group performances by id.
//        // map runnerId to a MeetDTO constructed from performances and meet info map
//        val runnerPerformanceDTOs = performances.groupBy { it.runnerId }
//                .map {
//                    runners[it.key]!! to sortingMethodContainer.sortingFunction(performanceAdjusterService.adjustMeetPerformances(it.value.map { meetPerformance ->
//                        val meet = meetIdToMeetInfo[meetPerformance.meetId]!!
//                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place, null, meetPerformance.passesSecondMile, meetPerformance.passesLastMile, meetPerformance.skullsEarned)
//                    }.toMutableList(), adjustForDistance).take(count).toMutableList())
//                }.map {
//                    RunnerPerformanceDTO(it.first, it.second)
//                }
//
//        return runnerPerformanceDTOs
//
//    }

//    fun getMeetPerformancesForRunner(runnerId: Int,
//                                     startDate: Date,
//                                     endDate: Date): List<XCMeetPerformance> {
//
//        return meetRepository.findByDateBetween(startDate, endDate).mapNotNull {
//            meetPerformanceRepository.findByMeetIdAndRunnerId(it.id, runnerId)
//        }
//    }
//
//    fun getFirstPlacePerformancesForRunner(runnerId: Int): List<XCMeetPerformance> {
//        return meetPerformanceRepository.findByRunnerIdAndPlace(runnerId, 1)
//    }
//
//    fun getPerformancesForRunner(runnerId: Int): List<XCMeetPerformance> {
//        return meetPerformanceRepository.findByRunnerId(runnerId)
//    }

//    fun getMeetPerformancesAtMeetName(partialName: String,
//                                      startDate: Date,
//                                      endDate: Date,
//                                      sortingMethodContainer: SortingMethodContainer,
//                                      count: Int,
//                                      adjustForDistance: Boolean): List<RunnerPerformanceDTO> {
//        // find runners matching partial name
//
//        val meets = meetRepository.findByNameAndDateBetween(partialName, startDate, endDate)
//
//        // construct map for meet id to meet
//        val meetMap = meets.map { it.id to it }.toMap()
//
//        // construct all performances for the meets only for meets in date range, and containing an id of a matching runner
//        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)}.flatten()
//
//        // construct map for runner id to runner over the selected performances
//        val runners = performances.map {
//            it.runnerId to runnerRepository.findById(it.runnerId).get()
//        }.toMap()
//
//        // group performances by id.
//        // map runnerId to a MeetDTO constructed from performances and meet info map
//        val runnerPerformanceDTOs = performances.groupBy { it.runnerId }
//                .map {
//                    runners[it.key]!! to sortingMethodContainer.sortingFunction(performanceAdjusterService.adjustMeetPerformances(it.value.map { meetPerformance ->
//                        val meet = meetMap[meetPerformance.meetId]!!
//                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place, null, meetPerformance.passesSecondMile, meetPerformance.passesLastMile, meetPerformance.skullsEarned)
//                    }.toMutableList(), adjustForDistance).take(count).toMutableList())
//                }.map {
//                    RunnerPerformanceDTO(it.first, it.second)
//                }.sortedBy { it.performance.first().time.calculateSecondsFrom() }
//
//        return runnerPerformanceDTOs
//    }


    fun getTrackMeetResults(meetUUID: String): List<TrackMeetPerformanceDTO> {

        val meet = meetRepository.findByUuid(meetUUID)
        if (!meet.isPresent) {
            return emptyList()
        } else {

            val runners = runnerRepository.findByGraduatingClassGreaterThanEqual((meet.get().date.getYearString())).map { it.id to it }.toMap()
            return trackMeetPerformanceRepository.findByMeetId(meetUUID)
                    .groupBy { it.runnerId }
                   .map { TrackMeetPerformanceDTO(
                           meet.get(), runners[it.key]!!, it.value
                   ) }
        }
    }

    fun createTrackMeetResult(createTrackMeetResultRequest: CreateTrackMeetResultRequest): TrackMeetPerformanceDTO? {

        val objectMapper = ObjectMapper()

        val meets = meetRepository.findByUuid(createTrackMeetResultRequest.meetId)

        if (!meets.isPresent) {
            return null
        }

        val runner = runnerRepository.findById(createTrackMeetResultRequest.runnerId).get()

        val meet = meets.get()

        val performances = trackMeetPerformanceRepository.findByUuidAndRunnerId(meet.uuid, runner.id)

        if (performances.isEmpty()) {

           val createdResults: MutableList<TrackMeetPerformance> = mutableListOf()
            createTrackMeetResultRequest.results.forEach {
                val splitsString = objectMapper.writeValueAsString(it.splits)
                val newPerformance = TrackMeetPerformance(meet.uuid, it.uuid, runner.id, it.time, it.place, it.event, it.isSplit, splitsString)
                trackMeetPerformanceRepository.save(newPerformance)
                createdResults.add(newPerformance)
            }

            return TrackMeetPerformanceDTO(meet, runner, createdResults)

        } else {
            val createdResults: MutableList<TrackMeetPerformance> = mutableListOf()
            createTrackMeetResultRequest.results.map {
                performances.firstOrNull { perf -> perf.event == it.event } to it
            }.forEach {
                var splitsString = objectMapper.writeValueAsString(it.second.splits)
                if (it.first != null) {
                    it.first!!.place = it.second.place
                    it.first!!.time = it.second.time
                    it.first!!.event = it.second.event
                    it.first!!.isSplit = it.second.isSplit
                    it.first!!.splits = splitsString
                    trackMeetPerformanceRepository.save(it.first!!)
                    createdResults.add(it.first!!)
                } else {
                    // existing performance doesn't exist for this event, create it
                    val newResult = TrackMeetPerformance(meet.uuid, it.second.uuid, runner.id, it.second.time,
                            it.second.place, it.second.event, it.second.isSplit, splitsString)
                    trackMeetPerformanceRepository.save(newResult)
                    createdResults.add(newResult)
                }
            }

            return TrackMeetPerformanceDTO(meet, runner, createdResults)

        }

    }


//    private fun transformMeetPairToPerformancePair(
//            runner: Runner,
//            meetPair: Pair<Meet, Meet>,
//            adjustForDistance: Boolean
//    ): Pair<Double?, Double?>  {
//
//        var meet1Performance = meetPerformanceRepository.findByMeetIdAndRunnerId(meetPair.first.id, runner.id)
//                ?.toMeetPerformanceDTO(meetPair.first)
//
//
//        var meet2Performance = meetPerformanceRepository.findByMeetIdAndRunnerId(meetPair.second.id, runner.id)
//                ?.toMeetPerformanceDTO(meetPair.second)
//
//        if (meet1Performance != null) {
//            meet1Performance = performanceAdjusterService.adjustMeetPerformances(listOf(meet1Performance), adjustForDistance).first()
//        }
//
//        if (meet2Performance != null) {
//            meet2Performance = performanceAdjusterService.adjustMeetPerformances(listOf(meet2Performance), adjustForDistance).first()
//        }
//
//        return meet1Performance?.time?.calculateSecondsFrom() to meet2Performance?.time?.calculateSecondsFrom()
//    }



}

