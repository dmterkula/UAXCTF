package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import javax.persistence.*

class EncouragementSaveId(
    var encouragementUuid: String = "",
    var runnerId: Int = 0
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncouragementSaveId) return false
        return encouragementUuid == other.encouragementUuid && runnerId == other.runnerId
    }

    override fun hashCode(): Int {
        var result = encouragementUuid.hashCode()
        result = 31 * result + runnerId
        return result
    }
}

@Entity
@Table(name = "encouragement_save", schema = "uaxc")
@IdClass(EncouragementSaveId::class)
class EncouragementSave(
    @Id
    @Column(name = "encouragement_uuid", nullable = false, length = 36)
    var encouragementUuid: String,

    @Id
    @Column(name = "runner_id", nullable = false)
    var runnerId: Int
)
