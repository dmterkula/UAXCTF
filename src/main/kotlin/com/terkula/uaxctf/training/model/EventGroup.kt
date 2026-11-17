package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "event_groups", schema = "uaxc")
class EventGroup(
    @Column(name = "event_uuid", nullable = false)
    var eventUuid: String,

    @Column(name = "track_group", nullable = false)
    var trackGroup: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
}
