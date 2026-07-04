package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.service.TrainingLoadConfigService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class TrainingLoadConfigResponse(
    val id: Int,
    val configKey: String,
    val team: String?,
    val value: Double,
    val description: String?
)

data class UpdateTrainingLoadConfigRequest(val value: Double)

@RestController
@RequestMapping("/xc/training-load-config")
class TrainingLoadConfigController(val configService: TrainingLoadConfigService) {

    @GetMapping
    fun getAll(): List<TrainingLoadConfigResponse> =
        configService.getAll().map {
            TrainingLoadConfigResponse(it.id, it.configKey, it.team, it.value, it.description)
        }

    @PutMapping("/{key}")
    fun update(
        @PathVariable key: String,
        @RequestParam(required = false) team: String?,
        @RequestBody body: UpdateTrainingLoadConfigRequest
    ): ResponseEntity<TrainingLoadConfigResponse> {
        val updated = configService.update(key, team, body.value)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            TrainingLoadConfigResponse(updated.id, updated.configKey, updated.team, updated.value, updated.description)
        )
    }
}
