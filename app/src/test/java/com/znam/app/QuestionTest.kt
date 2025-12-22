package com.znam.app

import org.junit.Test
import org.junit.Assert.*
import android.os.Parcel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
}
