
    package com.example.gettingbiology

    import android.content.Intent
    import android.graphics.Color
    import android.os.Bundle
    import android.widget.*
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.children
    import android.view.View

    class MainActivity : AppCompatActivity() {

        private lateinit var questionTextView: TextView
        private lateinit var radioGroup: RadioGroup
        private lateinit var submitButton: Button
        private var currentQuestionIndex = 0
        private val questions = QuizData.getRandomQuestions()
        private var score = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            questionTextView = findViewById(R.id.question_text_view)
            radioGroup = findViewById(R.id.options_radio_group)
            submitButton = findViewById(R.id.submit_button)

            loadQuestion()
            submitButton.setOnClickListener { checkAnswer() }
        }

        private fun loadQuestion() {
            if (currentQuestionIndex < questions.size) {
                val question = questions[currentQuestionIndex]
                questionTextView.text = question.questionText
                radioGroup.removeAllViews()

                question.options.forEachIndexed { index, option ->
                    val radioButton = RadioButton(this)
                    radioButton.text = option
                    radioButton.id = index
                    radioGroup.addView(radioButton)
                }
            } else {
                // Navigate to ResultActivity
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SCORE", score)
                    putExtra("QUESTIONS", ArrayList(questions))
                }
                startActivity(intent)
            }
        }

        private fun checkAnswer() {
            val selectedOptionIndex = radioGroup.checkedRadioButtonId
            if (selectedOptionIndex == -1) {
                // No option selected, show hint
                // Assuming you have a TextView with the ID hint_text_view for the hint
                val hintText = findViewById<TextView>(R.id.hint_text_view)
                hintText.text = "*Моля изберете отговор!"
                hintText.setTextColor(Color.RED)
                hintText.visibility = View.VISIBLE
            } else {
                // Hide the hint as the user has selected an option
                findViewById<TextView>(R.id.hint_text_view).visibility = View.GONE

                val correctAnswer = questions[currentQuestionIndex].correctAnswer
                val selectedOption =
                    radioGroup.findViewById<RadioButton>(selectedOptionIndex).text.toString()

                if (correctAnswer == selectedOption) {
                    score++
                    radioGroup.findViewById<RadioButton>(selectedOptionIndex)
                        .setTextColor(Color.GREEN)
                } else {
                    radioGroup.children.forEach { button ->
                        if ((button as RadioButton).text == correctAnswer) {
                            button.setTextColor(Color.GREEN)
                        }
                    }
                    radioGroup.findViewById<RadioButton>(selectedOptionIndex)
                        .setTextColor(Color.RED)
                }

                // Move to next question or finish quiz
                currentQuestionIndex++
                if (currentQuestionIndex < questions.size) {
                    radioGroup.postDelayed({ loadQuestion() }, 2000)
                } else {
                    // Navigate to ResultActivity
                    val intent = Intent(this, ResultActivity::class.java).apply {
                        putExtra("SCORE", score)
                        putExtra("QUESTIONS", ArrayList(questions))
                    }
                    startActivity(intent)
                }
            }
        }
    }
