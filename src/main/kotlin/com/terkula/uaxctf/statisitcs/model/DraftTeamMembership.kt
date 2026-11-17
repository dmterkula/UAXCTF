package com.terkula.uaxctf.statisitcs.model

import javax.persistence.*

@Entity
@Table(
    name = "draft_team_memberships",
    schema = "uaxc",
    uniqueConstraints = [UniqueConstraint(columnNames = ["runner_id", "team_id"])]
)
class DraftTeamMembership(
    @Column(name = "team_id", nullable = false)
    val teamId: Long,

    @Column(name = "runner_id", nullable = false)
    val runnerId: Int
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}
