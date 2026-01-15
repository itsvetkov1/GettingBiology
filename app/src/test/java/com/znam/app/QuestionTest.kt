package com.znam.app

import org.junit.Test
import org.junit.Assert.*

class QuestionTest {
    @Test
    fun getParsedOptions_splitsCorrectly() {
        val question = Question(1, "Text", "A;B;C;D", "A")
        val options = question.getParsedOptions()
        assertEquals(4, options.size)
        assertEquals("A", options[0])
        assertEquals("D", options[3])
    }

    @Test
    fun getParsedOptions_trimsWhitespace() {
        val question = Question(1, "Text", " A ; B ; C ", "A")
        val options = question.getParsedOptions()
        assertEquals(3, options.size)
        assertEquals("A", options[0])
        assertEquals("B", options[1])
        assertEquals("C", options[2])
    }
    
    @Test
    fun getParsedOptions_filtersEmpty() {
        val question = Question(1, "Text", "A;;B", "A")
        val options = question.getParsedOptions()
        assertEquals(2, options.size)
        assertEquals("A", options[0])
        assertEquals("B", options[1]) 
    }

    @Test
    fun hasHints_returnsCorrectValue() {
        val noHints = Question(1, "Text", "A;B", "A")
        assertFalse(noHints.hasHints())

        val oneHint = Question(1, "Text", "A;B", "A", hint1 = "Hint 1")
        assertTrue(oneHint.hasHints())

        val twoHints = Question(1, "Text", "A;B", "A", hint1 = "Hint 1", hint2 = "Hint 2")
        assertTrue(twoHints.hasHints())

        val emptyHint = Question(1, "Text", "A;B", "A", hint1 = "")
        assertFalse(emptyHint.hasHints())
    }
}
