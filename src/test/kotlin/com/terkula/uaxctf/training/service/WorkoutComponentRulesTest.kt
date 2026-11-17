package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.training.request.ComponentCreationElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class WorkoutComponentRulesTest {

    private fun component(
            type: String = "Interval",
            description: String = "6x 3min @90%, 60s jog",
            targetDistance: Int = 400,
            targetEvent: Int = 5000,
            recovery: String? = "01:00",
            recoveryType: String? = "Active",
            recoveryDistance: Int? = null,
            percent: Int? = 90,
            duration: String? = "03:00",
            setRecovery: String? = "00:00",
            sets: Int? = 1
    ) = ComponentCreationElement(
            description = description, type = type, pace = "Base Training Pace", targetDistance = targetDistance,
            targetCount = 6, duration = duration, targetPaceAdjustment = "0:00.0", uuid = "c-1", ratio = 1.0, sets = sets,
            recovery = recovery, recoveryType = recoveryType, recoveryDistance = recoveryDistance,
            setRecovery = setRecovery, targetEvent = targetEvent, percent = percent)

    @Test
    fun `null recoveryDistance on Active recovery is defaulted to 0 so the app never force-unwraps null`() {
        val c = component(recoveryType = "Active", recoveryDistance = null)
        WorkoutComponentRules.normalizeAndValidate(listOf(c))
        assertEquals(0, c.recoveryDistance)
    }

    @Test
    fun `missing form-defaulted fields are filled with the form defaults`() {
        val c = component(recovery = null, recoveryType = null, recoveryDistance = null, percent = null,
                duration = null, setRecovery = null, sets = null)
        WorkoutComponentRules.normalizeAndValidate(listOf(c))
        assertEquals("00:00", c.recovery)
        assertEquals("00:00", c.duration)
        assertEquals("00:00", c.setRecovery)
        assertEquals("Standing", c.recoveryType)
        assertEquals(0, c.recoveryDistance)
        assertEquals(100, c.percent)
        assertEquals(1, c.sets)
    }

    @Test
    fun `descriptionOnly components are left untouched`() {
        val c = component(type = "descriptionOnly", recovery = null, recoveryType = null, recoveryDistance = null,
                percent = null, duration = null, targetDistance = 0)
        WorkoutComponentRules.normalizeAndValidate(listOf(c))
        assertEquals(null, c.recoveryType)
        assertEquals(null, c.recoveryDistance)
        assertEquals(null, c.percent)
    }

    @Test
    fun `invalid values are rejected with a 400 listing every problem`() {
        val bad = component(description = "x".repeat(256), targetDistance = 0, targetEvent = 1500,
                recovery = "1 minute", recoveryType = "Jog", percent = 150)
        val ex = assertThrows(ResponseStatusException::class.java) { WorkoutComponentRules.normalizeAndValidate(listOf(bad)) }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
        val msg = ex.reason!!
        assertTrue(msg.contains("description is 256 chars"), msg)
        assertTrue(msg.contains("targetDistance"), msg)
        assertTrue(msg.contains("targetEvent 1500"), msg)
        assertTrue(msg.contains("recovery '1 minute'"), msg)
        assertTrue(msg.contains("recoveryType 'Jog'"), msg)
        assertTrue(msg.contains("percent 150"), msg)
    }

    @Test
    fun `a workout with no components is rejected`() {
        assertThrows(ResponseStatusException::class.java) { WorkoutComponentRules.normalizeAndValidate(emptyList()) }
    }

    @Test
    fun `a valid timed interval passes unchanged`() {
        val c = component(recoveryType = "Standing", recoveryDistance = 0)
        WorkoutComponentRules.normalizeAndValidate(listOf(c))
        assertEquals("Standing", c.recoveryType)
        assertEquals(90, c.percent)
        assertEquals("03:00", c.duration)
    }

    @Test
    fun `fractional mm ss values from existing data are accepted`() {
        val c = component(setRecovery = "00:00.00", recovery = "2:30")
        WorkoutComponentRules.normalizeAndValidate(listOf(c))
    }
}
