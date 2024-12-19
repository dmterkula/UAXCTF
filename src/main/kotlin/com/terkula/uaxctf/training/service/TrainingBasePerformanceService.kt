package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.model.trainingbase.BaseTrainingPercentage
import com.terkula.uaxctf.training.model.trainingbase.TrainingBasePerformance
import com.terkula.uaxctf.training.repository.BaseTrainingPercentagesRepository
import com.terkula.uaxctf.training.repository.TrainingBasePerformanceRepository
import com.terkula.uaxctf.training.request.CreateBaseTrainingPerformanceRequest
import com.terkula.uaxctf.training.response.RunnersTrainingRunPaceRange
import com.terkula.uaxctf.training.response.TrainingRunPaceRange
import com.terkula.uaxctf.training.response.TrainingBasePerformanceResponse
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.removeDecimal
import com.terkula.uaxctf.util.secondsPerMileToPercentPacePerMile
import org.springframework.stereotype.Service

@Service
class TrainingBasePerformanceService(
        val trainingBasePerformanceRepository: TrainingBasePerformanceRepository,
        val runnerRepository: RunnerRepository,
        val baseTrainingPercentagesRepository: BaseTrainingPercentagesRepository,
        val runnerService: RunnerService
) {

    fun getRunnersTrainingBasePerformance(
            runnerId: Int,
            season: String,
            year: String
            ): TrainingBasePerformanceResponse {
        val basePace = trainingBasePerformanceRepository.findByRunnerIdAndSeasonAndYear(runnerId, season, year).firstOrNull()
        val runner = runnerRepository.findById(runnerId).get()

        return TrainingBasePerformanceResponse(runner, basePace)
    }

    fun getBaseTrainingPerformances(
            season: String,
            year: String
    ): List<TrainingBasePerformanceResponse> {

        var basePaces = trainingBasePerformanceRepository.findBySeasonAndYear(season, year)
        var runners = runnerRepository.findByGraduatingClassGreaterThanEqual(year).map { it.id to it }.toMap()

        return basePaces.map { TrainingBasePerformanceResponse(runners[it.runnerId]!!, it) }

    }

    fun getBaseTrainingPerformancesForEvent(
            event: String,
            season: String,
            year: String
    ): List<TrainingBasePerformanceResponse> {

        var basePaces = trainingBasePerformanceRepository.findByEventAndSeasonAndYear(event, season, year)
        var runners = runnerRepository.findByGraduatingClassGreaterThanEqual(year).map { it.id to it }.toMap()

        return basePaces.map { TrainingBasePerformanceResponse(runners[it.runnerId]!!, it) }

    }

    fun createOrUpdateTrainingBasePerformance(createBaseTrainingPerformanceRequest: CreateBaseTrainingPerformanceRequest): TrainingBasePerformanceResponse {

        var runner = runnerRepository.findById(createBaseTrainingPerformanceRequest.runnerId).get()
        var existingRecord = trainingBasePerformanceRepository.findByUuid(createBaseTrainingPerformanceRequest.uuid)

        if (existingRecord != null) {
            // update existing record

            existingRecord.event = createBaseTrainingPerformanceRequest.event
            existingRecord.fractionOfMiles = createBaseTrainingPerformanceRequest.fractionOfMiles
            existingRecord.season = createBaseTrainingPerformanceRequest.season
            existingRecord.year = createBaseTrainingPerformanceRequest.year
            existingRecord.seconds = createBaseTrainingPerformanceRequest.seconds

            trainingBasePerformanceRepository.save(existingRecord)

            return TrainingBasePerformanceResponse(runner, existingRecord)

        } else {
            val newRecord = TrainingBasePerformance(
                    createBaseTrainingPerformanceRequest.runnerId,
                    createBaseTrainingPerformanceRequest.fractionOfMiles,
                    createBaseTrainingPerformanceRequest.seconds,
                    createBaseTrainingPerformanceRequest.season,
                    createBaseTrainingPerformanceRequest.event,
                    createBaseTrainingPerformanceRequest.year,
                    createBaseTrainingPerformanceRequest.uuid
            )

            trainingBasePerformanceRepository.save(newRecord)

            return TrainingBasePerformanceResponse(runner, newRecord)
        }

    }

    fun createOrUpdateTrainingBasePerformanceManually(): TrainingBasePerformanceResponse? {

//        var runner = runnerRepository.findById(createBaseTrainingPerformanceRequest.runnerId).get()
//        var existingRecord = trainingBasePerformanceRepository.findByUuid(createBaseTrainingPerformanceRequest.uuid)
//
//        if (existingRecord != null) {
//            // update existing record
//
//            existingRecord.event = createBaseTrainingPerformanceRequest.event
//            existingRecord.fractionOfMiles = createBaseTrainingPerformanceRequest.fractionOfMiles
//            existingRecord.season = createBaseTrainingPerformanceRequest.season
//            existingRecord.year = createBaseTrainingPerformanceRequest.year
//            existingRecord.seconds = createBaseTrainingPerformanceRequest.seconds
//
//            trainingBasePerformanceRepository.save(existingRecord)
//
//            return TrainingBasePerformanceResponse(runner, existingRecord)
//
//        } else {
//            val newRecord = TrainingBasePerformance(
//                    createBaseTrainingPerformanceRequest.runnerId,
//                    createBaseTrainingPerformanceRequest.fractionOfMiles,
//                    createBaseTrainingPerformanceRequest.seconds,
//                    createBaseTrainingPerformanceRequest.season,
//                    createBaseTrainingPerformanceRequest.event,
//                    createBaseTrainingPerformanceRequest.year,
//                    createBaseTrainingPerformanceRequest.uuid
//            )
//
//            trainingBasePerformanceRepository.save(newRecord)
//
//            return TrainingBasePerformanceResponse(runner, newRecord)
//        }

        return null

    }

    fun getRunnersBaseTrainingPaces(type: String, season: String, year: String, runnerId: Int): TrainingRunPaceRange? {
        val baseTrainingPercentages = baseTrainingPercentagesRepository.findByTypeAndSeason(type, season)
        val paceRanges = getRunnersPaceRanges(runnerId, season, year, baseTrainingPercentages)
        return paceRanges;
    }


    fun getRunnersPaceRanges(runnerId: Int, season: String, year: String, baseTrainingPercentages: List<BaseTrainingPercentage>): TrainingRunPaceRange? {
        return getRunnersPaceRanges(trainingBasePerformanceRepository.findByRunnerIdAndSeasonAndYear(runnerId, season, year), baseTrainingPercentages)
    }

    fun getRunnersPaceRanges(basePerformances: List<TrainingBasePerformance>,  baseTrainingPercentages: List<BaseTrainingPercentage>): TrainingRunPaceRange? {
        return basePerformances.map {
            it to baseTrainingPercentages.filter { percentage -> percentage.event == it.event }
        }
                .map {
                    it.second.map { percentage -> Triple(percentage.event, percentage.percent, (it.first.seconds / it.first.fractionOfMiles).secondsPerMileToPercentPacePerMile(percentage.percent, 1.0)
                            .removeDecimal()) }
                }.flatten()
                .groupBy {
                    it.first
                }
                .map {
                    it.key to it.value.map { triple -> triple.second to triple.third }.sortedBy { pair-> pair.second.calculateSecondsFrom() }
                }.filter {
                    it.second.isNotEmpty() && it.second.size == 2
                }
                .sortedBy {
                    it.second.first().second.calculateSecondsFrom()
                }
                .map {
                    TrainingRunPaceRange(it.first, it.second[0].first, it.second[0].second, it.second[1].first, it.second[1].second)
                }
                .firstOrNull()
    }

    fun getAllRunnersBaseTrainingPaces(type: String, season: String, year: String): List<RunnersTrainingRunPaceRange> {
        val baseTrainingPercentages = baseTrainingPercentagesRepository.findByTypeAndSeason(type, season)
        val paceRanges = getAllRunnersPaceRanges(season, year, baseTrainingPercentages)
        return paceRanges
    }

    fun getAllRunnersPaceRanges(season: String, year: String, baseTrainingPercentages: List<BaseTrainingPercentage>): List<RunnersTrainingRunPaceRange> {

       val runners = if (season == "track") {
            runnerService.getTrackRoster(true, year)
        } else {
            runnerService.getXcRoster(true, year)
       }
               .map { it.id to it }.toMap()

        val trainingPaceRanges: List<RunnersTrainingRunPaceRange> = trainingBasePerformanceRepository.findBySeasonAndYear(season, year)
                .groupBy { it.runnerId }
                .map {
                    runners[it.key]!! to it.value
                }.map {
                    RunnersTrainingRunPaceRange(it.first, getRunnersPaceRanges(it.second, baseTrainingPercentages))
                }.sortedBy { it.trainingRunPaceRange?.fastEndPace?.calculateSecondsFrom() }

           return trainingPaceRanges

    }

}