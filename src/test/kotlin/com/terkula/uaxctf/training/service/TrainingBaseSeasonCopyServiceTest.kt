package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statisitcs.model.Runner
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.training.model.trainingbase.TrainingBasePerformance
import com.terkula.uaxctf.training.repository.TrainingBasePerformanceRepository
import com.terkula.uaxctf.training.request.CopyBaseTrainingPacesRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp

class TrainingBaseSeasonCopyServiceTest {

    private lateinit var baseRepo: TrainingBasePerformanceRepository
    private lateinit var runnerRepo: RunnerRepository
    private lateinit var service: TrainingBaseSeasonCopyService

    private fun <T : Any> T.withId(id: Int): T {
        val field = this::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.setInt(this, id)
        return this
    }

    private fun runner(id: Int, name: String) =
            Runner(name, "2027", deviceId = null, team = "UA", trackGroup = null).withId(id)

    private fun perf(id: Int, runnerId: Int, seconds: Int, season: String, year: String, createdAt: Long = 1_000L) =
            TrainingBasePerformance(runnerId, 3.11, seconds, season, "5000m", year, "uuid-$id",
                    createdAt = Timestamp(createdAt)).withId(id)

    @BeforeEach
    fun setUp() {
        baseRepo = mock(TrainingBasePerformanceRepository::class.java)
        runnerRepo = mock(RunnerRepository::class.java)
        service = TrainingBaseSeasonCopyService(baseRepo, runnerRepo)

        // Runner 1 has two XC records (newest wins); runner 2 already has a track pace.
        Mockito.`when`(baseRepo.findByEventAndSeasonAndYear("5000m", "xc", "2026")).thenReturn(listOf(
                perf(10, 1, 1200, "xc", "2026", createdAt = 1_000L),
                perf(11, 1, 1130, "xc", "2026", createdAt = 2_000L),
                perf(12, 2, 1300, "xc", "2026")
        ))
        Mockito.`when`(baseRepo.findByEventAndSeasonAndYear("5000m", "track", "2027")).thenReturn(listOf(
                perf(20, 2, 1290, "track", "2027")
        ))
        Mockito.`when`(runnerRepo.findAllById(listOf(1, 2))).thenReturn(listOf(runner(1, "A"), runner(2, "B")))
        Mockito.`when`(baseRepo.save(Mockito.any(TrainingBasePerformance::class.java))).thenAnswer { it.arguments[0] }
    }

    private fun request(dryRun: Boolean, type: String = "PERCENT", value: Double = 3.0, overwrite: Boolean = false, ids: List<Int> = emptyList()) =
            CopyBaseTrainingPacesRequest(fromYear = "2026", toYear = "2027", runnerIds = ids,
                    adjustmentType = type, adjustmentValue = value, dryRun = dryRun, overwriteExisting = overwrite)

    @Test
    fun `dry run previews newest source with percent adjustment and writes nothing`() {
        val response = service.copyBaseTrainingPaces(request(dryRun = true))

        val a = response.results.first { it.runner.id == 1 }
        assertEquals(1130, a.sourceSeconds)
        assertEquals(1164, a.newSeconds) // 1130 * 1.03 = 1163.9
        assertEquals("WILL_CREATE", a.status)
        assertEquals("SKIPPED_EXISTING", response.results.first { it.runner.id == 2 }.status)
        verify(baseRepo, never()).save(Mockito.any(TrainingBasePerformance::class.java))
    }

    @Test
    fun `seconds per mile adjustment scales by distance`() {
        val response = service.copyBaseTrainingPaces(request(dryRun = true, type = "SECONDS_PER_MILE", value = 10.0))
        assertEquals(1161, response.results.first { it.runner.id == 1 }.newSeconds) // 1130 + 31.1
    }

    @Test
    fun `total seconds adjustment adds to the race time`() {
        val response = service.copyBaseTrainingPaces(request(dryRun = true, type = "TOTAL_SECONDS", value = 30.0))
        assertEquals(1160, response.results.first { it.runner.id == 1 }.newSeconds) // 1130 + 30
    }

    @Test
    fun `commit creates tagged track records and skips existing`() {
        val response = service.copyBaseTrainingPaces(request(dryRun = false, ids = listOf(1, 2)))

        assertEquals(1, response.createdCount)
        assertEquals(1, response.skippedCount)
        val captor = ArgumentCaptor.forClass(TrainingBasePerformance::class.java)
        verify(baseRepo, times(1)).save(captor.capture())
        val saved = captor.value
        assertEquals(1, saved.runnerId)
        assertEquals("track", saved.season)
        assertEquals("2027", saved.year)
        assertEquals("5000m", saved.event)
        assertEquals(1164, saved.seconds)
        assertEquals("SEASON_COPY", saved.source)
        assertEquals(11, saved.derivedFromBasePerformanceId)
        assertEquals("Copied from xc 2026 5000m, +3% (was 18:50.0)", saved.sourceDetail)
    }

    @Test
    fun `overwrite adds a newer record for runners with an existing pace`() {
        val response = service.copyBaseTrainingPaces(request(dryRun = false, overwrite = true, ids = listOf(1, 2)))
        assertEquals(2, response.createdCount)
        assertEquals("REPLACED", response.results.first { it.runner.id == 2 }.status)
    }

    @Test
    fun `commit without runners is rejected`() {
        assertThrows(ResponseStatusException::class.java) { service.copyBaseTrainingPaces(request(dryRun = false)) }
    }
}
