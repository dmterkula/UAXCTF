package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statistics.dto.RunnerGoalDTO
import com.terkula.uaxctf.statistics.exception.GoalNotFoundException
import com.terkula.uaxctf.statistics.exception.RunnerNotFoundByPartialNameException
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.XcGoalRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class XcGoalService (@field:Autowired
                     private val runnerRepository: RunnerRepository,
                     @field:Autowired
                     private val xcGoalRepository: XcGoalRepository) {


    fun getGoalsForSeason(season: String): List<RunnerGoalDTO> {

        val goals = xcGoalRepository.findBySeason(season)

        val runnerMap = goals.map {
            val runner = runnerRepository.findById(it.runnerId)
            runner.get().id to runner
        }.toMap()

        val runnerGoalDTOs = goals.map { RunnerGoalDTO(runnerMap[it.runnerId]!!.get(), it.time) }

        return runnerGoalDTOs

    }

    fun getRunnersGoalForSeason(name: String, season: String): List<RunnerGoalDTO> {

        val runner = runnerRepository.findByNameContaining(name).firstOrNull() ?: throw RunnerNotFoundByPartialNameException("No runner matching the given name of '$name'")

        val goal = xcGoalRepository.findByRunnerId(runner.id).firstOrNull()

        if (goal == null) {
            throw GoalNotFoundException("No goal found for $name")
        } else {
            return listOf(RunnerGoalDTO(runner, goal.time))
        }

    }

}