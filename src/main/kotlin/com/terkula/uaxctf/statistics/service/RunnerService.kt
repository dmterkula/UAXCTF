package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.request.CreateRunnerRequest
import org.springframework.stereotype.Service

@Service
class RunnerService (val runnerRepository: RunnerRepository) {

    fun createRunner(createRunnerRequest: CreateRunnerRequest): Runner {

        val runner = Runner(createRunnerRequest.name, createRunnerRequest.graduatingClass, createRunnerRequest.isActive)

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

            runnerRepository.save(foundRunner)

            foundRunner
        } else {
            null
        }
    }

}