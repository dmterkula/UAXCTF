package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*

@Entity
@Table(name = "xc_meet_info", schema = "uaxc")
data class MeetInfo (
        val meetId: Int,
        val distance: Int,
        @Column(name = "elevation_change")
        val elevationChange: Int,
        val temperature: Int,
        val humidity: Double,
        val windSpeed: Int,
        @Column(name = "cloud_cover_ratio")
        val cloudCoverRatio: Double,
        @Column(name = "is_rainy")
        val isRainy: Boolean,
        @Column(name = "is_snowy")
        val isSnowy: Boolean) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JoinColumn
    val id: Int = 0
}