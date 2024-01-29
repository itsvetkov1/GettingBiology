package com.example.gettingbiology

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val score = intent.getIntExtra("SCORE", 0)
        val questions = intent.getSerializableExtra("QUESTIONS") as List<Question>
        val userAnswers = intent.getStringArrayListExtra("USER_ANSWERS") as List<String>

        val resultTextView = findViewById<TextView>(R.id.result_text_view)
        resultTextView.text = "Score: $score/${questions.size}"

        val questionsLayout = findViewById<LinearLayout>(R.id.questions_layout)

        questions.forEachIndexed { index, question ->
            if (question.correctAnswer != userAnswers[index]) {
                questionsLayout.addView(createQuestionView(question, userAnswers[index]))
            }
        }
        val restartButton = findViewById<Button>(R.id.restart_quiz_button)
        restartButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun createQuestionView(question: Question, userAnswer: String): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 30 // Add bottom margin for spacing between blocks
        }


        val questionTextView = TextView(this)
        questionTextView.text = question.questionText
        questionTextView.setTextColor(Color.BLACK)
        questionTextView.textSize = 18f // Increase text size
        questionTextView.setTypeface(null, Typeface.BOLD) // Set text to bold
        questionTextView.setPadding(16)
        questionTextView.setBackgroundColor(Color.parseColor("#D3D3D3"))
        questionTextView.setBackgroundColor(Color.parseColor("#D3D3D3"))

        val correctAnswerTextView = TextView(this)
        correctAnswerTextView.text = "Правилен отговор: ${question.correctAnswer}"
        correctAnswerTextView.setTextColor(Color.BLACK) // Set text color
        correctAnswerTextView.textSize = 18f // Increase text size
        correctAnswerTextView.setTypeface(null, Typeface.BOLD) // Set text to bold
        correctAnswerTextView.setBackgroundColor(Color.parseColor("#4CAF50")) // Always green for correct answer
        correctAnswerTextView.setPadding(16)
        correctAnswerTextView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val userAnswerTextView = TextView(this)
        userAnswerTextView.text = "Вашият отговор: $userAnswer"
        userAnswerTextView.setTextColor(Color.BLACK)
        userAnswerTextView.textSize = 18f // Increase text size
        userAnswerTextView.setTypeface(null, Typeface.BOLD) // Set text to bold
        userAnswerTextView.setBackgroundColor(if (question.correctAnswer != userAnswer) Color.parseColor("#F44336") else Color.TRANSPARENT)
        userAnswerTextView.setPadding(16)
        userAnswerTextView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        layout.addView(questionTextView)
        layout.addView(correctAnswerTextView)
        layout.addView(userAnswerTextView)

        return layout
    }


}
