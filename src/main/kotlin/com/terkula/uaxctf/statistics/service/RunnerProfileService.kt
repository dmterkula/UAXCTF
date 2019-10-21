package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.calculateSpread
import com.terkula.uaxctf.statistics.controller.MeetPerformanceController
import com.terkula.uaxctf.statistics.dto.MeetPerformanceDTO
import com.terkula.uaxctf.statistics.dto.RunnerMeetSplitDTO
import com.terkula.uaxctf.statistics.dto.toMeetSplit
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class RunnerProfileService (
        @field:Autowired
        internal var runnerRepository: RunnerRepository,
        @field:Autowired
        internal var seasonBestService: SeasonBestService,
        @field:Autowired
        internal var personalRecordService: PersonalRecordService,
        @field: Autowired
        internal var improvementRateService: ImprovementRateService,
        @field: Autowired
        internal var meetProgressionService: MeetProgressionService,
        @field: Autowired
        internal var meetRepository: MeetRepository,
        @field: Autowired
        internal var goalService: XcGoalService,
        @field: Autowired
        internal var meetMileSplitService: MeetMileSplitService,
        @field: Autowired
        internal val runnerConsistencyRankService: ConsistencyRankService,
        @field: Autowired
        internal val timeTrialProgressionService: TimeTrialProgressionService) {


    fun buildRunnerProfile(name: String) {

        val runner =
                try {
                    runnerRepository.findByNameContaining(name).first()
                } catch (e: Exception) {
                    throw RunnerNotFoundByPartialNameException("No runner matching the given name: $name")
                }

        val startDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-01-01")
        val endDate = Date.valueOf(MeetPerformanceController.CURRENT_YEAR + "-12-31")


        // todo entirely possible this throws exception if called in year before meet ran
        val seasonBests = seasonBestService.getSeasonBestsByName(name, listOf(startDate to endDate))
        var seasonBest: MeetPerformanceDTO? = null
        if (seasonBests.isNotEmpty()) {
            if (seasonBests.first().seasonBest.isNotEmpty()) {
                seasonBest = seasonBests.first().seasonBest.first()
            }
        }

        var prs = personalRecordService.getPRsByName(name)
        var pr: MeetPerformanceDTO?

        if (prs.isNotEmpty()) {
            if(prs.first().pr.isNotEmpty())
                pr = prs.first().pr.first()
        }

        val goal = goalService.getRunnersGoalForSeason(name, MeetPerformanceController.CURRENT_YEAR).first().time

        val improvementRate = improvementRateService.getImprovementRateForRunner(name, startDate, endDate, "")

        val meetSplits = meetMileSplitService.getAllMeetMileSplitsForRunner(name, startDate, endDate).mileSplits

        var mostConsistentRace = meetSplits.sortedBy { it.meetSplitsDTO.toMeetSplit().calculateSpread() }

        var mostConsistentIsSeasonBest = false
        var seasonBestSplits = listOf<RunnerMeetSplitDTO>()

        if (mostConsistentRace.isNotEmpty()) {
            mostConsistentRace = listOf(mostConsistentRace.first())

            if (seasonBest != null && mostConsistentRace.first().meetPerformanceDTO == seasonBest) {
                mostConsistentIsSeasonBest = true
            } else {
               if (seasonBest != null) seasonBestSplits =  meetSplits.filter { it.meetPerformanceDTO.meetName == seasonBest.meetName }
            }
        }

        val workoutConsistency =
                runnerConsistencyRankService.getRunnersOrderedByMostConsistentWorkouts(startDate, endDate)
        val workoutInfo = workoutConsistency
        .filter { it.runner.id == runner.id }
        .first()

        val raceConsistency = runnerConsistencyRankService.getRunnersOrderedByMostConsistentRaces(startDate, endDate)
        val raceInfo = raceConsistency
        .filter { it.runner.id == runner.id }
        .first()

        val totalConsistency = runnerConsistencyRankService.getRunnersOrderedMostConsistent(startDate, endDate, 1.0)
        val combinedInfo = totalConsistency
                .filter { it.runner.id == runner.id }
                .first()

        val progressionRank = timeTrialProgressionService.getRankedProgressionSinceTimeTrial(startDate, endDate)
                .filter { it.runner.id == runner.id }
                .first()


        // todo: response looks like (pr, goal, season best (was it most consistent race), most recent milesplits, last workout result, progression ranks, consistency ranks,


        // todo: need to get recent workout results and allow nullable results in response (season best)

        // todo: could find next upcoming meet and provide details from last year e.g splits, place, time, was in a seasonbest/pr

        println("test")

    }


}