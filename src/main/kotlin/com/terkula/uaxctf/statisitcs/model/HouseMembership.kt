package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*

@Entity
@Table(
    name = "house_memberships",
    schema = "uaxc",
    uniqueConstraints = [UniqueConstraint(columnNames = ["runner_id", "house_id"])]
)
class HouseMembership(
    @Column(name = "house_id", nullable = false)
    val houseId: Long,

    @Column(name = "runner_id", nullable = false)
    val runnerId: Int
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}
