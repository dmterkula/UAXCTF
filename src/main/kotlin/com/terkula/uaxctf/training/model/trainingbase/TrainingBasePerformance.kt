package com.terkula.uaxctf.training.model.trainingbase

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "runners_training_base_performance", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class TrainingBasePerformance (
        @Column(name = "runner_id")
        var runnerId: Int,
        @Column(name = "fraction_of_miles")
        var fractionOfMiles: Double,
        @Column(name = "seconds")
        var seconds: Int,
        @Column(name = "season")
        var season: String,
        @Column(name = "event")
        var event: String,
        @Column(name = "year")
        var year: String,
        @Column(name = "uuid")
        var uuid: String,
        @Column(name = "created_at", nullable = false, updatable = false)
        var createdAt: Timestamp? = null,
        @Column(name = "updated_at", nullable = false)
        var updatedAt: Timestamp? = null
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

    /**
     * Automatically sets timestamps when the entity is first persisted.
     * Called by JPA before the INSERT statement is executed.
     */
    @PrePersist
    protected fun onCreate() {
        val now = Timestamp(System.currentTimeMillis())
        createdAt = now
        updatedAt = now
    }

    /**
     * Automatically updates the updated_at timestamp when the entity is modified.
     * Called by JPA before the UPDATE statement is executed.
     */
    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }

}