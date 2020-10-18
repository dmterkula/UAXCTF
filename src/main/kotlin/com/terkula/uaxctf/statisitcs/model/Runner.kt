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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Runner

        if (name != other.name) return false
        if (graduatingClass != other.graduatingClass) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + graduatingClass.hashCode()
        result = 31 * result + id
        return result
    }


}

fun Runner.wasFreshmanDuringYear(atYear: Int): Boolean {
    return this.graduatingClass.toInt() - atYear == 4

}
