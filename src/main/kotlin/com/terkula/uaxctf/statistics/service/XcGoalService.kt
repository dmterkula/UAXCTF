package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.XcGoal
import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.exception.GoalNotFoundException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.XcGoalRepository
import com.terkula.uaxctf.statistics.request.GoalsRequest
import com.terkula.uaxctf.statistics.request.UpdateGoalRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

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

//        val goals = xcGoalRepository.findBySeason(season)
//
//        val runnerMap = goals.map {
//            val runner = runnerRepository.findById(it.runnerId)
//            runner.get().id to runner
//        }.toMap()
//
//        return goals.map { runnerMap[it.runnerId]!!.get() to it.time }
//                .groupBy { it.first }
//                .map {
//                    it.key to it.value.map { pair -> pair.second }
//                }
//                .map {
//                    RunnerGoalDTO(it.first, it.second.map{time ->
//                        time.calculateSecondsFrom()
//                    }
//                            .toMutableList()
//                            .sorted()
//                            .map {time-> time.toMinuteSecondString()})
//                }

        return emptyList()

    }

    fun getRunnersGoalForSeason(name: String, season: String): RunnerGoalDTO {


        val runner = runnerRepository.findByNameContaining(name).firstOrNull()
                ?: throw RunnerNotFoundByPartialNameException("No runner matching the given name of '$name'")

        val goals = xcGoalRepository.findByRunnerId(runner.id)
                .filter { it.season == season }

        return RunnerGoalDTO(runner, goals)

    }

    fun createRunnersGoalsForSeason(name: String, season: String, createGoalsRequest: GoalsRequest): RunnerGoalDTO {

        val runner = runnerRepository.findByNameContaining(name).firstOrNull()
                ?: throw RunnerNotFoundByPartialNameException("No runner matching the given name of '$name'")

        val goals = createGoalsRequest.goals.map {
            XcGoal(runner.id, season, it.type, it.value, it.isMet)
        }

        goals.forEach {

            val existingGoal = xcGoalRepository.findByRunnerIdAndSeasonAndTypeAndValue(runner.id, season, it.type, it.value)

            if (existingGoal.isEmpty()) {
                xcGoalRepository.save(XcGoal(runner.id, season, it.type, it.value, it.isMet))
            } else {
                // update
                existingGoal[0].season = it.season
                existingGoal[0].type = it.type
                existingGoal[0].value = it.value
                existingGoal[0].isMet = it.isMet
                xcGoalRepository.save(existingGoal[0])
            }
        }

        return RunnerGoalDTO(runner, goals)

    }

    fun deleteRunnersGoals(name: String, season: String, createGoalsRequest: GoalsRequest): RunnerGoalDTO {

        val runner = runnerRepository.findByNameContaining(name).firstOrNull()
                ?: throw RunnerNotFoundByPartialNameException("No runner matching the given name of '$name'")

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

// todo

//        //get target meet and all other meets this season
//        val targetMeet: Meet
//        val meets: List<Meet>
//        try {
//            meets = meetRepository.findByDateBetween(startSeasonDate, endSeasonDate).toMutableList().sortedBy { it.date }
//            try {
//                targetMeet = meets.filter { it.name == meetName }.first()
//            } catch (e: Exception) {
//                throw MeetNotFoundException("could not find a meet matching the given name: $meetName")
//            }
//
//        } catch (e: Exception) {
//            throw MeetNotFoundException("unable to find meet by name: $meetName")
//        }
//
//        // get map runnerId to last meet performances
//        val lastMeetPerformances = meetPerformanceService.getMeetPerformancesAtMeetName(meetName,
//                startSeasonDate, endSeasonDate, SortingMethodContainer.TIME, 99, adjustForDistance).map { it.runner.id to it }.toMap()
//
//        // get goals for those who ran
//        val goals = xcGoalRepository.findBySeason(MeetPerformanceController.CURRENT_YEAR).map { it.runnerId to it }.toMap()
//                .filter { it.key in lastMeetPerformances.keys }
//
//        // get the seasonBests before the last meet for each runner
//        val justBeforeLastMeetDate = substractDays(targetMeet.date, 1)
//        val seasonBestsBeforeLastMeet = seasonBestService.getAllSeasonBests(startSeasonDate, justBeforeLastMeetDate, adjustForDistance)
//                .map { it.runner.id to it }.toMap()
//
//        // get goals which have been previously un-met before the last meet
//        val unmetGoalsBeforeLastMeet = goals.filter { it.key in seasonBestsBeforeLastMeet.keys }
//                .filter { it.value.time < seasonBestsBeforeLastMeet[it.key]!!.seasonBest.first().time }
//
//        // of the unmet goals, find those who met their goal and return
//        return unmetGoalsBeforeLastMeet.filter {
//            truncate(lastMeetPerformances[it.key]!!.performance.first().time.calculateSecondsFrom()) <= truncate(it.value.time.calculateSecondsFrom())
//        }
//                .map {
//                    RunnersMetGoals(lastMeetPerformances[it.key]!!.runner, listOf(MetGoalPerformanceDTO(it.value.time, lastMeetPerformances[it.key]!!.performance.first())))
//                }

        return emptyList()
    }

    fun getRunnersWhoHaveMetGoal(startSeasonDate: Date, endSeasonDate: Date, adjustForDistance: Boolean): List<RunnersMetGoals> {

        // todo

//        val seasonBests = seasonBestService.getAllSeasonBests(startSeasonDate, endSeasonDate, adjustForDistance)
//                .map { it.runner.id to it }.toMap()
//
//        val goalsMap = xcGoalRepository.findBySeason(MeetPerformanceController.CURRENT_YEAR)
//                .filter { it.runnerId in seasonBests.keys }
//                .groupBy { it.runnerId }.toMap()
//
//        val meets = meetRepository.findByDateBetween(startSeasonDate, endSeasonDate).map { it.id to it }.toMap()
//
//        val meetPerformances = goalsMap.map {
//            meetPerformanceService.getMeetPerformancesForRunner(it.key, startSeasonDate, endSeasonDate)
//        }.flatten().groupBy { it.runnerId }.toMap().map {
//            it.key to it.value.toMutableList().sortedBy { meet-> meets[meet.meetId]!!.date }
//        }.toMap()
//
//
//        // map of runner id to list of met goals
//        val runnerToMetGoals = goalsMap.filter { it.key in seasonBests.keys }
//                .map {
//                    it.key to
//                    it.value.filter {
//                        // filter for runner who has met goals
//                        goal -> seasonBests[goal.runnerId]!!.seasonBest.first().time.calculateSecondsFrom() <= goal.time.calculateSecondsFrom()
//                    }
//                }.toMap()
//                .filter { it.value.isNotEmpty() }
//
//       // map each met goal to the first race at which the goal was met
//
//        return runnerToMetGoals.map {
//            it.key to it.value.map {
//                goal -> goal to meetPerformances[goal.runnerId]!!.first { meetResult -> meetResult.time.calculateSecondsFrom() <= goal.time.calculateSecondsFrom()}
//            } .toMap()
//        }.toMap().map {
//            RunnersMetGoals(seasonBests[it.key]!!.runner,  it.value.map { pair -> MetGoalPerformanceDTO(pair.key.time, listOf(pair.value).toMeetPerformanceDTO(meets).first()) })
//        }




//        return seasonBests
//                .filter { goals[it.key] != null }
//                .filter {
//            truncate(it.value.seasonBest.first().time.calculateSecondsFrom()) <= truncate(goals[it.key]!!.time.calculateSecondsFrom())
//        }.map {
//            MetGoalDTO(it.value.runner, goals[it.key]!!.time, it.value.seasonBest.first())
//        }.toMutableList()

        return emptyList()

    }

    fun getRunnersWhoHaveNotMetGoal(startSeasonDate: Date, endSeasonDate: Date, adjustForDistance: Boolean): List<UnMetGoalDTO> {

        // todo

//        val seasonBests = seasonBestService.getAllSeasonBests(startSeasonDate, endSeasonDate, adjustForDistance)
//                .map { it.runner.id to it }.toMap()
//        val goals = xcGoalRepository.findBySeason(MeetPerformanceController.CURRENT_YEAR)
//                .filter { it.runnerId in seasonBests.keys }
//                .map { it.runnerId to it }.toMap()
//
//        return seasonBests
//                .filter { goals[it.key] != null }
//                .filter {
//            truncate(it.value.seasonBest.first().time.calculateSecondsFrom()) > truncate(goals[it.key]!!.time.calculateSecondsFrom())
//        }.map {
//            UnMetGoalDTO(it.value.runner, goals[it.key]!!.time, it.value.seasonBest.first(),
//                    (it.value.seasonBest.first().time.calculateSecondsFrom() - goals[it.key]!!.time.calculateSecondsFrom()).toMinuteSecondString())
//        }.toMutableList().sortedBy { it.difference }

        return emptyList()

    }

}