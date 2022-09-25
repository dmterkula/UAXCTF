package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Date
import javax.persistence.*

@Entity
@Table(name = "workout", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class Workout (var date: Date,
               var type: String,
               var description: String,
               @Column(name = "target_distance")
               var targetDistance: Int,
               @Column(name = "target_count")
               var targetCount: Int,
               @Column(name = "target_pace")
               var pace: String,
               @Column(name = "target_duration")
               var duration: String?,
               var title: String) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}