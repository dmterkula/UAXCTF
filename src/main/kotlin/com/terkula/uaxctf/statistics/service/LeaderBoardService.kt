package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.*
import com.terkula.uaxctf.statistics.request.SortingMethodContainer
import com.terkula.uaxctf.training.response.RankedRunnerDistanceRunDTO
import com.terkula.uaxctf.training.service.TrainingRunsService
import com.terkula.uaxctf.util.TimeUtilities
import com.terkula.uaxctf.util.round
import org.springframework.stereotype.Service

@Service
class LeaderBoardService(
    val prService: PersonalRecordService,
    val seasonBestService: SeasonBestService,
    val consistencyRankService: ConsistencyRankService,
    val trainingRunsService: TrainingRunsService,
    val timeTrialService: TimeTrialService
) {

    fun getPRLeaderBoard(pageSize: Int): List<RankedPRDTO> {

        val allPRs: List<PRDTO> = prService.getAllPRs("2017", filterClass = "", adjustForDistance = false, sortingMethodContainer = SortingMethodContainer.TIME)
                .filter { it.pr.isNotEmpty() }
                .take(pageSize)

        return allPRs.mapIndexed { index, it ->
            RankedPRDTO(it.runner, it.pr.first(), index + 1)
        }

    }

    fun getSeasonBestLeaderBoard(season: String): List<RankedSeasonBestDTO> {
        val currentSBs = seasonBestService.getAllSeasonBests(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season), false)

       return currentSBs.filter {
            it.seasonBest.isNotEmpty()
        }.mapIndexed() { index, it ->
            RankedSeasonBestDTO(it.runner, it.seasonBest.first(), index + 1)
        }

    }

    // time trial improvement covered

   fun getRaceConsistentRankLeaderBoard(season: String): List<RankedRunnerConsistencyDTO> {

       return consistencyRankService.getRunnersOrderedByMostConsistentRaces(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season))
               .map {
                   RankedRunnerConsistencyDTO(it.runner, it.consistencyRank.consistencyValue.round(2), it.consistencyRank.rank)
               }
   }

    fun getDistanceRunRankLeaderBoard(season: String): List<RankedRunnerDistanceRunDTO> {

        // need to get workout splits sum and total meets run

        return trainingRunsService.getAllTrainingMilesRunByRunner(season)

    }

    fun getTimeTrialProgressionLeaderBoard(season: String): List<TimeTrialImprovementDTO> {
        return timeTrialService.getRankedProgressionSinceTimeTrial(TimeUtilities.getFirstDayOfGivenYear(season), TimeUtilities.getLastDayOfGivenYear(season), false)
    }

}