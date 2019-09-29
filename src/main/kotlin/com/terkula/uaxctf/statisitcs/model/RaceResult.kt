package com.terkula.uaxctf.statisitcs.model

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "race_results", schema = "uaxc")
class RaceResult(@Id
                 val name: String,
                 val time: String,
                 val place: Int)