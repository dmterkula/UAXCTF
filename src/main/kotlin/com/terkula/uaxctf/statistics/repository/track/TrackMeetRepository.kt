package com.terkula.uaxctf.statistics.repository.track

import com.terkula.uaxctf.statisitcs.model.track.TrackMeet
import java.sql.Date
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TrackMeetRepository : CrudRepository<TrackMeet, String> {

    fun findByUuid(uuid: String): Optional<TrackMeet>

    fun findByIdAndDateBetween(id: String, start: Date, end: Date): MutableList<TrackMeet>

    fun findByDateBetween(start: Date, end: Date): MutableList<TrackMeet>

    fun findByNameContains(meetName: String): MutableList<TrackMeet>

    fun findByNameContainingAndDateBetween(meetName: String, start: Date, end: Date): MutableList<TrackMeet>

    fun findByTeam(team: String): MutableList<TrackMeet>

}


