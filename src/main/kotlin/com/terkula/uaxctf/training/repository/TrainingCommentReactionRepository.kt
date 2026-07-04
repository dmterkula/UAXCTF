package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.TrainingCommentReaction
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TrainingCommentReactionRepository : CrudRepository<TrainingCommentReaction, Int> {
    fun findByCommentUuid(commentUuid: String): List<TrainingCommentReaction>
    fun findByCommentUuidAndReactorUsernameAndEmoji(commentUuid: String, reactorUsername: String, emoji: String): TrainingCommentReaction?

    @Transactional
    fun deleteByCommentUuidAndReactorUsernameAndEmoji(commentUuid: String, reactorUsername: String, emoji: String): Int
}
