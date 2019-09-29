package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "runners", schema = "uaxc")
class Runner(
             var name: String,
             @Column(name = "graduating_class")
             var graduatingClass: String) {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    val id: Int = 0

    override fun toString(): String {
        return this.name + ": " + graduatingClass
    }
}
