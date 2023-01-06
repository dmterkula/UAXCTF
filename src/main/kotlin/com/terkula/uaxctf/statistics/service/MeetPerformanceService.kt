package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.*
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.dto.streak.StreakDTO
import com.terkula.uaxctf.statistics.exception.MeetNotFoundException
import com.terkula.uaxctf.statistics.exception.MultipleMeetsFoundException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.*
import com.terkula.uaxctf.statistics.request.CreateMeetResultRequest
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.training.repository.RawWorkoutRepository
import com.terkula.uaxctf.training.repository.WorkoutRepository
import com.terkula.uaxctf.training.repository.WorkoutSplitRepository
import com.terkula.uaxctf.util.*
import java.sql.Date
import org.springframework.beans.factory.annotation.Autowired
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

            return@map XCMeetPerformance(runnerList[0].id, meetId, result.time, result.place, 0, 0, 0)
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

//        val workout = workoutRepository.findById(workoutId).get()
//
//        val runners = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR)
//
//        val rawSplits = rawWorkoutRepository.findAll().asSequence().toList()
//
//
//        val workoutSplits = rawSplits.map { record ->
//            val runnerList = runners.filter { runner: Runner -> runner.name.equals(record.name, ignoreCase = true) }
//            if (runnerList.isEmpty()) {
//                println("runner name in raw workout split table does not matcher roster: " + record.name)
//                throw RunnerNotFoundByPartialNameException("runner name in raw workout split table does not matcher roster:" + record.name)
//            }
//            return@map WorkoutSplit(workoutId, runnerList[0].id, workout.targetDistance, record.time.convertHourMileSplitToMinuteSecond(), record.splitNumber)
//        }
//
//        workoutSplitRepository.saveAll(workoutSplits)

    }

    fun loadTimeTrial(season: String) {

        val rawTimeTrialResults = rawTimeTrialRepository.findAll().asSequence().toList()
        val runners = runnerRepository.findByGraduatingClassGreaterThan(MeetPerformanceController.CURRENT_YEAR)

        val timeTrialResults = rawTimeTrialResults.map {record ->
            val runnerList = runners.filter { runner: Runner -> runner.name.equals(record.name, ignoreCase = true) }
            if (runnerList.isEmpty()) {
                println("runner name in raw workout split table does not matcher roster: " + record.name)
                throw RunnerNotFoundByPartialNameException("runner name in raw time trial table does not matcher roster:" + record.name)
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

        val meets = meetRepository.findByNameContainsAndDateBetween(meetName, startDate, endDate)

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
                    runners[it.key]!! to sortingMethodContainer.sortingFunction(performanceAdjusterService.adjustMeetPerformances(it.value.map { meetPerformance ->
                        val meet = meetIdToMeetInfo[meetPerformance.meetId]!!
                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place, null, meetPerformance.passesSecondMile, meetPerformance.passesLastMile, meetPerformance.skullsEarned)
                    }.toMutableList(), adjustForDistance).take(count).toMutableList())
                }.map {
                    RunnerPerformanceDTO(it.first, it.second)
                }

        return runnerPerformanceDTOs

    }

    fun getResultsForMeet(meetId: Int): List<XCMeetPerformance> {
        return meetPerformanceRepository.findByMeetId(meetId)
    }

    fun getMeetPerformancesForRunner(
            runnerId: Int,
            startDate: Date,
            endDate: Date,
            sortingMethodContainer: SortingMethodContainer,
            count: Int,
            adjustForDistance: Boolean): List<RunnerPerformanceDTO> {
        // find runners matching partial name
        val runner = runnerRepository.findById(runnerId).get()

        // get meets within date range
        val meets =  meetRepository.findByDateBetween(startDate, endDate)

        // construct map for meet id to meet
        val meetIdToMeetInfo = meets.map { it.id to it }.toMap()

        // construct all performances for the meets only for meets in date range, and containing an id of a matching runner
        val performances = meets.map { meetPerformanceRepository.findByMeetId(it.id)
                .filter { record -> record.runnerId == runnerId }
        }.flatten()

        // construct map for runner id to runner over the selected performances
        val runners = performances.map {
            it.runnerId to runnerRepository.findById(it.runnerId).get()
        }.toMap()

        // group performances by id.
        // map runnerId to a MeetDTO constructed from performances and meet info map
        val runnerPerformanceDTOs = performances.groupBy { it.runnerId }
                .map {
                    runners[it.key]!! to sortingMethodContainer.sortingFunction(performanceAdjusterService.adjustMeetPerformances(it.value.map { meetPerformance ->
                        val meet = meetIdToMeetInfo[meetPerformance.meetId]!!
                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place, null, meetPerformance.passesSecondMile, meetPerformance.passesLastMile, meetPerformance.skullsEarned)
                    }.toMutableList(), adjustForDistance).take(count).toMutableList())
                }.map {
                    RunnerPerformanceDTO(it.first, it.second)
                }

        return runnerPerformanceDTOs

    }

    fun getMeetPerformancesForRunner(runnerId: Int,
                                     startDate: Date,
                                     endDate: Date): List<XCMeetPerformance> {

        return meetRepository.findByDateBetween(startDate, endDate).mapNotNull {
            meetPerformanceRepository.findByMeetIdAndRunnerId(it.id, runnerId)
        }
    }

    fun getFirstPlacePerformancesForRunner(runnerId: Int): List<XCMeetPerformance> {
        return meetPerformanceRepository.findByRunnerIdAndPlace(runnerId, 1)
    }

    fun getPerformancesForRunner(runnerId: Int): List<XCMeetPerformance> {
        return meetPerformanceRepository.findByRunnerId(runnerId)
    }

    fun getMeetPerformancesAtMeetName(partialName: String,
                                      startDate: Date,
                                      endDate: Date,
                                      sortingMethodContainer: SortingMethodContainer,
                                      count: Int,
                                      adjustForDistance: Boolean): List<RunnerPerformanceDTO> {
        // find runners matching partial name

        val meets = meetRepository.findByNameAndDateBetween(partialName, startDate, endDate)

        // construct map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()

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
                    runners[it.key]!! to sortingMethodContainer.sortingFunction(performanceAdjusterService.adjustMeetPerformances(it.value.map { meetPerformance ->
                        val meet = meetMap[meetPerformance.meetId]!!
                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place, null, meetPerformance.passesSecondMile, meetPerformance.passesLastMile, meetPerformance.skullsEarned)
                    }.toMutableList(), adjustForDistance).take(count).toMutableList())
                }.map {
                    RunnerPerformanceDTO(it.first, it.second)
                }.sortedBy { it.performance.first().time.calculateSecondsFrom() }

        return runnerPerformanceDTOs
    }

    fun getTotalMeetPerformancesAtMeet(meetName: String,
                                       startDate: Date,
                                       endDate: Date,
                                       sortingMethodContainer: SortingMethodContainer,
                                       count: Int,
                                       adjustForDistance: Boolean): List<RunnerMeetPerformanceDTO> {
        // find meet matching partial name

        val meets = meetRepository.findByNameAndDateBetween(meetName, startDate, endDate)

        // construct map for meet id to meet
        val meetMap = meets.map { it.id to it }.toMap()

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
                    runners[it.key]!! to sortingMethodContainer.sortingFunction(performanceAdjusterService.adjustMeetPerformances(it.value.map { meetPerformance ->
                        val meet = meetMap[meetPerformance.meetId]!!
                        MeetPerformanceDTO(meet.name, meet.date, meetPerformance.time, meetPerformance.place, null, meetPerformance.passesSecondMile, meetPerformance.passesLastMile, meetPerformance.skullsEarned)
                    }.toMutableList(), adjustForDistance).take(count).toMutableList())
                }.map {
                    RunnerPerformanceDTO(it.first, it.second)
                }.sortedBy { it.performance.first().time.calculateSecondsFrom() }
                .filter {
                    it.performance.isNotEmpty()
                }
                .map {
                    it.runner to it.performance.first()
                }
                .map {
                    var splits = mileSplitRepository.findByMeetIdAndRunnerId(meets.first { meet-> meet.name == it.second.meetName }.id, it.first.id)

                    if (splits.isNotEmpty()) {
                        return@map RunnerMeetPerformanceDTO(it.first, TotalMeetPerformanceDTO(it.second.meetName, it.second.meetDate, it.second.time, it.second.place,
                        splits.first().mileOne, splits.first().mileTwo, splits.first().mileThree, it.second.passesLastMile, it.second.passesSecondMile, it.second.skullsEarned))
                    } else {
                        return@map RunnerMeetPerformanceDTO(it.first, TotalMeetPerformanceDTO(it.second.meetName, it.second.meetDate, it.second.time, it.second.place,
                                "00:00", "00:00", "00:00", it.second.passesLastMile, it.second.passesSecondMile, it.second.skullsEarned))
                    }

                }

        return runnerPerformanceDTOs
    }

    fun createMeetResult(createMeetResultRequest: CreateMeetResultRequest): List<RunnerMeetPerformanceDTO> {

        var startDate = Date.valueOf("${createMeetResultRequest.season}-01-01")
        var endDate = Date.valueOf("${createMeetResultRequest.season}-12-31")

        val meets = meetRepository.findByNameAndDateBetween(createMeetResultRequest.meetName, startDate, endDate)

        if (meets.isEmpty()) {
            return emptyList()
        }

        var runner = runnerRepository.findById(createMeetResultRequest.runnerId).get()

        var meet = meets.first()

        var performance = meetPerformanceRepository.findByMeetIdAndRunnerId(meet.id, runner.id)

        if (performance == null) {

           var newPerformance = XCMeetPerformance(runner.id, meet.id, createMeetResultRequest.time, createMeetResultRequest.place, createMeetResultRequest.passesSecondMile,
                   createMeetResultRequest.passesLastMile, createMeetResultRequest.skullsEarned)
            performance = newPerformance
            meetPerformanceRepository.save(newPerformance)
        } else {
            performance.time = createMeetResultRequest.time
            performance.place = createMeetResultRequest.place
            performance.passesSecondMile = createMeetResultRequest.passesSecondMile
            performance.passesLastMile = createMeetResultRequest.passesLastMile
            performance.skullsEarned = createMeetResultRequest.skullsEarned
            meetPerformanceRepository.save(performance)
        }

        if (createMeetResultRequest.mileOneSplit == "00:00" && createMeetResultRequest.mileTwoSplit == "00:00" && createMeetResultRequest.mileThreeSplit == "00:00") {
           // if no real split info, don't insert splits

            return listOf(RunnerMeetPerformanceDTO(runner,
                    TotalMeetPerformanceDTO(meet.name, meet.date, performance.time, performance.place, createMeetResultRequest.mileOneSplit, createMeetResultRequest.mileTwoSplit,
                            createMeetResultRequest.mileThreeSplit, createMeetResultRequest.passesLastMile, createMeetResultRequest.passesSecondMile, createMeetResultRequest.skullsEarned)))
        }

        var splits = mileSplitRepository.findByMeetIdAndRunnerId(meet.id, createMeetResultRequest.runnerId)

        if (splits.isEmpty()) {
            var newSplit = MeetMileSplit(meet.id, runner.id, createMeetResultRequest.mileOneSplit, createMeetResultRequest.mileTwoSplit, createMeetResultRequest.mileThreeSplit)
            splits = listOf(newSplit)
            mileSplitRepository.save(newSplit)
        } else {
            var existingSplit = splits.first()
            existingSplit.mileOne = createMeetResultRequest.mileOneSplit
            existingSplit.mileTwo = createMeetResultRequest.mileTwoSplit
            existingSplit.mileThree = createMeetResultRequest.mileThreeSplit
            splits = listOf(existingSplit)
        }

        return listOf(RunnerMeetPerformanceDTO(runner,
                TotalMeetPerformanceDTO(meet.name, meet.date, performance.time, performance.place,
                        splits.first().mileOne, splits.first().mileTwo, splits.first().mileThree)))

    }



    fun compareTwoMeetsHistorically(
            meetName1: String,
            meetName2: String,
            startDate: Date,
            endDate: Date,
            excludeSeasons: List<String>,
            includeSeasons: List<String>,
            adjustForDistance: Boolean): StatisticalComparisonDTO {

        var meets = meetRepository.findByDateBetween(startDate, endDate)
                .filter { it.name.equals(meetName1, true) || it.name.equals(meetName2, true) }
                .filter { it.date.getYearString() !in excludeSeasons }

        if (includeSeasons.isNotEmpty()) {
            meets =  meets.filter { it.date.getYearString() in includeSeasons }
        }

        if (meets.groupBy { it.name }.size != 2) {
            throw MeetNotFoundException ("at least one of the provided meet names was not found with the included year range")
        }

        if (meets[0].name != meetName1) {
            meets = meets.reversed()
        }

        val runners = runnerRepository.findAll()

        val pairedMeets = meets.groupBy { it.date.getYearString() }
                .filter { it.value.size == 2 } // make sure both meets were ran each year
                .map {
                    it.value[0] to it.value[1]
                }

        val averageDifferenceBetweenTimesForRunners = runners.map {
            pairedMeets.map { meetPair-> transformMeetPairToPerformancePair(it, meetPair, adjustForDistance) }
        }.map {
            it.filter { pair-> pair.first != null && pair.second != null }
        }.filter {
            it.isNotEmpty()
        }.map {
            it.map { meetPair -> meetPair.second!! - meetPair.first!! }.average()
        }

        return StatisticalComparisonDTO.from(
                "comparing $meetName1 to $meetName2",
                averageDifferenceBetweenTimesForRunners,
                "time",
                2
        )
    }

    private fun transformMeetPairToPerformancePair(
            runner: Runner,
            meetPair: Pair<Meet, Meet>,
            adjustForDistance: Boolean
    ): Pair<Double?, Double?>  {

        var meet1Performance = meetPerformanceRepository.findByMeetIdAndRunnerId(meetPair.first.id, runner.id)
                ?.toMeetPerformanceDTO(meetPair.first)


        var meet2Performance = meetPerformanceRepository.findByMeetIdAndRunnerId(meetPair.second.id, runner.id)
                ?.toMeetPerformanceDTO(meetPair.second)

        if (meet1Performance != null) {
            meet1Performance = performanceAdjusterService.adjustMeetPerformances(listOf(meet1Performance), adjustForDistance).first()
        }

        if (meet2Performance != null) {
            meet2Performance = performanceAdjusterService.adjustMeetPerformances(listOf(meet2Performance), adjustForDistance).first()
        }

        return meet1Performance?.time?.calculateSecondsFrom() to meet2Performance?.time?.calculateSecondsFrom()
    }

    fun getTotalPassesLastMileForRunner(runnerId: Int): Int {
        return meetPerformanceRepository.findByRunnerId(runnerId).sumOf { it.passesLastMile }
    }

    fun getSkullsEarnedStreak(runnerId: Int): StreakDTO {

        var longestStreak = 0
        var currentStreak = 0

        meetPerformanceRepository.findByRunnerId(runnerId)
                .forEach {
                    if (it.passesLastMile > 0) {
                        currentStreak += 1
                        if (currentStreak > longestStreak) {
                            longestStreak = currentStreak
                        }
                    } else {
                        currentStreak = 0
                    }

                }
        return StreakDTO(currentStreak, longestStreak)

    }

    fun getSkullStreakForRaces(runner: Runner, races: List<MeetPerformanceDTO>, active: Boolean): StreakDTO {

        var longestStreak = 0
        var currentStreak = 0


        races.forEach {
            if (it.passesLastMile > 0) {
                currentStreak += 1
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak
                }
            } else {
                currentStreak = 0
            }
        }

        if (active) {
            currentStreak = 0
            var streakActive = true
            for (i in races.size - 1 downTo 0) {
//
                if (races[i].passesLastMile > 0) {
                    if (streakActive) {
                        currentStreak ++
                    }
                } else {
                    streakActive = false
                }
            }
        }


        return StreakDTO(currentStreak, longestStreak)

    }

    fun getSkullsEarnedTotal(runnerId: Int): Int {
        return meetPerformanceRepository.findByRunnerId(runnerId).sumOf { it.skullsEarned }
    }

    fun getAllRaces(): List<RunnerPerformanceDTO> {

        var runners = runnerRepository.findAll().map { it.id to it }.toMap()

        var meets = meetRepository.findAll()
        var meetMap = meets.map { it.id to it }.toMap()

        var races = meetPerformanceRepository.findAll().map {
            RunnerPerformanceDTO(runners[it.runnerId]!!, listOf(it.toMeetPerformanceDTO(meetMap[it.meetId]!!)))
        }


        return races

    }

    fun getRacesInSeason(season: String): List<RunnerPerformanceDTO> {

        var runners = runnerRepository.findAll().map { it.id to it }.toMap()

        var meets = meetRepository.findByDateBetween(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))
        var meetMap = meets.map { it.id to it }.toMap()

        val races = meets.map {
            meetPerformanceRepository.findByMeetId(it.id)
                    .map { result -> RunnerPerformanceDTO(runners[result.runnerId]!!, listOf(result.toMeetPerformanceDTO(meetMap[result.meetId]!!))) }
        }
                .flatten()


        return races

    }

}

