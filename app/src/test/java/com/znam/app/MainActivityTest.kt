package com.znam.app

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MainActivityTest {

    @Test
    fun testHintButtonFlow() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Manually inject a question with hints to test the UI logic
                val testQuestion = Question(
                    id = 1,
                    questionText = "What is biology?",
                    options = "Study of life;Study of rocks;Study of stars;Study of numbers",
                    correctAnswer = "Study of life",
                    hint1 = "It starts with 'Study of l...'",
                    hint2 = "It's about living things."
                )
                
                // Use reflection to set private fields for testing
                val questionsField = activity.javaClass.getDeclaredField("questions")
                questionsField.isAccessible = true
                questionsField.set(activity, listOf(testQuestion))
                
                val currentQuestionIndexField = activity.javaClass.getDeclaredField("currentQuestionIndex")
                currentQuestionIndexField.isAccessible = true
                currentQuestionIndexField.set(activity, 0)

                // Trigger loadQuestion to update UI with our test question
                val loadQuestionMethod = activity.javaClass.getDeclaredMethod("loadQuestion")
                loadQuestionMethod.isAccessible = true
                loadQuestionMethod.invoke(activity)

                val hintButton = activity.findViewById<MaterialCardView>(R.id.hintButton)
                val hintBubblesContainer = activity.findViewById<View>(R.id.hintBubblesContainer)
                val hint1Bubble = activity.findViewById<View>(R.id.hint1Bubble)
                val hint2Bubble = activity.findViewById<View>(R.id.hint2Bubble)
                val hint1Text = activity.findViewById<TextView>(R.id.hintText) // This might find the first one

                // Initially, bubbles should be hidden
                assertEquals(View.GONE, hintBubblesContainer.visibility)
                
                // Click hint button for first hint
                hintButton.performClick()
                
                assertEquals(View.VISIBLE, hintBubblesContainer.visibility)
                assertEquals(View.VISIBLE, hint1Bubble.visibility)
                assertEquals(View.GONE, hint2Bubble.visibility)
                
                // Click hint button for second hint
                hintButton.performClick()
                
                assertEquals(View.VISIBLE, hint1Bubble.visibility)
                assertEquals(View.VISIBLE, hint2Bubble.visibility)
                
                // Button should now be disabled (alpha 0.5f)
                assertFalse(hintButton.isEnabled)
                assertEquals(0.5f, hintButton.alpha, 0.01f)
            }
        }
    }
}
