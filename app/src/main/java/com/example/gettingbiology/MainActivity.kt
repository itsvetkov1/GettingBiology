package com.example.gettingbiology

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.room.Room
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var hintText: TextView
    private var currentQuestionIndex = 0
    private lateinit var questions: List<Question>
    private var score = 0
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        questionTextView = findViewById(R.id.question_text_view)
        radioGroup = findViewById(R.id.options_radio_group)
        submitButton = findViewById(R.id.submit_button)
        hintText = findViewById(R.id.hint_text_view)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "quiz-database")
            .createFromAsset("dbquestions.db")
            .build()

        fetchQuestions()
    }

    private fun fetchQuestions() {
        CoroutineScope(Dispatchers.IO).launch {
            val dbQuestions = db.questionDao().getAllQuestions()
            withContext(Dispatchers.Main) {
                if (dbQuestions.isNotEmpty()) {
                    questions = dbQuestions
                    loadQuestion()
                }
            }
        }
    }

    private fun loadQuestion() {
        if (currentQuestionIndex >= questions.size) {
            navigateToResultActivity()
            return
        }

        val question = questions[currentQuestionIndex]
        questionTextView.text = question.questionText
        radioGroup.removeAllViews()
        radioGroup.clearCheck()
        hintText.visibility = View.GONE

        question.options.split(";").forEachIndexed { index, option ->
            val radioButton = RadioButton(this).apply {
                text = option
                id = index
            }
            radioGroup.addView(radioButton)
        }

        submitButton.setOnClickListener { checkAnswer() }
    }

    private fun checkAnswer() {
        val selectedOptionIndex = radioGroup.checkedRadioButtonId
        if (selectedOptionIndex == -1) {
            hintText.text = "Please select an answer"
            hintText.setTextColor(Color.RED)
            hintText.visibility = View.VISIBLE
            return
        }

        hintText.visibility = View.GONE
        val correctAnswer = questions[currentQuestionIndex].correctAnswer
        val selectedOption = radioGroup.findViewById<RadioButton>(selectedOptionIndex).text.toString()

        if (correctAnswer == selectedOption) {
            score++
            radioGroup.findViewById<RadioButton>(selectedOptionIndex).setTextColor(Color.GREEN)
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
        finish()
    }
}
