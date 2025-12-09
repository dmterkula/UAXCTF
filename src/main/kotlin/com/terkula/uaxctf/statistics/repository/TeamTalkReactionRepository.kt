package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalkReaction
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TeamTalkReactionRepository : CrudRepository<TeamTalkReaction, Int> {
    fun findByTeamTalkUuid(teamTalkUuid: String): List<TeamTalkReaction>
    fun findByTeamTalkUuidAndUsername(teamTalkUuid: String, username: String): List<TeamTalkReaction>
    fun findByTeamTalkUuidAndUsernameAndEmoji(teamTalkUuid: String, username: String, emoji: String): TeamTalkReaction?

    @Transactional
    fun deleteByTeamTalkUuidAndUsernameAndEmoji(teamTalkUuid: String, username: String, emoji: String): Int
}
