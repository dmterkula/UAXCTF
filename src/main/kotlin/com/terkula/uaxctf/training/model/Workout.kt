package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Date
import javax.persistence.*

@Entity
@Table(name = "workout", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class Workout (val date: Date,
               val type: String,
               val description: String,
               @Column(name = "target_distance")
               val targetDistance: Int,
               @Column(name = "target_count")
               val targetCount: Int,
               @Column(name = "target_pace")
               val pace: String,
               @Column(name = "target_duration")
               val duration: String?,
               val title: String) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}