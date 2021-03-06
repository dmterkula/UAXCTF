package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Meet
import com.terkula.uaxctf.statisitcs.model.toMeetPerformanceDTO
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.exception.GoalNotFoundException
import com.terkula.uaxctf.statistics.exception.MeetNotFoundException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.XcGoalRepository
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.substractDays
import com.terkula.uaxctf.util.toMinuteSecondString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date
import kotlin.math.truncate

@Component
class XcGoalService (@field:Autowired
                     private val runnerRepository: RunnerRepository,
                     @field:Autowired
                     private val xcGoalRepository: XcGoalRepository,
                     @field: Autowired
                     internal var meetRepository: MeetRepository,
                     @field: Autowired
                     internal var meetPerformanceService: MeetPerformanceService,
                     @field: Autowired
                     internal var seasonBestService: SeasonBestService) {


    fun getGoalsForSeason(season: String): List<RunnerGoalDTO> {

        val goals = xcGoalRepository.findBySeason(season)

        val runnerMap = goals.map {
            val runner = runnerRepository.findById(it.runnerId)
            runner.get().id to runner
        }.toMap()

        return goals.map { runnerMap[it.runnerId]!!.get() to it.time }
                .groupBy { it.first }
                .map {
                    it.key to it.value.map { pair -> pair.second }
                }
                .map {
                    RunnerGoalDTO(it.first, it.second.map{time ->
                        time.calculateSecondsFrom()
                    }
                            .toMutableList()
                            .sorted()
                            .map {time-> time.toMinuteSecondString()})
                }
    }

    fun getRunnersGoalForSeason(name: String, season: String): List<RunnerGoalDTO> {

        val runner = runnerRepository.findByNameContaining(name).firstOrNull()
                ?: throw RunnerNotFoundByPartialNameException("No runner matching the given name of '$name'")

        val goals = xcGoalRepository.findByRunnerId(runner.id)
                .filter { it.season == season }

        if (goals == null) {
            throw GoalNotFoundException("No goal found for $name")
        } else {
            return listOf(RunnerGoalDTO(runner, goals
                    .map { it.time.calculateSecondsFrom() }
                    .toMutableList()
                    .sorted()
                    .map { it.toMinuteSecondString()}))
        }

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

        // get goals for those who ran
        val goals = xcGoalRepository.findBySeason(MeetPerformanceController.CURRENT_YEAR).map { it.runnerId to it }.toMap()
                .filter { it.key in lastMeetPerformances.keys }

        // get the seasonBests before the last meet for each runner
        val justBeforeLastMeetDate = substractDays(targetMeet.date, 1)
        val seasonBestsBeforeLastMeet = seasonBestService.getAllSeasonBests(startSeasonDate, justBeforeLastMeetDate, adjustForDistance)
                .map { it.runner.id to it }.toMap()

        // get goals which have been previously un-met before the last meet
        val unmetGoalsBeforeLastMeet = goals.filter { it.key in seasonBestsBeforeLastMeet.keys }
                .filter { it.value.time < seasonBestsBeforeLastMeet[it.key]!!.seasonBest.first().time }

        // of the unmet goals, find those who met their goal and return
        return unmetGoalsBeforeLastMeet.filter {
            truncate(lastMeetPerformances[it.key]!!.performance.first().time.calculateSecondsFrom()) <= truncate(it.value.time.calculateSecondsFrom())
        }
                .map {
                    RunnersMetGoals(lastMeetPerformances[it.key]!!.runner, listOf(MetGoalPerformanceDTO(it.value.time, lastMeetPerformances[it.key]!!.performance.first())))
                }

    }

    fun getRunnersWhoHaveMetGoal(startSeasonDate: Date, endSeasonDate: Date, adjustForDistance: Boolean): List<RunnersMetGoals> {

        val seasonBests = seasonBestService.getAllSeasonBests(startSeasonDate, endSeasonDate, adjustForDistance)
                .map { it.runner.id to it }.toMap()

        val goalsMap = xcGoalRepository.findBySeason(MeetPerformanceController.CURRENT_YEAR)
                .filter { it.runnerId in seasonBests.keys }
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
                        goal -> seasonBests[goal.runnerId]!!.seasonBest.first().time.calculateSecondsFrom() <= goal.time.calculateSecondsFrom()
                    }
                }.toMap()
                .filter { it.value.isNotEmpty() }

       // map each met goal to the first race at which the goal was met

        return runnerToMetGoals.map {
            it.key to it.value.map {
                goal -> goal to meetPerformances[goal.runnerId]!!.first { meetResult -> meetResult.time.calculateSecondsFrom() <= goal.time.calculateSecondsFrom()}
            } .toMap()
        }.toMap().map {
            RunnersMetGoals(seasonBests[it.key]!!.runner,  it.value.map { pair -> MetGoalPerformanceDTO(pair.key.time, listOf(pair.value).toMeetPerformanceDTO(meets).first()) })
        }




//        return seasonBests
//                .filter { goals[it.key] != null }
//                .filter {
//            truncate(it.value.seasonBest.first().time.calculateSecondsFrom()) <= truncate(goals[it.key]!!.time.calculateSecondsFrom())
//        }.map {
//            MetGoalDTO(it.value.runner, goals[it.key]!!.time, it.value.seasonBest.first())
//        }.toMutableList()

    }

    fun getRunnersWhoHaveNotMetGoal(startSeasonDate: Date, endSeasonDate: Date, adjustForDistance: Boolean): List<UnMetGoalDTO> {

        val seasonBests = seasonBestService.getAllSeasonBests(startSeasonDate, endSeasonDate, adjustForDistance)
                .map { it.runner.id to it }.toMap()
        val goals = xcGoalRepository.findBySeason(MeetPerformanceController.CURRENT_YEAR)
                .filter { it.runnerId in seasonBests.keys }
                .map { it.runnerId to it }.toMap()

        return seasonBests
                .filter { goals[it.key] != null }
                .filter {
            truncate(it.value.seasonBest.first().time.calculateSecondsFrom()) > truncate(goals[it.key]!!.time.calculateSecondsFrom())
        }.map {
            UnMetGoalDTO(it.value.runner, goals[it.key]!!.time, it.value.seasonBest.first(),
                    (it.value.seasonBest.first().time.calculateSecondsFrom() - goals[it.key]!!.time.calculateSecondsFrom()).toMinuteSecondString())
        }.toMutableList().sortedBy { it.difference }

    }

}