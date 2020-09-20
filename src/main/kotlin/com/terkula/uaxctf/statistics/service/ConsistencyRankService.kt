package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.ConsistencyRank
import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.dto.RunnerConsistencyDTO
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.request.MeetSplitsOption
import com.terkula.uaxctf.training.service.WorkoutResultService
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.round
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.Date

@Component
class ConsistencyRankService (@field:Autowired
                              internal val workoutResultService: WorkoutResultService,
                              @field:Autowired
                              internal val meetMileSplitService: MeetMileSplitService,
                              @field: Autowired
                              internal val meetRepository: MeetRepository) {

    fun getRunnersOrderedMostConsistent(startDate: Date, endDate: Date, weight: Double): List<RunnerConsistencyDTO> {

        val workoutConsistencyRank = getRunnersOrderedByMostConsistentWorkouts(startDate, endDate)
        val racesConsistencyRank = getRunnersOrderedByMostConsistentRaces(startDate, endDate)

        val combined =  workoutConsistencyRank.map {
            it to racesConsistencyRank.filter { race-> race.runner == it.runner }.firstOrNull()
        }
                .toMap()
                .map {
                    RunnerConsistencyDTO(it.key.runner, ConsistencyRank(0, calculateAverageConsistencyValue(it.key, it.value, weight) ))
                }.sortedBy { it.consistencyRank.consistencyValue }
                .mapIndexed { index, pair ->
                    RunnerConsistencyDTO(pair.runner, ConsistencyRank(index + 1, pair.consistencyRank.consistencyValue))
                }

        return combined

    }

    fun getRunnersOrderedByMostConsistentRaces(startDate: Date, endDate: Date): List<RunnerConsistencyDTO> {

        val races = meetRepository.findByDateBetween(startDate, endDate).filter { it.name == "Lebanon" }.map { it.name }

        val spreadsGroupedByRunner = races.map { meetMileSplitService.getMeetSplitInfo(it, MeetSplitsOption.Spread,
                startDate, endDate, "lowest", 50) }.flatten().groupBy { it.runner }

        return spreadsGroupedByRunner.map {
            it.key to it.value.map { spread-> spread.avgDifference.calculateSecondsFrom()}.average()
        } .sortedBy { it.second }.
                mapIndexed { index, pair ->
                    RunnerConsistencyDTO(pair.first, ConsistencyRank(index + 1, pair.second))
                }

    }

    fun getRunnersOrderedByMostConsistentWorkouts(startDate: Date, endDate: Date): List<RunnerConsistencyDTO> {

        val runners = workoutResultService.getEveryRunnerWhoHasRanAWorkout(startDate, endDate)

        val workouts =  runners.map { workoutResultService.getWorkoutsForRunner(startDate, endDate,
                it.name, 0, "", "") }

        return workouts.map {
            it.runner to it.workouts.map { wkt -> wkt.workoutResultDTO.spread.calculateSecondsFrom() }.average()
        }.sortedBy { it.second }.mapIndexed { index, pair ->
                    RunnerConsistencyDTO(pair.first, ConsistencyRank(index + 1, pair.second))
                }

    }

    private fun calculateAverageConsistencyValue(rank1: RunnerConsistencyDTO, rank2: RunnerConsistencyDTO?, weight: Double): Double {
        return if (rank2 == null) {
            rank1.consistencyRank.consistencyValue.round(3)
        } else {
            (((rank1.consistencyRank.consistencyValue * weight) + rank2.consistencyRank.consistencyValue) / 2).round(3)
        }

    }


}