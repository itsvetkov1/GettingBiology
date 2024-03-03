package com.znam.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val score = QuizResultsHolder.score
        val questions = QuizResultsHolder.questions
        val userAnswers = QuizResultsHolder.userAnswers

        val resultTextView = findViewById<TextView>(R.id.result_text_view).apply {
            text = "Резултат: $score/15"
            setBackgroundColor(ContextCompat.getColor(this@ResultActivity, R.color.transparent_white))
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        val questionsLayout = findViewById<LinearLayout>(R.id.questions_layout)



        questions.forEachIndexed { index, question ->
            if (index < 15) {
                val userAnswer = userAnswers.getOrNull(index) ?: "Question Skipped"
                Log.d("QuizDebug", "Displaying Question ${index + 1}: Correct Answer: ${question.correctAnswer}, User Answer: $userAnswer")
                questionsLayout.addView(createQuestionView(question, userAnswer))
            }
        }


        findViewById<Button>(R.id.restart_quiz_button).setOnClickListener {
            QuizResultsHolder.clear() // Clear the results before starting a new quiz
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Finish ResultActivity to remove it from the back stack
        }
    }

    private fun createQuestionView(question: Question, userAnswer: String?): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                (it as LinearLayout.LayoutParams).bottomMargin = 30
            }
        }

        val questionTextView = TextView(this).apply {
            text = question.questionText
            setTextColor(Color.BLACK)
            textSize = 18f
            typeface = Typeface.create("", Typeface.BOLD)
            setPadding(16, 16, 16, 16) // Left, Top, Right, Bottom padding
            setBackgroundColor(Color.parseColor("#D3D3D3"))
        }

        val correctAnswerTextView = TextView(this).apply {
            text = "Правилен отговор: ${question.correctAnswer}"
            setTextColor(Color.BLACK)
            textSize = 18f
            typeface = Typeface.create("", Typeface.BOLD)
            setPadding(16, 16, 16, 16) // Left, Top, Right, Bottom padding
            setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        layout.addView(questionTextView)
        layout.addView(correctAnswerTextView)


        if (userAnswer != question.correctAnswer) {
            val userAnswerTextView = TextView(this).apply {
                text = if (userAnswer == "SKIPPED") {
                    "Въпросът е пропуснат."
                } else {
                    "Вашият отговор: $userAnswer"
                }
                setTextColor(Color.BLACK)
                textSize = 18f
                typeface = Typeface.create("", Typeface.BOLD)
                setPadding(16, 16, 16, 16) // Left, Top, Right, Bottom padding
                setBackgroundColor(if (userAnswer == null || question.correctAnswer == userAnswer) Color.TRANSPARENT else Color.parseColor("#F44336"))
            }
            layout.addView(userAnswerTextView) // Add this view only if the condition above is met
        }
        return layout
    }
}