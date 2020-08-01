package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.RunnerPerformanceDTO
import com.terkula.uaxctf.statistics.exception.MultipleMeetsFoundException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.*
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.training.model.WorkoutSplit
import com.terkula.uaxctf.training.repository.RawWorkoutRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.repository.WorkoutSplitRepository
import com.terkula.uaxctf.util.convertHourMileSplitToMinuteSecond
import java.sql.Date
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.geo.Distance
import org.springframework.stereotype.Component

@Component
class MeetPerformanceService(@field:Autowired
                             private val meetRepository: MeetRepository,
                             @field:Autowired
                             private val meetInfoRepository: MeetInfoRepository,
                             @field:Autowired
                             private val meetPerformanceRepository: MeetPerformanceRepository,
                             @field:Autowired
                             private val runnerRepository: RunnerRepository, @field:Autowired
                             private val raceResultRepository: RaceResultRepository,
                             @field:Autowired
                             private val rawMileSplitRepository: RawMileSplitRepository,
                             @field:Autowired
                             private val mileSplitRepository: MeetMileSplitRepository,
                             @field:Autowired
                             private val workoutRepository: WorkoutRepository,
                             @field:Autowired
                             private val workoutSplitRepository: WorkoutSplitRepository,
                             @field:Autowired
                             private val rawWorkoutRepository: RawWorkoutRepository,
                             @field:Autowired
                             private val rawTimeTrialRepository: RawTimeTrialRepository,
                             @field:Autowired
                             private val timeTrialRepository: TimeTrialRepository,
                             @field: Autowired
                             private val performanceAdjusterService: PerformanceAdjusterService) {

    fun loadMeetPerformance(meetId: Int) {
        val runners = runnerRepository.findAll().asSequence().toList()
        val raceResults = raceResultRepository.findAll().asSequence().toList()

        val xcMeetPerformances = raceResults.map { result ->
            val runnerList = runners.filter { runner: Runner -> runner.name.equals(result.name, ignoreCase = true) }
            if (runnerList.isEmpty()) {
                println("runner name in performance does not matcher roster: " + result.name)
            }

            return@map XCMeetPerformance(runnerList[0].id, meetId, result.time, result.place)
        }

        meetPerformanceRepository.saveAll(xcMeetPerformances)

    }

    fun loadMileSplits(meetId: Int) {
        val runners = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR)
        val rawMileSplits = rawMileSplitRepository.findAll().asSequence().toList()

        val mileSplits = rawMileSplits.map { split ->
            val runnerList = runners.filter { runner: Runner -> runner.name.equals(split.name, ignoreCase = true) }
            if (runnerList.isEmpty()) {
                println("runner name in mile split table does not matcher roster: " + split.name)
            }

            return@map MeetMileSplit(meetId, runnerList[0].id, split.mileOne, split.mileTwo, split.mileThree)
        }

        mileSplitRepository.saveAll(mileSplits)

    }


    fun loadWorkout(workoutId: Int) {

        val workout = workoutRepository.findById(workoutId).get()

        val runners = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR)

        val rawSplits = rawWorkoutRepository.findAll().asSequence().toList()


        val workoutSplits = rawSplits.map { record ->
            val runnerList = runners.filter { runner: Runner -> runner.name.equals(record.name, ignoreCase = true) }
            if (runnerList.isEmpty()) {
                println("runner name in raw workout split table does not matcher roster: " + record.name)
                throw RunnerNotFoundByPartialNameException("runner name in raw workout split table does not matcher roster:" + record.name)
            }
            return@map WorkoutSplit(workoutId, runnerList[0].id, workout.targetDistance, record.time.convertHourMileSplitToMinuteSecond(), record.splitNumber)
        }

        workoutSplitRepository.saveAll(workoutSplits)

    }

    fun loadTimeTrial(season: String) {

        val rawTimeTrialResults = rawTimeTrialRepository.findAll().asSequence().toList()
        val runners = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR)

        val timeTrialResults = rawTimeTrialResults.map {record ->
            val runnerList = runners.filter { runner: Runner -> runner.name.equals(record.name, ignoreCase = true) }
            if (runnerList.isEmpty()) {
                println("runner name in raw workout split table does not matcher roster: " + record.name)
                throw RunnerNotFoundByPartialNameException("runner name in raw workout split table does not matcher roster:" + record.name)
            }
            return@map TimeTrial(runnerList[0].id, record.time, record.place, season)
        }

        timeTrialRepository.saveAll(timeTrialResults)

    }


    fun cleanSplits(meetId: Int) {

        var toClean = if (meetId == 0) {
            mileSplitRepository.findAll().asSequence().toList()
        } else {
            mileSplitRepository.findByMeetId(meetId).asSequence().toList()
        }

        var cleanedMileSplits = mutableListOf<MeetMileSplit>()

        for(mileSplit in toClean) {
            cleanedMileSplits.add(MeetMileSplit(mileSplit.meetId, mileSplit.runnerId, mileSplit.mileOne.convertHourMileSplitToMinuteSecond(),
                    mileSplit.mileTwo.convertHourMileSplitToMinuteSecond(), mileSplit.mileThree.convertHourMileSplitToMinuteSecond()))
        }

        mileSplitRepository.saveAll(cleanedMileSplits)

    }

    fun postMeetInfoEntry(meetName: String,
                          startDate: Date,
                          endDate: Date,
                          distance: Int,
                          elevationChange: Int,
                          humidity: Double,
                          isRainy: Boolean,
                          isSnowy: Boolean,
                          temperature: Int,
                          windSpeed: Int,
                          cloudCoverRatio: Double) {

        val meets = meetRepository.findByNameAndDateBetween(meetName, startDate, endDate)

        if (meets.isEmpty()) {
            throw MultipleMeetsFoundException("multiple meets found in the requested season, please specify unique meet name in the season")
        }

        val meetInfo = MeetInfo(meets.first().id, distance, elevationChange, temperature, humidity, windSpeed, cloudCoverRatio, isRainy, isSnowy)

        meetInfoRepository.save(meetInfo)



    }


    fun getMeetPerformancesForRunnerWithNameContaining(
            partialName: String,
            startDate: Date,
            endDate: Date,
            sortingMethodContainer: SortingMethodContainer,
            count: Int,
            adjustForDistance: Boolean): List<RunnerPerformanceDTO> {
        // find runners matching partial name
        val runnerIds = runnerRepository.findByNameContaining(partialName).map{ it.id }

        // get meets within date range
        val meets =  meetRepository.findByDateBetween(startDate, endDate)

        // construct map for meet id to meet
        val meetIdToMeetInfo = meets.map { it.id to it }.toMap()

        // construct all performances for the meets only for meets in date range, and containing an id of a matching runner
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)
                .filter { record -> runnerIds.contains(record.runnerId) }
        }.flatten()

        // construct map for runner id to runner over the selected performances
        val runners = performances.map {
            it.runnerId to runnerRepository.findById(it.runnerId).get()
        }.toMap()

        // group performances by id.
        // map runnerId to a MeetDTO constructed from performances and meet info map
        val runnerPerformanceDTOs = performances.groupBy { it.runnerId }
                .map {
                    runners[it.key]!! to performanceAdjusterService.adjustMeetPerformances(sortingMethodContainer.sortingFunction(it.value.map { meetPerformance ->
                        val meet = meetIdToMeetInfo[meetPerformance.meetId]!!
                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place, null)
                    }.toMutableList()).take(count), adjustForDistance)
                }.map {
                    RunnerPerformanceDTO(it.first, it.second)
                }

        return runnerPerformanceDTOs

    }

    fun getMeetPerformancesAtMeetName(partialName: String,
                                      startDate: Date,
                                      endDate: Date,
                                      sortingMethodContainer: SortingMethodContainer,
                                      count: Int,
                                      adjustForDistance: Boolean): List<RunnerPerformanceDTO> {
        // find runners matching partial name

        val meets =  meetRepository.findByNameAndDateBetween(partialName, startDate, endDate)

        // construct map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()

        // get meets within date range

        // construct all performances for the meets only for meets in date range, and containing an id of a matching runner
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)}.flatten()

        // construct map for runner id to runner over the selected performances
        val runners = performances.map {
            it.runnerId to runnerRepository.findById(it.runnerId).get()
        }.toMap()

        // group performances by id.
        // map runnerId to a MeetDTO constructed from performances and meet info map
        val runnerPerformanceDTOs = performances.groupBy { it.runnerId }
                .map {
                    runners[it.key]!! to performanceAdjusterService.adjustMeetPerformances(sortingMethodContainer.sortingFunction(it.value.map { meetPerformance ->
                        val meet = meetMap[meetPerformance.meetId]!!
                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place, null)
                    }.toMutableList()).take(count), adjustForDistance)
                }.map {
                    RunnerPerformanceDTO(it.first, it.second)
                }

        return runnerPerformanceDTOs

    }

}

