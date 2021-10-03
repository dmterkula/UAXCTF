package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.Meet
import java.sql.Date
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MeetRepository : CrudRepository<Meet, Int> {

    fun findByIdAndDateBetween(id: Int, start: Date, end: Date): MutableList<Meet>

    fun findByDateBetween(start: Date, end: Date): MutableList<Meet>

    fun findByDateLessThan(targetDate: Date): MutableList<Meet>

    fun findByNameContains(meetName: String): MutableList<Meet>

    fun findByNameAndDateBetween(meetName: String, start: Date, end: Date): MutableList<Meet>

    fun findByNameContainsAndDateBetween(meetName: String, start: Date, end: Date): MutableList<Meet>




    fun findByNameAndDate(meetName: String, date: Date): Meet?

}


