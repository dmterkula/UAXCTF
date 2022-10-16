package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Date
import javax.persistence.*

@Entity
@Table(name = "workout_clone", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class Workout (var date: Date,
               var description: String,
               var title: String,
               var icon: String,
               var uuid: String,
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}