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
    var team: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}
