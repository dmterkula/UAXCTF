package com.terkula.uaxctf.statistics.repository

import com.terkula.uaxctf.statisitcs.model.TeamTalk
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TeamTalkRepository : CrudRepository<TeamTalk, Int> {
    fun findByUuid(uuid: String): TeamTalk?

    @Query("""
        SELECT t FROM TeamTalk t
        WHERE t.season = :season AND t.year = :year AND t.team = :team
          AND (:includeDrafts = true OR t.published = true)
        ORDER BY COALESCE(t.publishedAt, t.updatedAt) DESC
    """)
    fun findForList(
        @Param("season") season: String,
        @Param("year") year: String,
        @Param("team") team: String,
        @Param("includeDrafts") includeDrafts: Boolean
    ): List<TeamTalk>

    @Query("""
        SELECT t FROM TeamTalk t
        WHERE (:includeDrafts = true OR t.published = true)
        ORDER BY COALESCE(t.publishedAt, t.updatedAt) DESC
    """)
    fun findAllForList(@Param("includeDrafts") includeDrafts: Boolean): List<TeamTalk>
}
