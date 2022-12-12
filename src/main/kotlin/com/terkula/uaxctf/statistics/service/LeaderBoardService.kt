package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.PRDTO
import com.terkula.uaxctf.statistics.dto.RankedPRDTO
import com.terkula.uaxctf.statistics.dto.RankedRunnerConsistencyDTO
import com.terkula.uaxctf.statistics.dto.RankedSeasonBestDTO
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
    val trainingRunsService: TrainingRunsService
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

        return trainingRunsService.getAllTrainingMilesRunByRunner(season)

    }

}