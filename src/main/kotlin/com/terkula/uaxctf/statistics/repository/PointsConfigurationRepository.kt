package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.PointsConfiguration
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PointsConfigurationRepository : CrudRepository<PointsConfiguration, Int> {
    fun findByConfigKey(configKey: String): Optional<PointsConfiguration>
}