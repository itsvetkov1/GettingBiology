package com.znam.app

import org.junit.Test
import org.junit.Assert.*

class QuizResultsHolderTest {

    @Test
    fun singleton_retainsData() {
        QuizResultsHolder.score = 10
        QuizResultsHolder.questions = listOf(Question(1, "Q", "O", "A"))
        QuizResultsHolder.userAnswers.add("Answer")

        assertEquals(10, QuizResultsHolder.score)
        assertEquals(1, QuizResultsHolder.questions.size)
        assertEquals("Answer", QuizResultsHolder.userAnswers[0])
    }

    @Test
    fun clear_resetsAllData() {
        // Set data
        QuizResultsHolder.score = 5
        QuizResultsHolder.questions = listOf(Question(1, "Q", "O", "A"))
        QuizResultsHolder.userAnswers.add("A")
        
        // Clear
        QuizResultsHolder.clear()
        
        // Check reset
        assertEquals(0, QuizResultsHolder.score)
        assertTrue(QuizResultsHolder.questions.isEmpty())
        assertTrue(QuizResultsHolder.userAnswers.isEmpty())
    }
}
