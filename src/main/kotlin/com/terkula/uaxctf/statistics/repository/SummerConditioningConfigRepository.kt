package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.SummerConditioningConfig
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SummerConditioningConfigRepository : CrudRepository<SummerConditioningConfig, Long> {
    fun findByYear(year: String): Optional<SummerConditioningConfig>
}
