package com.example.shiyu.fsrs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class FsrsRating(val value: Int, val label: String) {
    AGAIN(1, "忘了"),
    HARD(2, "困难"),
    GOOD(3, "记住了"),
    EASY(4, "简单")
}

enum class FsrsState(val value: Int) {
    NEW(0),
    LEARNING(1),
    REVIEW(2),
    RELEARNING(3)
}

data class FsrsCard(
    val due: Long = System.currentTimeMillis(),
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val state: FsrsState = FsrsState.NEW,
    val lapses: Int = 0,
    val reps: Int = 0,
    val lastReview: Long? = null
)

data class RatingResult(
    val card: FsrsCard,
    val intervalDays: Double
)

object FsrsScheduler {
    private const val REQUEST_RETENTION = 0.9
    private const val MAXIMUM_INTERVAL = 365.0

    /**
     * Rating review and calculate new SRS card parameters
     */
    fun review(card: FsrsCard, rating: FsrsRating, now: Long = System.currentTimeMillis()): RatingResult {
        val lastReview = card.lastReview ?: now
        val elapsedDays = max(0.0, (now - lastReview) / (1000.0 * 60 * 60 * 24))

        val newReps = card.reps + 1
        var newLapses = card.lapses
        if (rating == FsrsRating.AGAIN) {
            newLapses += 1
        }

        var newStability: Double
        var newDifficulty: Double
        var newState: FsrsState

        if (card.state == FsrsState.NEW) {
            // First time review
            newDifficulty = max(1.0, min(10.0, 5.0 - (rating.value - 3) * 0.8))
            newStability = when (rating) {
                FsrsRating.AGAIN -> 0.4
                FsrsRating.HARD -> 1.0
                FsrsRating.GOOD -> 3.0
                FsrsRating.EASY -> 7.0
            }
            newState = if (rating == FsrsRating.AGAIN) FsrsState.LEARNING else FsrsState.REVIEW
        } else {
            // Subsequent reviews
            val retrievability = if (card.stability > 0) {
                (1.0 + elapsedDays / (9.0 * card.stability)).pow(-1.0)
            } else 1.0

            // Update difficulty
            val difficultyDelta = -0.1 * (rating.value - 3)
            newDifficulty = max(1.0, min(10.0, card.difficulty + difficultyDelta))

            if (rating == FsrsRating.AGAIN) {
                newStability = max(0.2, card.stability * 0.4)
                newState = FsrsState.RELEARNING
            } else {
                val hardPenalty = if (rating == FsrsRating.HARD) 0.8 else 1.0
                val easyBonus = if (rating == FsrsRating.EASY) 1.3 else 1.0
                val factor = exp(0.1 * (10.0 - newDifficulty)) * (1.0 - retrievability)
                newStability = max(0.5, card.stability * (1.0 + factor * hardPenalty * easyBonus))
                newState = FsrsState.REVIEW
            }
        }

        // Calculate next interval days
        var intervalDays = newStability * (9.0 * (1.0 / REQUEST_RETENTION - 1.0))
        if (rating == FsrsRating.AGAIN) {
            intervalDays = 0.05 // ~1 hour
        } else if (rating == FsrsRating.HARD && intervalDays < 1.0) {
            intervalDays = 1.0
        }
        intervalDays = min(MAXIMUM_INTERVAL, max(0.05, intervalDays))

        val nextDue = now + (intervalDays * 24.0 * 60.0 * 60.0 * 1000.0).toLong()

        val updatedCard = card.copy(
            due = nextDue,
            stability = newStability,
            difficulty = newDifficulty,
            state = newState,
            lapses = newLapses,
            reps = newReps,
            lastReview = now
        )

        return RatingResult(updatedCard, intervalDays)
    }

    /**
     * Preview interval days for each rating button
     */
    fun previewRatings(card: FsrsCard, now: Long = System.currentTimeMillis()): Map<FsrsRating, String> {
        return FsrsRating.entries.associateWith { rating ->
            val result = review(card, rating, now)
            formatInterval(result.intervalDays)
        }
    }

    fun formatInterval(days: Double): String {
        return when {
            days < 0.1 -> "< 1小时"
            days < 1.0 -> "1天内"
            days < 30.0 -> "${days.toInt()}天"
            days < 365.0 -> "${(days / 30.0).toInt()}个月"
            else -> String.format("%.1f年", days / 365.0)
        }
    }
}
