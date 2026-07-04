package com.terkula.uaxctf.statistics.service

import com.terkula.uaxctf.statisitcs.model.TrainingLoadConfig
import com.terkula.uaxctf.statistics.repository.TrainingLoadConfigRepository
import org.springframework.stereotype.Service

@Service
class TrainingLoadConfigService(
    val configRepository: TrainingLoadConfigRepository
) {
    /**
     * Fetch a config value. Resolution order:
     * 1. Team-specific row (if team provided)
     * 2. Global row (team IS NULL)
     * 3. Hardcoded default (if row missing — safe fallback, never breaks)
     */
    fun get(key: String, team: String? = null, default: Double): Double {
        if (team != null) {
            val teamRow = configRepository.findByConfigKeyAndTeam(key, team)
            if (teamRow.isPresent) return teamRow.get().value
        }
        return configRepository.findByConfigKeyAndTeamIsNull(key)
            .map { it.value }
            .orElse(default)
    }

    fun getAll(): List<TrainingLoadConfig> =
        configRepository.findAll().toList().sortedBy { it.configKey }

    fun update(key: String, team: String?, value: Double): TrainingLoadConfig? {
        val row = if (team != null) {
            configRepository.findByConfigKeyAndTeam(key, team).orElse(null)
        } else {
            configRepository.findByConfigKeyAndTeamIsNull(key).orElse(null)
        } ?: return null
        row.value = value
        return configRepository.save(row)
    }
}
