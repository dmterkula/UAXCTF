package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.RaceResult
import org.springframework.data.repository.CrudRepository

interface RaceResultRepository : CrudRepository<RaceResult, String>
