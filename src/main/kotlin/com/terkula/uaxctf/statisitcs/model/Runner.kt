package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*

@Entity
@Table(name = "runners", schema = "uaxc")
class Runner(
         var name: String,
         @Column(name = "graduating_class")
         var graduatingClass: String,
         @Column(name = "is_active")
         var isActive: Boolean = true,
         @Column(name = "xc")
         var doesXc: Boolean = true,
         @Column(name = "track")
         var doesTrack: Boolean = true,
         @Column(name = "device_id")
         var deviceId: String?,
         @Column(name = "team")
         var team: String,

) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
