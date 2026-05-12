package com.znam.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records one completed quiz session for historical tracking.
 * Inserted at the end of every quiz (regardless of score).
 */
@Entity(tableName = "quiz_sessions")
data class QuizSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quizType: String,           // "class8.db", "class9.db", etc.
    val score: Int,
    val totalQuestions: Int,
    val elapsedTimeSeconds: Int,
    val hintsUsed: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val accuracyPercent: Float
        get() = if (totalQuestions > 0) (score.toFloat() / totalQuestions) * 100f else 0f

    val averageSecondsPerQuestion: Float
        get() = if (totalQuestions > 0) elapsedTimeSeconds.toFloat() / totalQuestions else 0f
}
