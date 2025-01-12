package com.terkula.uaxctf.training.repository

import com.terkula.uaxctf.training.model.TrainingComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TrainingCommentRepository : CrudRepository<TrainingComment, Int> {

    fun findByTrainingEntityUuid(trainingEntityUuid: String): List<TrainingComment>

    fun deleteByTrainingEntityUuid(trainingEntityUuid: String): List<TrainingComment>

}