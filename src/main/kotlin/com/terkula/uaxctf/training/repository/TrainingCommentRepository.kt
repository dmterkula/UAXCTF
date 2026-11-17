package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.TrainingComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TrainingCommentRepository : CrudRepository<TrainingComment, Int> {

    fun findByTrainingEntityUuid(trainingEntityUuid: String): List<TrainingComment>

    /**
     * Batch lookup across many logs at once. The reply queue needs the comments for a whole
     * roster's worth of logs to decide which ones a coach has already answered; fetching them
     * one entity at a time would be hundreds of round trips.
     */
    fun findByTrainingEntityUuidIn(trainingEntityUuids: List<String>): List<TrainingComment>

    fun deleteByTrainingEntityUuid(trainingEntityUuid: String): List<TrainingComment>

}