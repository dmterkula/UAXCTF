package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.heartrate.HeartRateSeries
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface HeartRateSeriesRepository : CrudRepository<HeartRateSeries, Int> {

    fun findByActivityTypeAndActivityUuidAndRunnerId(activityType: String, activityUuid: String, runnerId: Int): List<HeartRateSeries>

    @Transactional
    fun deleteByActivityTypeAndActivityUuidAndRunnerId(activityType: String, activityUuid: String, runnerId: Int): List<HeartRateSeries>
}
