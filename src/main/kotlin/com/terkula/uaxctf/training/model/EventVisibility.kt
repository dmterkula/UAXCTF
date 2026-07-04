package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "event_visibility", schema = "uaxc")
class EventVisibility(
    @Column(name = "event_uuid", nullable = false)
    var eventUuid: String,

    @Column(name = "runner_id", nullable = false)
    var runnerId: Int
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
}
