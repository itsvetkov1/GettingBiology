package com.znam.app

import com.znam.app.data.QuestionPerformance
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionPerformanceTest {
    private val oneDay = 24 * 60 * 60 * 1000L

    @Test
    fun computeNextInterval_usesPostAnswerConsecutiveCorrectSequence() {
        val expectedDays = listOf(1L, 3L, 7L, 14L, 30L, 60L)

        expectedDays.forEachIndexed { index, days ->
            val performance = QuestionPerformance(
                quizType = "class9.db",
                questionId = 1,
                consecutiveCorrect = index + 1
            )

            assertEquals(days * oneDay, performance.computeNextInterval(wasCorrect = true))
        }
    }

    @Test
    fun computeNextInterval_wrongAnswerSchedulesTwelveHourReviewAfterReset() {
        val performance = QuestionPerformance(
            quizType = "class9.db",
            questionId = 1,
            consecutiveCorrect = 0,
            consecutiveWrong = 1
        )

        assertEquals(oneDay / 2, performance.computeNextInterval(wasCorrect = false))
    }
}
