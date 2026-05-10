package com.znam.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * DAO for quiz session history — powering the Statistics dashboard.
 */
@Dao
interface StatsDao {

    @Insert
    suspend fun insertSession(session: QuizSession): Long

    // ── Lifetime aggregates ────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM quiz_sessions")
    suspend fun totalSessions(): Int

    @Query("SELECT COALESCE(SUM(totalQuestions), 0) FROM quiz_sessions")
    suspend fun totalQuestionsAnswered(): Int

    @Query("SELECT COALESCE(SUM(score), 0) FROM quiz_sessions")
    suspend fun totalCorrectAnswers(): Int

    @Query("""
        SELECT COALESCE(
            CAST(SUM(score) AS FLOAT) / NULLIF(SUM(totalQuestions), 0) * 100,
            0
        ) FROM quiz_sessions
    """)
    suspend fun overallAccuracyPercent(): Float

    @Query("SELECT COALESCE(SUM(elapsedTimeSeconds), 0) FROM quiz_sessions")
    suspend fun totalTimeSeconds(): Int

    // ── Per-category stats ─────────────────────────────────────────

    @Query("""
        SELECT quizType,
               COUNT(*) as sessions,
               SUM(totalQuestions) as questions,
               SUM(score) as correct,
               CAST(SUM(score) AS FLOAT) / NULLIF(SUM(totalQuestions), 0) * 100 as accuracy
        FROM quiz_sessions
        GROUP BY quizType
        ORDER BY quizType
    """)
    suspend fun statsByCategory(): List<CategoryStats>

    // ── Streaks & bests ────────────────────────────────────────────

    @Query("SELECT COALESCE(MAX(score), 0) FROM quiz_sessions")
    suspend fun bestScore(): Int

    @Query("""
        SELECT COALESCE(MAX(CAST(score AS FLOAT) / NULLIF(totalQuestions, 0) * 100), 0)
        FROM quiz_sessions
    """)
    suspend fun bestAccuracyPercent(): Float

    // ── Recent sessions (for trend chart) ──────────────────────────

    @Query("""
        SELECT * FROM quiz_sessions
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun recentSessions(limit: Int = 20): List<QuizSession>

    // ── Weekly activity (sessions per day for last 7 days) ──────────

    @Query("""
        SELECT COUNT(*) FROM quiz_sessions
        WHERE timestamp >= :sinceMillis
    """)
    suspend fun sessionsAfter(sinceMillis: Long): Int
}

/**
 * Projection for per-category aggregate stats.
 */
data class CategoryStats(
    val quizType: String,
    val sessions: Int,
    val questions: Int,
    val correct: Int,
    val accuracy: Float
)
