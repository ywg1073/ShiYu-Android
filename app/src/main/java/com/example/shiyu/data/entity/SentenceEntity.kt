package com.example.shiyu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sentences")
data class SentenceEntity(
    @PrimaryKey val id: String,
    val sentence: String,
    val explanation: String,
    val articlePath: String? = null,
    val reviewCount: Long = 0,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // FSRS SRS fields
    val srsDue: Long? = null,
    val srsStability: Double = 0.0,
    val srsDifficulty: Double = 0.0,
    val srsState: Int = 0,
    val srsLapses: Int = 0,
    val srsReps: Int = 0,
    val srsLastReview: Long? = null
)
