package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*

@Entity
@Table(
    name = "coaching_group_memberships",
    schema = "uaxc",
    uniqueConstraints = [UniqueConstraint(columnNames = ["runner_id", "group_id"])]
)
class CoachingGroupMembership(
    @Column(name = "group_id", nullable = false)
    val groupId: Long,

    @Column(name = "runner_id", nullable = false)
    val runnerId: Int
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}
