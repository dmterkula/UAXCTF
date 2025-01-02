package com.terkula.uaxctf.training.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "training_comments", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class TrainingComment (
        @Column(name = "uuid")
        var uuid: String,
        @Column(name = "training_entity_uuid")
        var trainingEntityUuid: String,
        @Column(name = "made_by")
        var madeBy: String,
        @Column(name = "message")
        var message: String,
        @Column(name = "timestamp")
        var timestamp: Timestamp
){

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
