package com.znam.app

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class QuizManagerTest {
    
    private lateinit var questions: List<Question>
    private lateinit var quizManager: QuizManager
    
    @Before
    fun setup() {
        questions = listOf(
            Question(1, "Q1", "A;B;C;D", "A"),
            Question(2, "Q2", "A;B;C;D", "B"),
            Question(3, "Q3", "A;B;C;D", "C")
        )
        quizManager = QuizManager(questions)
    }

    @Test
    fun initialState_isCorrect() {
        assertEquals(0, quizManager.score)
        assertEquals("1/15", quizManager.getCurrentProgress())
        assertFalse(quizManager.isQuizFinished())
    }

    @Test
    fun submitAnswer_correct_incrementsScore() {
        val isCorrect = quizManager.submitAnswer("A")
        assertTrue(isCorrect)
        assertEquals(1, quizManager.score)
    }

    @Test
    fun submitAnswer_incorrect_doesNotIncrementScore() {
        val isCorrect = quizManager.submitAnswer("B") // Correct is A
        assertFalse(isCorrect)
        assertEquals(0, quizManager.score)
    }
    
    @Test
    fun submitAnswer_advancesQuestion_onlyWhenExplicit() {
        quizManager.submitAnswer("A")
        // Should still be on Q1
        assertEquals("1/15", quizManager.getCurrentProgress())
        assertEquals(questions[0], quizManager.getCurrentQuestion())
        
        quizManager.advanceToNextQuestion()
        // Now on Q2
        assertEquals("2/15", quizManager.getCurrentProgress())
        assertEquals(questions[1], quizManager.getCurrentQuestion())
    }
    
    @Test
    fun quizFinishes_afterLastQuestion() {
        // Answer all 3 questions
        quizManager.submitAnswer("A")
        quizManager.advanceToNextQuestion()
        quizManager.submitAnswer("B")
        quizManager.advanceToNextQuestion()
        quizManager.submitAnswer("C")
        quizManager.advanceToNextQuestion()
        
        assertTrue(quizManager.isQuizFinished())
        assertNull(quizManager.getCurrentQuestion())
    }
}
