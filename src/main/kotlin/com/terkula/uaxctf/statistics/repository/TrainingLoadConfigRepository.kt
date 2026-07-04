package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TrainingLoadConfig
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TrainingLoadConfigRepository : CrudRepository<TrainingLoadConfig, Int> {
    fun findByConfigKeyAndTeam(configKey: String, team: String): Optional<TrainingLoadConfig>
    fun findByConfigKeyAndTeamIsNull(configKey: String): Optional<TrainingLoadConfig>
}
