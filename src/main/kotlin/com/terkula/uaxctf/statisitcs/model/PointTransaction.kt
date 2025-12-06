package com.terkula.uaxctf.statisitcs.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "point_transactions", schema = "uaxc")
class PointTransaction(
        @Column(name = "runner_id", nullable = false)
        val runnerId: Int,

        @Column(name = "transaction_type", nullable = false, length = 20)
        val transactionType: String, // EARNED, SPENT, REFUND

        @Column(name = "points_amount", nullable = false)
        val pointsAmount: Int,

        @Column(name = "activity_type", length = 50)
        val activityType: String? = null, // TRAINING_RUN, WORKOUT, CROSS_TRAINING, MEET_LOG, PRE_MEET_LOG

        @Column(name = "feature_type", length = 50)
        val featureType: String? = null, // SEND_GIF, PIN_MESSAGE, MESSAGE_ANIMATION, CHANGE_CHAT_NAME, CUSTOM_AVATAR

        @Column(name = "activity_uuid")
        val activityUuid: String? = null,

        @Column(name = "description", columnDefinition = "TEXT")
        var description: String? = null,

        @Column(name = "balance_after", nullable = false)
        val balanceAfter: Int,

        @Column(name = "season", length = 20)
        val season: String? = null,

        @Column(name = "is_refunded")
        var isRefunded: Boolean = false,

        @Column(name = "refunded_at")
        var refundedAt: Timestamp? = null,

        @Column(name = "related_transaction_id")
        var relatedTransactionId: Long? = null

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    var id: Long = 0

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis())

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
    }
}

enum class TransactionType {
    EARNED,
    SPENT,
    REFUND
}

enum class ActivityType {
    TRAINING_RUN,
    WORKOUT,
    CROSS_TRAINING,
    MEET_LOG,
    PRE_MEET_LOG
}

enum class FeatureType {
    SEND_GIF,
    PIN_MESSAGE,
    MESSAGE_ANIMATION,
    CHANGE_CHAT_NAME,
    CUSTOM_AVATAR
}