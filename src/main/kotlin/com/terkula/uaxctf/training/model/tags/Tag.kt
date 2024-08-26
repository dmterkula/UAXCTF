package com.terkula.uaxctf.training.model.tags

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "tags", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class Tag(
    @Column(name = "uuid")
    var uuid: String,
    @Column(name = "tag")
    var tag: String,
    @Column(name = "category")
    var category: String,
    @Column(name = "runner_id")
    var runnerId: Int?
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0


}