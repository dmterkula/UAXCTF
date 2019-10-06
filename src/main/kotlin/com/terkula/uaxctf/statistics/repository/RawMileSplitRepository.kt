package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.RawMileSplit
import org.springframework.data.repository.CrudRepository

interface RawMileSplitRepository: CrudRepository<RawMileSplit, String> {
}