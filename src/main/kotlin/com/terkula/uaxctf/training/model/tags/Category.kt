package com.terkula.uaxctf.training.model.tags

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "categories", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class Category(
    @Column(name = "uuid")
    val uuid: String,
    @Column(name = "name")
    var name: String,
    @Column(name = "runner_id")
    var runnerId: Int?
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}