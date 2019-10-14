package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.RawTimeTrial
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RawTimeTrialRepository: CrudRepository<RawTimeTrial, String> {

}