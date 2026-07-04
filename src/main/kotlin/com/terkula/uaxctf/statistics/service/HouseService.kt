package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.House
import com.terkula.uaxctf.statisitcs.model.HouseMembership
import com.terkula.uaxctf.statistics.repository.HouseMembershipRepository
import com.terkula.uaxctf.statistics.repository.HouseRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.SeasonalPointsTrackingRepository
import com.terkula.uaxctf.statistics.request.AssignRunnerRequest
import com.terkula.uaxctf.statistics.request.CreateHouseRequest
import com.terkula.uaxctf.statistics.request.UpdateHouseRequest
import com.terkula.uaxctf.statistics.response.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HouseService(
    val houseRepository: HouseRepository,
    val houseMembershipRepository: HouseMembershipRepository,
    val seasonalPointsTrackingRepository: SeasonalPointsTrackingRepository,
    val runnerRepository: RunnerRepository
) {

    fun getHouses(season: String, year: String): List<HouseDTO> {
        return houseRepository.findBySeasonAndYear(season, year).map { it.toDTO() }
    }

    fun getStandings(season: String, year: String): HouseStandingsResponse {
        val houses = houseRepository.findBySeasonAndYear(season, year)
        if (houses.isEmpty()) {
            return HouseStandingsResponse(season = season, year = year, standings = emptyList())
        }

        val houseIds = houses.map { it.id }
        val allMemberships = houseMembershipRepository.findByHouseIdIn(houseIds)
        val membershipsByHouse = allMemberships.groupBy { it.houseId }

        // Fetch seasonal points for all members in one query per house
        val entries = houses.map { house ->
            val memberships = membershipsByHouse[house.id] ?: emptyList()
            val runnerIds = memberships.map { it.runnerId }
            val totalPoints = if (runnerIds.isEmpty()) {
                0
            } else {
                seasonalPointsTrackingRepository.calculateTeamTotal(season, year, runnerIds)
            }
            Triple(house, totalPoints, memberships.size)
        }

        // Sort by total points descending and assign ranks
        val sorted = entries.sortedByDescending { it.second }
        val standings = sorted.mapIndexed { index, (house, totalPoints, memberCount) ->
            HouseStandingsEntry(
                id = house.id,
                name = house.name,
                totalPoints = totalPoints,
                memberCount = memberCount,
                rank = index + 1
            )
        }

        return HouseStandingsResponse(season = season, year = year, standings = standings)
    }

    @Transactional
    fun createHouse(request: CreateHouseRequest): HouseDTO {
        val house = House(
            name = request.name,
            season = request.season,
            year = request.year,
            createdBy = request.createdBy
        )
        return houseRepository.save(house).toDTO()
    }

    @Transactional
    fun updateHouse(id: Long, request: UpdateHouseRequest): HouseDTO {
        val house = houseRepository.findById(id)
            .orElseThrow { RuntimeException("House not found: $id") }
        house.name = request.name
        return houseRepository.save(house).toDTO()
    }

    @Transactional
    fun deleteHouse(id: Long) {
        if (!houseRepository.existsById(id)) {
            throw RuntimeException("House not found: $id")
        }
        // Memberships cascade via FK, but CrudRepository.deleteById doesn't trigger FK cascade
        // on all drivers — delete memberships explicitly first
        houseMembershipRepository.deleteByHouseId(id)
        houseRepository.deleteById(id)
    }

    @Transactional
    fun assignRunner(houseId: Long, request: AssignRunnerRequest): HouseMembership {
        val house = houseRepository.findById(houseId)
            .orElseThrow { RuntimeException("House not found: $houseId") }

        // Check runner is not already in another house for this season/year
        val siblingHouseIds = houseRepository.findBySeasonAndYear(house.season, house.year)
            .map { it.id }
        val existing = houseMembershipRepository.findByRunnerIdAndHouseIdIn(request.runnerId, siblingHouseIds)
        if (existing.isNotEmpty()) {
            val conflictHouseId = existing.first().houseId
            val conflictName = houseRepository.findById(conflictHouseId).map { it.name }.orElse("unknown")
            throw IllegalArgumentException(
                "Runner ${request.runnerId} is already assigned to house '$conflictName' for ${house.season} ${house.year}"
            )
        }

        val membership = HouseMembership(houseId = houseId, runnerId = request.runnerId)
        return houseMembershipRepository.save(membership)
    }

    @Transactional
    fun removeRunner(houseId: Long, runnerId: Int) {
        if (!houseRepository.existsById(houseId)) {
            throw RuntimeException("House not found: $houseId")
        }
        houseMembershipRepository.deleteByHouseIdAndRunnerId(houseId, runnerId)
    }

    fun getRosterAssignment(season: String, year: String): List<HouseRosterEntryDTO> {
        val houses = houseRepository.findBySeasonAndYear(season, year)
        val houseIds = houses.map { it.id }
        val allMemberships = if (houseIds.isEmpty()) emptyList()
                             else houseMembershipRepository.findByHouseIdIn(houseIds)
        val membershipByRunner = allMemberships.associateBy { it.runnerId }
        val houseById = houses.associateBy { it.id }

        // Get runners active for this season/year
        val yearInt = year.toIntOrNull() ?: return emptyList()
        val allRunners = if (season.equals("xc", ignoreCase = true)) {
            runnerRepository.findByDoesXc(true)
                .filter { it.graduatingClass.toInt() > yearInt && it.graduatingClass.toInt() <= yearInt + 4 }
                .filter { it.isActive }
        } else {
            runnerRepository.findByDoesTrack(true)
                .filter { it.graduatingClass.toInt() >= yearInt && it.graduatingClass.toInt() < yearInt + 4 }
                .filter { it.isActive }
        }

        return allRunners.map { runner ->
            val membership = membershipByRunner[runner.id]
            val house = membership?.let { houseById[it.houseId] }
            HouseRosterEntryDTO(
                runnerId = runner.id,
                runnerName = runner.name,
                houseId = house?.id,
                houseName = house?.name
            )
        }.sortedBy { it.runnerName }
    }

    fun getHouseMembersWithPoints(houseId: Long, season: String, year: String): List<HouseMemberPointsDTO> {
        val memberships = houseMembershipRepository.findByHouseId(houseId)
        if (memberships.isEmpty()) return emptyList()

        val runnerIds = memberships.map { it.runnerId }
        val runnerMap = runnerIds.mapNotNull { runnerRepository.findById(it).orElse(null) }
            .associateBy { it.id }
        val pointsMap = seasonalPointsTrackingRepository.findBySeasonAndYear(season, year)
            .filter { it.runnerId in runnerIds }
            .associateBy { it.runnerId }

        return runnerIds.map { runnerId ->
            HouseMemberPointsDTO(
                runnerId = runnerId,
                runnerName = runnerMap[runnerId]?.name ?: "Unknown",
                points = pointsMap[runnerId]?.pointsEarned ?: 0
            )
        }.sortedByDescending { it.points }
    }

    fun getRunnerHouse(runnerId: Int, season: String, year: String): RunnerHouseResponse {
        val houses = houseRepository.findBySeasonAndYear(season, year)
        val houseIds = houses.map { it.id }
        val membership = if (houseIds.isEmpty()) null
                         else houseMembershipRepository.findByRunnerIdAndHouseIdIn(runnerId, houseIds).firstOrNull()
        val house = membership?.let { houseRepository.findById(it.houseId).orElse(null) }
        return RunnerHouseResponse(
            runnerId = runnerId,
            houseId = house?.id,
            houseName = house?.name,
            season = season,
            year = year
        )
    }

    private fun House.toDTO() = HouseDTO(id = id, name = name, season = season, year = year)
}
