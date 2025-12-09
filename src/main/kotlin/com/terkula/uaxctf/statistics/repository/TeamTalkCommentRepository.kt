package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalkComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamTalkCommentRepository : CrudRepository<TeamTalkComment, Int> {
    fun findByUuid(uuid: String): TeamTalkComment?
    fun findByUuidAndUsername(uuid: String, username: String): List<TeamTalkComment>
    fun findByTeamTalkUuidAndUsername(teamTalkUuid: String, username: String): List<TeamTalkComment>
    fun findByTeamTalkUuidOrderByCreatedAtAsc(teamTalkUuid: String): List<TeamTalkComment>
    fun findByTeamTalkUuidAndParentCommentUuidIsNullOrderByCreatedAtAsc(teamTalkUuid: String): List<TeamTalkComment>
    fun findByParentCommentUuidOrderByCreatedAtAsc(parentCommentUuid: String): List<TeamTalkComment>
}
