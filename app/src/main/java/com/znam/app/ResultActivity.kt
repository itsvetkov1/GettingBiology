package com.znam.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Use QuizResultsHolder to get the data
        val score = QuizResultsHolder.score
        val questions = QuizResultsHolder.questions
        val userAnswers = QuizResultsHolder.userAnswers

        val resultTextView = findViewById<TextView>(R.id.result_text_view)
        resultTextView.text = "Резултат: $score/15"
        resultTextView.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_white))
        resultTextView.setTextColor(Color.BLACK)
        resultTextView.setTypeface(null, Typeface.BOLD)

        val questionsLayout = findViewById<LinearLayout>(R.id.questions_layout)

        questions.forEachIndexed { index, question ->
            if (index < userAnswers.size) {
                questionsLayout.addView(createQuestionView(question, userAnswers[index]))
            }
        }

        val restartButton = findViewById<Button>(R.id.restart_quiz_button)
        restartButton.setOnClickListener {
            QuizResultsHolder.clear() // Clear the results before starting a new quiz
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Finish ResultActivity to remove it from the back stack
        }
    }

    private fun createQuestionView(question: Question, userAnswer: String): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 30
            }
        }

        val questionTextView = TextView(this).apply {
            text = question.questionText
            setTextColor(Color.BLACK)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(16)
            setBackgroundColor(Color.parseColor("#D3D3D3"))
        }

        val correctAnswerTextView = TextView(this).apply {
            text = "Правилен отговор: ${question.correctAnswer}"
            setTextColor(Color.BLACK)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(16)
            setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        val userAnswerTextView = TextView(this).apply {
            text = "Вашият отговор: $userAnswer"
            setTextColor(Color.BLACK)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(16)
            setBackgroundColor(if (question.correctAnswer == userAnswer) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        }

        layout.addView(questionTextView)
        layout.addView(correctAnswerTextView)
        layout.addView(userAnswerTextView)

        return layout
    }
}
