package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statisitcs.model.XcGoal
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.dto.track.TrackPerformancesDTO
import com.terkula.uaxctf.statistics.exception.GoalNotFoundException
import com.terkula.uaxctf.statistics.exception.MeetNotFoundException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.XcGoalRepository
import com.terkula.uaxctf.statistics.request.GoalsRequest
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.statistics.request.UpdateGoalRequest
import com.terkula.uaxctf.statistics.service.track.TrackSBService
import com.terkula.uaxctf.util.TimeUtilities.Companion.getFirstDayOfGivenYear
import com.terkula.uaxctf.util.TimeUtilities.Companion.getLastDayOfGivenYear
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.getYearString
import com.terkula.uaxctf.util.substractDays
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date
import kotlin.math.truncate

@Component
class XcGoalService (
    val runnerRepository: RunnerRepository,
    val xcGoalRepository: XcGoalRepository,
    var meetRepository: MeetRepository,
    var meetPerformanceService: MeetPerformanceService,
    var seasonBestService: SeasonBestService,
    var trackSBService: TrackSBService
) {


    fun getGoalsForSeason(season: String, xcOnly: Boolean?, trackOnly: Boolean?): List<RunnerGoalDTO> {

        val goals: List<XcGoal> = if (xcOnly == null && trackOnly == null) {
            xcGoalRepository.findBySeason(season)
        } else if (xcOnly == null && trackOnly != null) {
            xcGoalRepository.findBySeasonAndTrackGoal(season, trackOnly)
        } else if (xcOnly != null && trackOnly == null) {
            xcGoalRepository.findBySeasonAndTrackGoal(season, !xcOnly)
        } else {
            // if both provided just get XC goals
            xcGoalRepository.findBySeasonAndTrackGoal(season, !xcOnly!!)
        }

        var runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val runnerMap = goals.map {
            val runner = runnerRepository.findById(it.runnerId)
            runner.get().id to runner
        }.toMap()

        return goals.map { runnerMap[it.runnerId]!!.get() to it }
                .groupBy { it.first }
                .map {
                    it.key to it.value.map { pair -> pair.second }
                }
                .map {
                    RunnerGoalDTO(it.first, it.second.map{ goal ->

                        if (goal.type.equals("Time", ignoreCase = true) && goal.event != null) {

                            if (goal.event == "5000m" || goal.event == "5000") {
                                var sb = seasonBestService.getSeasonBestsByName(runners[it.first.id]!!.name, listOf(Pair(getFirstDayOfGivenYear(season), getLastDayOfGivenYear(season))), false)

                                var goalMet = false

                                if (sb.isNotEmpty() && sb.first().seasonBest.isNotEmpty() && goal.value.calculateSecondsFrom() <= sb.first().seasonBest.first().time.calculateSecondsFrom()) {
                                    goalMet = true
                                }

                                return@map XcGoal(goal.runnerId, goal.season, goal.type, goal.value.calculateSecondsFrom().toMinuteSecondString(), goalMet, goal.trackGoal, goal.event)
                            } else {
                                var sb = trackSBService.getARunnersSBs(it.first.id, false, goal.event!!, season, false)

                                var goalMet = false

                                if (sb.bestResults.isNotEmpty() && goal.value.calculateSecondsFrom() <= sb.bestResults.first().best.time.calculateSecondsFrom() ) {
                                    goalMet = true
                                }

                                return@map XcGoal(goal.runnerId, goal.season, goal.type, goal.value.calculateSecondsFrom().toMinuteSecondString(), goalMet, goal.trackGoal, goal.event)
                            }


                        } else {
                            return@map goal
                        }
                    })
                }

    }

    fun getEventSpecificGoalsForSeason(season: String, event: String): List<RunnerGoalDTO> {

        val goals: List<XcGoal> = xcGoalRepository.findBySeasonAndEvent(season, event)

        var runners = runnerRepository.findAll().map { it.id to it }.toMap()

        val runnerMap = goals.map {
            val runner = runnerRepository.findById(it.runnerId)
            runner.get().id to runner
        }.toMap()

        return goals.map { runnerMap[it.runnerId]!!.get() to it }
                .groupBy { it.first }
                .map {
                    it.key to it.value.map { pair -> pair.second }
                }
                .map {
                    RunnerGoalDTO(it.first, it.second.map{ goal ->

                        if (goal.type.equals("Time", ignoreCase = true)) {

                            // todo, this needs to get SB from track if event is not 5000m

                            var sb = seasonBestService.getSeasonBestsByName(runners[it.first.id]!!.name, listOf(Pair(getFirstDayOfGivenYear(season), getLastDayOfGivenYear(season))), false)

                            var goalMet = false

                            if (sb.isNotEmpty() && sb.first().seasonBest.isNotEmpty() && goal.value.calculateSecondsFrom() <= sb.first().seasonBest.first().time.calculateSecondsFrom()) {
                                goalMet = true
                            }

                            return@map XcGoal(goal.runnerId, goal.season, goal.type, goal.value.calculateSecondsFrom().toMinuteSecondString(), goalMet, goal.trackGoal, goal.event)
                        } else {
                            return@map goal
                        }
                    })
                }

    }

    fun getRunnersGoalForSeason(name: String, season: String): RunnerGoalDTO {


        val runner = runnerRepository.findByNameContaining(name).firstOrNull()
                ?: throw RunnerNotFoundByPartialNameException("No runner matching the given name of '$name'")

        val sb = seasonBestService.getSeasonBestsByName(runner.name, listOf(Pair(getFirstDayOfGivenYear(season), getLastDayOfGivenYear(season))), false)

        val goals = xcGoalRepository.findByRunnerId(runner.id)
                .filter { it.season == season }.map {
            if (it.type.equals("Time", true)) {
                if (sb.isNotEmpty() && sb.first().seasonBest.isNotEmpty() && sb.first().seasonBest.first().time.calculateSecondsFrom() <= it.value.calculateSecondsFrom()) {
                    return@map XcGoal(it.id, it.season, it.type, it.value, true, it.trackGoal, it.event)
                }
                else {
                    return@map it
                }
            } else {
                return@map it
            }
        }


        return RunnerGoalDTO(runner, goals)

    }

    fun getRunnersGoalForYearAndSeason(runnerId: Int, year: String, season: String): RunnerGoalDTO {

        if (season.equals("xc", ignoreCase = true)) {
            val runner = runnerRepository.findById(runnerId).get()

            val sb = seasonBestService.getSeasonBestsByName(runner.name, listOf(Pair(getFirstDayOfGivenYear(year), getLastDayOfGivenYear(year))), false)

            val goals = xcGoalRepository.findByRunnerId(runner.id)
                    .filter { it.season == year }.map {
                        if (it.type.equals("Time", true)) {
                            if (sb.isNotEmpty() && sb.first().seasonBest.isNotEmpty() && sb.first().seasonBest.first().time.calculateSecondsFrom() <= it.value.calculateSecondsFrom()) {
                                return@map XcGoal(it.id, it.season, it.type, it.value, true, it.trackGoal, it.event)
                            }
                            else {
                                return@map it
                            }
                        } else {
                            return@map it
                        }
                    }


            return RunnerGoalDTO(runner, goals)
        } else {
            val runner = runnerRepository.findById(runnerId).get()

            val seasonBests: TrackPerformancesDTO = trackSBService.getARunnersSBs(runner.id, true, "", year, false)
            val goals = xcGoalRepository.findByRunnerId(runnerId).filter { it.season == year && it.trackGoal == true }
                    .map {
                        if (it.type.equals("Time", true)) {
                            if (seasonBests.bestResults.isNotEmpty() && seasonBests.bestResults.filter { sb -> sb.event == it.event}.isNotEmpty() && seasonBests.bestResults.filter { sb -> sb.event == it.event}.first().best.time.calculateSecondsFrom() <= it.value.calculateSecondsFrom()) {
                                return@map XcGoal(it.id, it.season, it.type, it.value, true, it.trackGoal, it.event)
                            }
                            else {
                                return@map it
                            }
                        } else {
                            return@map it
                        }
                    }
            return RunnerGoalDTO(runner, goals)

        }

    }


    fun createRunnersGoalsForSeason(runnerId: Int, season: String, createGoalsRequest: GoalsRequest): RunnerGoalDTO {

        val runner = runnerRepository.findById(runnerId).get()

        val goals = createGoalsRequest.goals.map {
            XcGoal(runner.id, season, it.type, it.value, it.isMet, it.trackGoal, it.event)
        }

        goals.forEach {

            val existingGoal = xcGoalRepository.findByRunnerIdAndSeasonAndTypeAndValue(runner.id, season, it.type, it.value)

            if (existingGoal.isEmpty()) {
                xcGoalRepository.save(XcGoal(runner.id, season, it.type, it.value, it.isMet, it.trackGoal, it.event))
            } else {
                // update
                existingGoal[0].season = it.season
                existingGoal[0].type = it.type
                existingGoal[0].value = it.value
                existingGoal[0].isMet = it.isMet
                existingGoal[0].trackGoal = it.trackGoal
                existingGoal[0].event = it.event
                xcGoalRepository.save(existingGoal[0])
            }
        }

        return RunnerGoalDTO(runner, goals)

    }

    fun deleteRunnersGoals(runnerId: Int, season: String, createGoalsRequest: GoalsRequest): RunnerGoalDTO {

        val runner = runnerRepository.findById(runnerId).get()

        var deletedGoals = mutableListOf<XcGoal>()

        createGoalsRequest.goals.forEach {

            val existingGoal = xcGoalRepository.findByRunnerIdAndSeasonAndTypeAndValue(runner.id, season, it.type, it.value)

            if (existingGoal.isNotEmpty()) {
                xcGoalRepository.delete(existingGoal[0])
                deletedGoals.add(existingGoal[0])
            }
        }

        return RunnerGoalDTO(runner, deletedGoals)

    }

    fun updateRunnerGoalForSeason(name: String, season: String, updateGoalRequest: UpdateGoalRequest): RunnerGoalDTO {

        val runner = runnerRepository.findByNameContaining(name).firstOrNull()
                ?: throw RunnerNotFoundByPartialNameException("No runner matching the given name of '$name'")

        val existingGoal = xcGoalRepository.findByRunnerIdAndSeasonAndTypeAndValue(runner.id, season, updateGoalRequest.existingGoal.type, updateGoalRequest.existingGoal.value)

            // update
            existingGoal[0].season = season
            existingGoal[0].type = updateGoalRequest.updatedGoal.type
            existingGoal[0].value = updateGoalRequest.updatedGoal.value
            existingGoal[0].isMet = updateGoalRequest.updatedGoal.isMet
            xcGoalRepository.save(existingGoal[0])

        return RunnerGoalDTO(runner, existingGoal)

    }

    fun getRunnerWhoNewlyMetGoalAtMeet(
            meetName: String,
            startSeasonDate: Date,
            endSeasonDate: Date,
            adjustForDistance: Boolean
    ): List<RunnersMetGoals> {

        //get target meet and all other meets this season
        val targetMeet: Meet
        val meets: List<Meet>
        try {
            meets = meetRepository.findByDateBetween(startSeasonDate, endSeasonDate).toMutableList().sortedBy { it.date }
            try {
                targetMeet = meets.filter { it.name == meetName }.first()
            } catch (e: Exception) {
                throw MeetNotFoundException("could not find a meet matching the given name: $meetName")
            }

        } catch (e: Exception) {
            throw MeetNotFoundException("unable to find meet by name: $meetName")
        }

        // get map runnerId to last meet performances
        val lastMeetPerformances = meetPerformanceService.getMeetPerformancesAtMeetName(meetName,
                startSeasonDate, endSeasonDate, SortingMethodContainer.TIME, 99, adjustForDistance).map { it.runner.id to it }.toMap()

        // get goals for those who ran in the last meet to their runnerId
        val goals: Map<Int, List<XcGoal>> = xcGoalRepository.findBySeason(MeetPerformanceController.CURRENT_YEAR)
                .filter { it.type.equals("Time", ignoreCase = true)}
                .groupBy { it.runnerId }.toMap()
                .filter { it.key in lastMeetPerformances.keys }

        // get the seasonBests before the last meet for each runner
        val justBeforeLastMeetDate = substractDays(targetMeet.date, 1)
        val seasonBestsBeforeLastMeet = seasonBestService.getAllSeasonBests(startSeasonDate, justBeforeLastMeetDate, adjustForDistance)
                .map { it.runner.id to it }.toMap()

        // get goals which have been previously un-met before the last meet
        val unmetGoalsBeforeLastMeet: Map<Int, List<XcGoal>> = goals.filter { it.key in seasonBestsBeforeLastMeet.keys }
                .map {
                    it.key to it.value.filter { goal -> goal.value < seasonBestsBeforeLastMeet[it.key]!!.seasonBest.first().time  }
                }
                .filter {
                    it.second.isNotEmpty()
                }.toMap()

        // of the unmet goals, find those who met their goal and return


        var runnerMetGoals: MutableList<RunnersMetGoals> = unmetGoalsBeforeLastMeet.map {
           it.key to it.value.filter { goal ->
                truncate(lastMeetPerformances[it.key]!!.performance.first().time.calculateSecondsFrom()) <= truncate(goal.value.calculateSecondsFrom())
            }
        }.map {
            RunnersMetGoals(
                    lastMeetPerformances[it.first]!!.runner,
                    it.second.map { goal -> MetGoalPerformanceDTO(goal.value, lastMeetPerformances[it.first]!!.performance.first()) }
            )
            }
                .filter {
                    it.metGoals.isNotEmpty()
                }.toMutableList()

        return runnerMetGoals
    }

    fun getRunnersWhoHaveMetGoal(startSeasonDate: Date, endSeasonDate: Date, adjustForDistance: Boolean): List<RunnersMetGoals> {

        val seasonBests = seasonBestService.getAllSeasonBests(startSeasonDate, endSeasonDate, adjustForDistance)
                .map { it.runner.id to it }.toMap()

        val goalsMap = xcGoalRepository.findBySeason(startSeasonDate.getYearString())
                .filter { it.runnerId in seasonBests.keys }
                .filter { it.type.equals("time", ignoreCase = true)}
                .groupBy { it.runnerId }.toMap()

        val meets = meetRepository.findByDateBetween(startSeasonDate, endSeasonDate).map { it.id to it }.toMap()

        val meetPerformances = goalsMap.map {
            meetPerformanceService.getMeetPerformancesForRunner(it.key, startSeasonDate, endSeasonDate)
        }.flatten().groupBy { it.runnerId }.toMap().map {
            it.key to it.value.toMutableList().sortedBy { meet-> meets[meet.meetId]!!.date }
        }.toMap()


        // map of runner id to list of met goals
        val runnerToMetGoals = goalsMap.filter { it.key in seasonBests.keys }
                .map {
                    it.key to
                    it.value.filter {
                        // filter for runner who has met goals
                        goal -> seasonBests[goal.runnerId]!!.seasonBest.first().time.calculateSecondsFrom() <= goal.value.calculateSecondsFrom()
                    }
                }.toMap()
                .filter { it.value.isNotEmpty() }

       // map each met goal to the first race at which the goal was met

        return runnerToMetGoals.map {
            it.key to it.value.map {
                goal -> goal to meetPerformances[goal.runnerId]!!.first { meetResult -> meetResult.time.calculateSecondsFrom() <= goal.value.calculateSecondsFrom()}
            } .toMap()
        }.toMap().map {
            RunnersMetGoals(seasonBests[it.key]!!.runner,  it.value.map { pair -> MetGoalPerformanceDTO(pair.key.value, listOf(pair.value).toMeetPerformanceDTO(meets).first()) })
        }
    }

    fun getRunnersWhoHaveNotMetGoal(startSeasonDate: Date, endSeasonDate: Date, adjustForDistance: Boolean): List<UnMetGoalDTO> {

        val seasonBests = seasonBestService.getAllSeasonBests(startSeasonDate, endSeasonDate, adjustForDistance)
                .map { it.runner.id to it }.toMap()
        val goals: List<XcGoal> = xcGoalRepository.findBySeason(startSeasonDate.getYearString())
                .filter { it.runnerId in seasonBests.keys }
                .filter { it.type.equals("Time", ignoreCase = true) }

        return  goals.filter {
            seasonBests[it.runnerId]!!.seasonBest.first().time.calculateSecondsFrom() > it.value.calculateSecondsFrom()
        }.map {
            UnMetGoalDTO(seasonBests[it.runnerId]!!.runner, it.value, seasonBests[it.runnerId]!!.seasonBest.first(),
                    (seasonBests[it.runnerId]!!.seasonBest.first().time.calculateSecondsFrom() - it.value.calculateSecondsFrom()).toMinuteSecondString())
        }.toMutableList().sortedBy { it.difference }


//        return seasonBests
//                .filter { goals[it.key] != null }
//                .filter {
//            truncate(it.value.seasonBest.first().time.calculateSecondsFrom()) > truncate(goals[it.key]!!.value.calculateSecondsFrom())
//        }.map {
//            UnMetGoalDTO(it.value.runner, goals[it.key]!!.value, it.value.seasonBest.first(),
//                    (it.value.seasonBest.first().time.calculateSecondsFrom() - goals[it.key]!!.value.calculateSecondsFrom()).toMinuteSecondString())
//        }.toMutableList().sortedBy { it.difference }

    }

}