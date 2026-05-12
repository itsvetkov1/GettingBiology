package com.znam.app.data

import androidx.room.Entity

/**
 * Tracks per-question performance for spaced repetition.
 * Composite key: quizType + questionId (questions live in separate DBs).
 */
@Entity(
    tableName = "question_performance",
    primaryKeys = ["quizType", "questionId"]
)
data class QuestionPerformance(
    val quizType: String,
    val questionId: Int,
    val timesAnswered: Int = 0,
    val timesCorrect: Int = 0,
    val consecutiveCorrect: Int = 0,
    val consecutiveWrong: Int = 0,
    val lastAnsweredAt: Long = 0L,
    val nextReviewAt: Long = 0L,      // spaced repetition: when to show again
    val difficultyScore: Float = 0.5f  // 0.0 = easy, 1.0 = hard (for this user)
) {
    val accuracyRate: Float
        get() = if (timesAnswered > 0) timesCorrect.toFloat() / timesAnswered else 0.5f

    /**
     * SM-2 inspired interval calculation.
     * Returns milliseconds until next review.
     */
    fun computeNextInterval(wasCorrect: Boolean): Long {
        val baseIntervalMs = 24 * 60 * 60 * 1000L // 1 day in ms
        return if (wasCorrect) {
            when (consecutiveCorrect + 1) {
                1 -> baseIntervalMs           // 1 day
                2 -> baseIntervalMs * 3       // 3 days
                3 -> baseIntervalMs * 7       // 1 week
                4 -> baseIntervalMs * 14      // 2 weeks
                5 -> baseIntervalMs * 30      // 1 month
                else -> baseIntervalMs * 60   // 2 months max
            }
        } else {
            // Wrong answer — review soon
            baseIntervalMs / 2  // 12 hours
        }
    }
}
