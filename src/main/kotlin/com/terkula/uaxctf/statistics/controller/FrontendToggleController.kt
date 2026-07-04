package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statisitcs.model.FrontendToggle
import com.terkula.uaxctf.statistics.repository.FrontendToggleRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class ToggleResponse(
    val id: Int,
    val toggleName: String,
    val enabled: Boolean,
    val team: String?,
    val description: String?
)

data class UpdateToggleRequest(
    val enabled: Boolean,
    val toggleName: String?,
    val team: String?,
    val clearTeam: Boolean = false,
    val description: String?
)

data class CreateToggleRequest(
    val toggleName: String,
    val team: String?,
    val enabled: Boolean,
    val description: String?
)

@RestController
@RequestMapping("/api/v1/toggles")
class FrontendToggleController(
    val frontendToggleRepository: FrontendToggleRepository
) {

    @GetMapping
    fun getAllToggles(): ResponseEntity<List<ToggleResponse>> {
        val all = frontendToggleRepository.findAll().map {
            ToggleResponse(it.id, it.toggleName, it.enabled, it.team, it.description)
        }
        return ResponseEntity.ok(all)
    }

    @RequestMapping(value = ["/{toggleName}"], method = [RequestMethod.GET])
    fun getToggle(
        @PathVariable toggleName: String,
        @RequestParam(required = false) team: String?
    ): ResponseEntity<ToggleResponse> {
        if (team != null) {
            val teamToggle = frontendToggleRepository.findByToggleNameAndTeam(toggleName, team)
            if (teamToggle.isPresent) {
                val t = teamToggle.get()
                return ResponseEntity.ok(ToggleResponse(t.id, toggleName, t.enabled, t.team, t.description))
            }
        }

        val globalToggle = frontendToggleRepository.findByToggleNameAndTeamIsNull(toggleName)
        val t = globalToggle.orElse(null)
        return if (t != null) {
            ResponseEntity.ok(ToggleResponse(t.id, toggleName, t.enabled, t.team, t.description))
        } else {
            ResponseEntity.ok(ToggleResponse(0, toggleName, false, null, null))
        }
    }

    @PostMapping
    fun createToggle(@RequestBody body: CreateToggleRequest): ResponseEntity<ToggleResponse> {
        val toggle = FrontendToggle(
            toggleName = body.toggleName,
            team = body.team,
            enabled = body.enabled,
            description = body.description
        )
        val saved = frontendToggleRepository.save(toggle)
        return ResponseEntity.ok(ToggleResponse(saved.id, saved.toggleName, saved.enabled, saved.team, saved.description))
    }

    @PutMapping("/{id}")
    fun updateToggle(
        @PathVariable id: Int,
        @RequestBody body: UpdateToggleRequest
    ): ResponseEntity<ToggleResponse> {
        val toggle = frontendToggleRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        toggle.enabled = body.enabled
        body.toggleName?.let { toggle.toggleName = it }
        toggle.team = if (body.clearTeam) null else body.team ?: toggle.team
        body.description?.let { toggle.description = it }
        frontendToggleRepository.save(toggle)
        return ResponseEntity.ok(ToggleResponse(toggle.id, toggle.toggleName, toggle.enabled, toggle.team, toggle.description))
    }

    @DeleteMapping("/{id}")
    fun deleteToggle(@PathVariable id: Int): ResponseEntity<Void> {
        if (!frontendToggleRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }
        frontendToggleRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
