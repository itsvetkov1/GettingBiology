package com.znam.app

import com.znam.app.data.QuestionPerformance
import com.znam.app.data.QuestionPerformanceDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmartQuestionSelectorTest {
    private class FakeQuestionPerformanceDao(
        private val rows: MutableList<QuestionPerformance> = mutableListOf()
    ) : QuestionPerformanceDao {
        override suspend fun getPerformance(quizType: String, questionId: Int): QuestionPerformance? =
            rows.firstOrNull { it.quizType == quizType && it.questionId == questionId }

        override suspend fun getAllForQuizType(quizType: String): List<QuestionPerformance> =
            rows.filter { it.quizType == quizType }

        override suspend fun upsert(performance: QuestionPerformance) {
            rows.removeAll { it.quizType == performance.quizType && it.questionId == performance.questionId }
            rows.add(performance)
        }

        override suspend fun getDueForReview(quizType: String, nowMillis: Long): List<QuestionPerformance> =
            rows.filter { it.quizType == quizType && it.nextReviewAt <= nowMillis }
                .sortedWith(compareByDescending<QuestionPerformance> { it.difficultyScore }.thenBy { it.lastAnsweredAt })

        override suspend fun getWeakestQuestions(quizType: String, limit: Int): List<QuestionPerformance> =
            rows.filter { it.quizType == quizType && it.timesAnswered >= 2 }
                .sortedByDescending { it.difficultyScore }
                .take(limit)

        override suspend fun getStrongestQuestions(quizType: String, limit: Int): List<QuestionPerformance> =
            rows.filter { it.quizType == quizType && it.timesAnswered >= 2 }
                .sortedBy { it.difficultyScore }
                .take(limit)

        override suspend fun averageDifficulty(quizType: String): Float =
            rows.filter { it.quizType == quizType && it.timesAnswered > 0 }
                .map { it.difficultyScore }
                .average()
                .takeUnless { it.isNaN() }
                ?.toFloat() ?: 0.5f

        override suspend fun answeredQuestionCount(quizType: String): Int =
            rows.count { it.quizType == quizType && it.timesAnswered > 0 }
    }

    private fun questions(count: Int): List<Question> = (1..count).map {
        Question(id = it, questionText = "Q$it", options = "A;B;C;D", correctAnswer = "A")
    }

    @Test
    fun emptyHistory_selectsRequestedCount() = runBlocking {
        val selected = SmartQuestionSelector(FakeQuestionPerformanceDao())
            .selectQuestions(questions(20), "class9.db", count = 15)

        assertEquals(15, selected.size)
        assertEquals(15, selected.map { it.id }.toSet().size)
    }

    @Test
    fun immediateSecondQuiz_excludesRecentIdsWhenPoolIsLargeEnough() = runBlocking {
        val all = questions(30)
        val recentIds = (1..15).toSet()
        val future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        val history = recentIds.map { id ->
            QuestionPerformance(
                quizType = "class9.db",
                questionId = id,
                timesAnswered = 1,
                timesCorrect = 1,
                consecutiveCorrect = 1,
                nextReviewAt = future,
                difficultyScore = 0.2f
            )
        }.toMutableList()

        val selected = SmartQuestionSelector(FakeQuestionPerformanceDao(history))
            .selectQuestions(all, "class9.db", count = 15, excludeIds = recentIds)

        assertEquals(15, selected.size)
        assertTrue(selected.none { it.id in recentIds })
    }

    @Test
    fun tooSmallPool_usesExcludedQuestionsOnlyAsFallback() = runBlocking {
        val all = questions(5)
        val selected = SmartQuestionSelector(FakeQuestionPerformanceDao())
            .selectQuestions(all, "class9.db", count = 10, excludeIds = setOf(1, 2, 3, 4, 5))

        assertEquals(5, selected.size)
        assertEquals((1..5).toSet(), selected.map { it.id }.toSet())
    }

    @Test
    fun allFutureHistory_prefersUnexcludedQuestions() = runBlocking {
        val all = questions(8)
        val future = System.currentTimeMillis() + 60_000L
        val history = (1..8).map { id ->
            QuestionPerformance("class9.db", id, timesAnswered = 1, nextReviewAt = future)
        }.toMutableList()

        val selected = SmartQuestionSelector(FakeQuestionPerformanceDao(history))
            .selectQuestions(all, "class9.db", count = 4, excludeIds = setOf(1, 2, 3, 4))

        assertEquals(4, selected.size)
        assertFalse(selected.any { it.id in setOf(1, 2, 3, 4) })
    }
}
