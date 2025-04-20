package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.service.RunnerService
import com.terkula.uaxctf.training.model.trainingbase.*
import com.terkula.uaxctf.training.repository.BaseTrainingPercentagesRepository
import com.terkula.uaxctf.training.repository.RunnerTrainingBasePercentageRepository
import com.terkula.uaxctf.training.repository.TrainingBasePerformanceRepository
import com.terkula.uaxctf.training.request.CreateBaseTrainingPerformanceRequest
import com.terkula.uaxctf.training.request.CreateRunnerBaseTrainingPercentageRequest
import com.terkula.uaxctf.training.response.*
import com.terkula.uaxctf.util.calculateSecondsFrom
import com.terkula.uaxctf.util.removeDecimal
import com.terkula.uaxctf.util.secondsPerMileToPercentPacePerMile
import org.springframework.stereotype.Service

@Service
class TrainingBasePerformanceService(
        val trainingBasePerformanceRepository: TrainingBasePerformanceRepository,
        val runnerRepository: RunnerRepository,
        val baseTrainingPercentagesRepository: BaseTrainingPercentagesRepository,
        val runnerService: RunnerService,
        val runnerBaseTrainingPercentagesRepository: RunnerTrainingBasePercentageRepository
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
            year: String,
            team: String
    ): List<TrainingBasePerformanceResponse> {

        var basePaces = trainingBasePerformanceRepository.findBySeasonAndYear(season, year)

        val bent = if (year.toInt() >= 2024) {
            runnerRepository.findByTeam("NU")
        } else {
            emptyList<Runner>()
        }
        var runners = runnerRepository.findByGraduatingClassGreaterThanEqual(year).toMutableList().plus(bent).map{ it.id to it }.toMap()

        return basePaces.map { TrainingBasePerformanceResponse(runners[it.runnerId]!!, it) }
                .filter { it.runner.team == team }
                .groupBy { it.runner.id }.map { it.value }.flatten()

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

/////////////

    // these functions are used for building the base training pace management view used in the iphone app
    // to see a runners whole set of paces for each pace type.

    fun getAllRunnersBaseTrainingPacesForView(event: String, season: String, year: String): List<RunnersTrainingPacePercentLabels> {
        val runners =
                if (season == "track") {
                    runnerService.getTrackRoster(true, year)
                } else {
                    runnerService.getXcRoster(true, year)
                }

        return runners.map { getRunnersBaseTrainingPacesForView(event, season, year, it.id) }
                .filter {
                    it.trainingPacePercentLabels.isNotEmpty()
                }
                .sortedBy { it.trainingPacePercentLabels.first().pacePerMile.calculateSecondsFrom() }
    }


    fun getRunnersBaseTrainingPacesForView(event: String, season: String, year: String, runnerId: Int): RunnersTrainingPacePercentLabels {

        // given runner, event, and season, give pace ranges, sorted by paceType and then by pace.


        val baseTrainingPercentages = baseTrainingPercentagesRepository.findByEventAndSeason(event, season)
        val paceRanges = getRunnersPaceRangesForView(runnerId, season, year, baseTrainingPercentages)
        val runner = runnerRepository.findById(runnerId).get()

        return RunnersTrainingPacePercentLabels(runner, paceRanges)
    }

    fun getRunnersPaceRangesForView(runnerId: Int, season: String, year: String, baseTrainingPercentages: List<BaseTrainingPercentage>): List<TrainingPacePercentLabel> {
        val personalPercentages: List<RunnerBaseTrainingPercentage> = runnerBaseTrainingPercentagesRepository.findByRunnerIdAndSeasonAndYear(runnerId, season, year)
        return getRunnersPaceRangesForView(trainingBasePerformanceRepository.findByRunnerIdAndSeasonAndYear(runnerId, season, year), baseTrainingPercentages, personalPercentages)
    }


    fun getRunnersPaceRangesForView(basePerformances: List<TrainingBasePerformance>,  baseTrainingPercentages: List<BaseTrainingPercentage>, personalPercentages: List<RunnerBaseTrainingPercentage>): List<TrainingPacePercentLabel> {
        return basePerformances.map {
            it to baseTrainingPercentages.filter { percentage -> percentage.event == it.event }
        }
                .map { basePerformanceToPercents ->
                    basePerformanceToPercents.second.map {
                        percentage ->
                        val personalPercent: Int? = personalPercentages.firstOrNull { it.event == percentage.event && it.paceType == percentage.type && it.paceName == percentage.paceName }?.percent
                        var percentageForCalculation: Int = percentage.percent
                        if (personalPercent != null) {
                            percentageForCalculation = personalPercent
                        }
                        TrainingPacePercentLabel(percentage.type, percentage.paceName, percentage.event, percentageForCalculation, (basePerformanceToPercents.first.seconds / basePerformanceToPercents.first.fractionOfMiles).secondsPerMileToPercentPacePerMile(percentageForCalculation, 1.0).removeDecimal())

                    }
                }.flatten()
                .groupBy { it.paceType }
                .map { it.key to it.value.sortedBy { it.percent }}
                .map { it.second }
                .flatten()
    }

    //////////////////////////

    fun getRunnersBaseTrainingPaces(type: String, season: String, year: String, runnerId: Int): TrainingRunPaceRange? {
        val baseTrainingPercentages = baseTrainingPercentagesRepository.findByTypeAndSeason(type, season)
        val paceRanges = getRunnersPaceRanges(runnerId, season, year, baseTrainingPercentages)
        return paceRanges;
    }


    fun getRunnersPaceRanges(runnerId: Int, season: String, year: String, baseTrainingPercentages: List<BaseTrainingPercentage>): TrainingRunPaceRange? {
        val personalPercentages: List<RunnerBaseTrainingPercentage> = runnerBaseTrainingPercentagesRepository.findByRunnerIdAndSeasonAndYear(runnerId, season, year)
        return getRunnersPaceRanges(trainingBasePerformanceRepository.findByRunnerIdAndSeasonAndYear(runnerId, season, year), baseTrainingPercentages, personalPercentages)
    }

    fun getRunnersPaceRanges(basePerformances: List<TrainingBasePerformance>,  baseTrainingPercentages: List<BaseTrainingPercentage>, personalPercentages: List<RunnerBaseTrainingPercentage>): TrainingRunPaceRange? {
        return basePerformances.map {
            it to baseTrainingPercentages.filter { percentage -> percentage.event == it.event }
        }
                .map { basePerformanceToPercents ->
                    basePerformanceToPercents.second.map {
                        percentage ->
                        val personalPercent: Int? = personalPercentages.firstOrNull { it.event == percentage.event && it.paceType == percentage.type && it.paceName == percentage.paceName }?.percent
                        var percentageForCalculation: Int = percentage.percent
                        if (personalPercent != null) {
                            percentageForCalculation = personalPercent
                        }
                        Triple(percentage.event, percentage.percent, (basePerformanceToPercents.first.seconds / basePerformanceToPercents.first.fractionOfMiles).secondsPerMileToPercentPacePerMile(percentageForCalculation, 1.0)
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

        val personalPercentages: List<RunnerBaseTrainingPercentage> = runnerBaseTrainingPercentagesRepository.findBySeasonAndYear(season, year)

        val trainingPaceRanges: List<RunnersTrainingRunPaceRange> = trainingBasePerformanceRepository.findBySeasonAndYear(season, year)
                .groupBy { it.runnerId }
                .filter { runners[it.key] != null }
                .map {
                    runners[it.key]!! to it.value
                }.map {
                    RunnersTrainingRunPaceRange(it.first, getRunnersPaceRanges(it.second, baseTrainingPercentages, personalPercentages.filter { pp -> pp.runnerId == it.first.id }))
                }.sortedBy { it.trainingRunPaceRange?.fastEndPace?.calculateSecondsFrom() }

        return trainingPaceRanges

    }

    fun getAllRunnersBaseTrainingPercentages(season: String, year: String, team: String): List<RunnerBaseTrainingPercentageResponse> {
        return runnerBaseTrainingPercentagesRepository.findBySeasonAndYear(season, year)
                .map { it to runnerRepository.findById(it.runnerId) }
                .filter { it.second.isPresent }
                .filter { it.second.get().team == team }
                .map {
                    RunnerBaseTrainingPercentageResponse(it.second.get(), it.first.event, it.first.paceType,
                            it.first.paceName, it.first.percent, it.first.season, it.first.year, it.first.uuid)
                }
                .sortedBy { it.runner.id }
    }

    fun resetRunnersBaseTrainingPercentagesToDefault(createRunnerBaseTrainingPercentage: CreateRunnerBaseTrainingPercentageRequest): RunnersTrainingPacePercentLabels {
        val runner = runnerRepository.findById(createRunnerBaseTrainingPercentage.runnerId).get()

        runnerBaseTrainingPercentagesRepository.findBySeasonAndYearAndEvent(
                createRunnerBaseTrainingPercentage.season,
                createRunnerBaseTrainingPercentage.year,
                createRunnerBaseTrainingPercentage.event
        )
            .forEach {
                runnerBaseTrainingPercentagesRepository.delete(it)
            }

        return getRunnersBaseTrainingPacesForView(createRunnerBaseTrainingPercentage.event, createRunnerBaseTrainingPercentage.season, createRunnerBaseTrainingPercentage.year, runner.id)

    }

    fun createRunnersBaseTrainingPercentage(createRunnerBaseTrainingPercentage: CreateRunnerBaseTrainingPercentageRequest): RunnersTrainingPacePercentLabels {

        val runner = runnerRepository.findById(createRunnerBaseTrainingPercentage.runnerId).get()

         createRunnerBaseTrainingPercentage.pacePercentages.forEach {
            var existingRecord = runnerBaseTrainingPercentagesRepository.findByUuid(it.uuid)

            if (existingRecord != null) {
                // update existing record

                existingRecord.event = createRunnerBaseTrainingPercentage.event
                existingRecord.paceType = it.paceType
                existingRecord.paceName = it.paceName
                existingRecord.season = createRunnerBaseTrainingPercentage.season
                existingRecord.year = createRunnerBaseTrainingPercentage.year
                existingRecord.percent = it.percent


            } else {

                val defaultRecords = baseTrainingPercentagesRepository.findByEventAndSeasonAndTypeAndPaceNameAndPercent(
                        createRunnerBaseTrainingPercentage.event,
                        createRunnerBaseTrainingPercentage.season,
                        it.paceType,
                        it.paceName,
                        it.percent
                )

                if (defaultRecords.isEmpty()) {
                    // no default record exists for same percentage, go ahead and create.
                    val newRecord = RunnerBaseTrainingPercentage(
                            createRunnerBaseTrainingPercentage.runnerId,
                            createRunnerBaseTrainingPercentage.event,
                            it.paceType,
                            it.paceName,
                            it.percent,
                            createRunnerBaseTrainingPercentage.season,
                            createRunnerBaseTrainingPercentage.year,
                            it.uuid
                    )
                    runnerBaseTrainingPercentagesRepository.save(newRecord)

                } else {
                    // noop for existing default record
                }
            }
        }

        return getRunnersBaseTrainingPacesForView(createRunnerBaseTrainingPercentage.event, createRunnerBaseTrainingPercentage.season, createRunnerBaseTrainingPercentage.year, runner.id)

    }

    fun getPaceTypes(season: String, year: String, event: String): List<String> {
        return baseTrainingPercentagesRepository.findAll().filter { it.event == event }.map { it.type }.sorted().distinct()
    }

    fun getPaceNames(season: String, year: String, event: String): List<String> {
        return baseTrainingPercentagesRepository.findAll().filter { it.event == event }.map { it.paceName }.sorted().distinct()
    }

    fun getCurrentBasePercentAndPacePerMile(event: String, season: String, year: String, paceType: String, paceName: String, runnerId: Int): BasePercentPerformanceAndPacePerMile {
        val basePercent = getBasePercentForPaceTypeAndPaceName(season, year, paceType, paceName)
        val baseTime: TrainingBasePerformance? = trainingBasePerformanceRepository.findByRunnerIdAndEventAndSeasonAndYear(runnerId, event, season, year).firstOrNull()
        var pacePerMile = "N/A"
        if (baseTime != null) {
            pacePerMile =  (baseTime!!.seconds / baseTime.fractionOfMiles).secondsPerMileToPercentPacePerMile(basePercent, 1.0)
        }

        return BasePercentPerformanceAndPacePerMile(basePercent, baseTime, pacePerMile)

    }

    fun calculatePacePerMileForNewPercent(event: String, season: String, year: String, percent: Int, runnerId: Int): BasePercentPerformanceAndPacePerMile {
        val baseTime: TrainingBasePerformance? = trainingBasePerformanceRepository.findByRunnerIdAndEventAndSeasonAndYear(runnerId, event, season, year).firstOrNull()
        var pacePerMile = "N/A"
        if (baseTime != null) {
            pacePerMile =  (baseTime!!.seconds / baseTime.fractionOfMiles).secondsPerMileToPercentPacePerMile(percent, 1.0).removeDecimal()
        }

        return BasePercentPerformanceAndPacePerMile(percent, baseTime, pacePerMile)

    }

    fun getBasePercentForPaceTypeAndPaceName(season: String, year: String, paceType: String, paceName: String) : Int {
        var percent: Int? = baseTrainingPercentagesRepository.findBySeasonAndTypeAndPaceName(season, paceType, paceName).firstOrNull()?.percent

        return if (percent != null) {
            percent
        } else {
            0
        }

    }



}