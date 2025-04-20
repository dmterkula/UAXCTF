package com.terkula.uaxctf.training.model.coachdavidisms

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "coach_david_isms", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class CoachDavidIsm(
      var text: String,
      var forNu: Boolean,
      var forUa: Boolean
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

}