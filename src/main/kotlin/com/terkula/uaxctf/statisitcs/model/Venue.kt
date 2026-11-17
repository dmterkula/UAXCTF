package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "venues", schema = "uaxc")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Venue(
    var name: String,
    @Column(name = "image_path")
    var imagePath: String?,
    var team: String,
    var address: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    @Column(name = "is_indoor")
    var isIndoor: Boolean = false
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
