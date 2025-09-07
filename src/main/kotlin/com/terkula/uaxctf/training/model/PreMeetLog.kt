package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "pre_meet_log", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class PreMeetLog(@Column(name = "meet_uuid")
                   var meetId: String,
                 @Column(name = "runner_id")
                   var runnerId: Int,
                 @Column(name = "goals")
                   var goals: String?,
                 @Column(name = "plan")
                   var plan: String?,
                 @Column(name = "confidence")
                   var confidence: String?,
                 @Column(name = "preparation")
                   var preparation: String?,
                 @Column(name = "when_its_hard")
                   var whenItsHard: String?,
                 @Column(name = "questions")
                   var questions: String?,
                 @Column(name = "notes")
                   var notes: String?,
                 @Column(name = "sleep_score")
                   var sleepScore: Int?,
                 @Column(name = "fueling_score")
                   var fuelingScore: Int?,
                 @Column(name = "hydration_score")
                   var hydrationScore: Int?,
                 @Column(name = "soreness_score")
                   var sorenessScore: Int?,
                 @Column(name = "uuid")
                 var uuid: String,
                  ) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
