package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.FrontendToggle
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface FrontendToggleRepository : CrudRepository<FrontendToggle, Int> {
    fun findByToggleNameAndTeam(toggleName: String, team: String): Optional<FrontendToggle>
    fun findByToggleNameAndTeamIsNull(toggleName: String): Optional<FrontendToggle>
}
