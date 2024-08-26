package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.CreateRunnerRequest
import org.springframework.stereotype.Service

@Service
class RunnerService (val runnerRepository: RunnerRepository) {

    fun createRunner(createRunnerRequest: CreateRunnerRequest): Runner {

        val runner = Runner(
                createRunnerRequest.name,
                createRunnerRequest.graduatingClass,
                createRunnerRequest.isActive,
                createRunnerRequest.doesXc,
                createRunnerRequest.doesTrack,
                createRunnerRequest.deviceId,
                createRunnerRequest.team
        )

        runnerRepository.save(runner)

        return runner
    }

    fun updateRunner(runnerId: Int, createRunnerRequest: CreateRunnerRequest): Runner? {

        var runner = runnerRepository.findById(runnerId)

        return if (runner.isPresent) {
            val foundRunner = runner.get()

            foundRunner.name = createRunnerRequest.name
            foundRunner.graduatingClass = createRunnerRequest.graduatingClass
            foundRunner.isActive = createRunnerRequest.isActive
            foundRunner.doesXc = createRunnerRequest.doesXc
            foundRunner.doesTrack = createRunnerRequest.doesTrack
            foundRunner.team = createRunnerRequest.team


            runnerRepository.save(foundRunner)

            foundRunner
        } else {
            null
        }
    }

    fun getRoster(active: Boolean, season: String): List<Runner> {

        var runners = runnerRepository.findAll()
                .filter { it.graduatingClass.toInt() > season.toInt() && it.graduatingClass.toInt() <= season.toInt() + 4 }

        if (active) {
            runners = runners.filter { it.isActive }
        }

        return runners
                .groupBy { it.graduatingClass }.map {
                    it.key to it.value.sortedBy { runner -> runner.name }
                }
                .sortedBy { it.first }
                .map { it.second }
                .flatten()

    }

    fun getXcRoster(active: Boolean, season: String): List<Runner> {

        var runners = runnerRepository.findByDoesXc(true)
                .filter { it.graduatingClass.toInt() > season.toInt() && it.graduatingClass.toInt() <= season.toInt() + 4 }

        if (active) {
            runners = runners.filter { it.isActive }
        }

        return runners
                .groupBy { it.graduatingClass }.map {
                    it.key to it.value.sortedBy { runner -> runner.name }
                }
                .sortedBy { it.first }
                .map { it.second }
                .flatten()

    }

    fun getTrackRoster(active: Boolean, season: String): List<Runner> {
        var runners = runnerRepository.findByDoesTrack(true)
                .filter { it.graduatingClass.toInt() >= season.toInt() && it.graduatingClass.toInt() < season.toInt() + 4 }

        if (active) {
            runners = runners.filter { it.isActive }
        }

        return runners
                .groupBy { it.graduatingClass }.map {
                    it.key to it.value.sortedBy { runner -> runner.name }
                }
                .sortedBy { it.first }
                .map { it.second }
                .flatten()

    }

    fun getRunnersByTeam(team: String): List<Runner> {
        return runnerRepository.findByTeam(team).sortedByDescending { it.graduatingClass }
    }

}