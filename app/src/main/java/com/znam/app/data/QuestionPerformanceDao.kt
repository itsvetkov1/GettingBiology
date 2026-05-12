package com.znam.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface QuestionPerformanceDao {

    @Query("SELECT * FROM question_performance WHERE quizType = :quizType AND questionId = :questionId")
    suspend fun getPerformance(quizType: String, questionId: Int): QuestionPerformance?

    @Query("SELECT * FROM question_performance WHERE quizType = :quizType")
    suspend fun getAllForQuizType(quizType: String): List<QuestionPerformance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(performance: QuestionPerformance)

    /**
     * Questions due for review: nextReviewAt <= now.
     * Ordered by difficulty (hardest first) then by oldest review date.
     */
    @Query("""
        SELECT * FROM question_performance
        WHERE quizType = :quizType AND nextReviewAt <= :nowMillis
        ORDER BY difficultyScore DESC, lastAnsweredAt ASC
    """)
    suspend fun getDueForReview(quizType: String, nowMillis: Long): List<QuestionPerformance>

    /**
     * Get the user's weakest questions (highest difficulty score).
     */
    @Query("""
        SELECT * FROM question_performance
        WHERE quizType = :quizType AND timesAnswered >= 2
        ORDER BY difficultyScore DESC
        LIMIT :limit
    """)
    suspend fun getWeakestQuestions(quizType: String, limit: Int = 10): List<QuestionPerformance>

    /**
     * Get the user's strongest questions (lowest difficulty score).
     */
    @Query("""
        SELECT * FROM question_performance
        WHERE quizType = :quizType AND timesAnswered >= 2
        ORDER BY difficultyScore ASC
        LIMIT :limit
    """)
    suspend fun getStrongestQuestions(quizType: String, limit: Int = 10): List<QuestionPerformance>

    /**
     * Average difficulty across all answered questions for a quiz type.
     */
    @Query("""
        SELECT COALESCE(AVG(difficultyScore), 0.5)
        FROM question_performance
        WHERE quizType = :quizType AND timesAnswered > 0
    """)
    suspend fun averageDifficulty(quizType: String): Float

    /**
     * Count of questions that have been answered at least once.
     */
    @Query("SELECT COUNT(*) FROM question_performance WHERE quizType = :quizType AND timesAnswered > 0")
    suspend fun answeredQuestionCount(quizType: String): Int
}
