package com.example.gettingbiology

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children

class MainActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var hintText: TextView
    private var currentQuestionIndex = 0
    private val questions = QuizData.getRandomQuestions()
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        questionTextView = findViewById(R.id.question_text_view)
        radioGroup = findViewById(R.id.options_radio_group)
        submitButton = findViewById(R.id.submit_button)
        hintText = findViewById(R.id.hint_text_view)

        loadQuestion()
        submitButton.setOnClickListener { checkAnswer() }
    }

    private fun loadQuestion() {
        if (currentQuestionIndex >= questions.size) {
            navigateToResultActivity()
            return
        }

        val question = questions[currentQuestionIndex]
        questionTextView.text = question.questionText
        radioGroup.removeAllViews()
        radioGroup.clearCheck()  // Clear any selection
        hintText.visibility = View.GONE  // Reset/hide the hint text

        question.options.forEachIndexed { index, option ->
            val radioButton = RadioButton(this)
            radioButton.text = option
            radioButton.id = index
            radioGroup.addView(radioButton)
        }
    }

    private fun checkAnswer() {
        val selectedOptionIndex = radioGroup.checkedRadioButtonId

        if (selectedOptionIndex == -1) {
            hintText.text = "*Моля изберете отговор!"
            hintText.setTextColor(Color.RED)
            hintText.visibility = View.VISIBLE
            return
        }

        hintText.visibility = View.GONE
        val correctAnswer = questions[currentQuestionIndex].correctAnswer
        val selectedOption =
            radioGroup.findViewById<RadioButton>(selectedOptionIndex).text.toString()

        if (correctAnswer == selectedOption) {
            score++
            radioGroup.findViewById<RadioButton>(
                selectedOptionIndex
            ).setTextColor(Color.GREEN)
        } else {
            radioGroup.children.forEach { button ->
                if ((button as RadioButton).text == correctAnswer) {
                    button.setTextColor(Color.GREEN)
                }
            }
            radioGroup.findViewById<RadioButton>(selectedOptionIndex).setTextColor(Color.RED)
        }
        if (currentQuestionIndex < questions.size - 1) {
            currentQuestionIndex++
            radioGroup.postDelayed({ loadQuestion() }, 2000)
        } else {
            navigateToResultActivity()
        }
    }

    private fun navigateToResultActivity() {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("QUESTIONS", ArrayList(questions))
        }
        startActivity(intent)
        finish() // Optionally, finish MainActivity
    }
}