package com.znam.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Enable edge-to-edge display and handle system window insets
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val quizResult = readQuizResult() ?: run {
            Log.e("QuizDebug", "Missing quiz result extra")
            finish()
            return
        }
        val score = quizResult.score
        val questions = quizResult.questions
        val userAnswers = quizResult.userAnswers
        val elapsedTimeInSeconds = quizResult.elapsedTimeInSeconds

        // Format the elapsed time
        val minutes = elapsedTimeInSeconds / 60
        val seconds = elapsedTimeInSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        findViewById<TextView>(R.id.result_text_view).apply {
            val fullText = "Резултат: $score/15"
            val spannable = SpannableString(fullText)
            val tealColor = ContextCompat.getColor(this@ResultActivity, R.color.md_theme_light_primary)
            val scoreStart = fullText.indexOf("$score/15")
            if (scoreStart != -1) {
                spannable.setSpan(
                    ForegroundColorSpan(tealColor),
                    scoreStart,
                    fullText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            text = spannable
            alpha = 0f
            animate().alpha(1f).setDuration(1000).start()
        }

        findViewById<TextView>(R.id.time_text_view).apply {
            text = "Време: $timeString"
            alpha = 0f
            animate().alpha(1f).setDuration(1000).start()
        }

        val questionsLayout = findViewById<LinearLayout>(R.id.questions_layout)

        questions.forEachIndexed { index, question ->
            if (index < 15) {
                val userAnswer = userAnswers.getOrNull(index) ?: "Question Skipped"
                Log.d("QuizDebug", "Displaying Question ${index + 1}: Correct Answer: ${question.correctAnswer}, User Answer: $userAnswer")
                questionsLayout.addView(createQuestionView(questionsLayout, question, userAnswer))
            }
        }


        findViewById<Button>(R.id.restart_quiz_button).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // Finish ResultActivity to remove it from the back stack
        }
    }

    private fun readQuizResult(): QuizResult? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_QUIZ_RESULT, QuizResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_QUIZ_RESULT)
        }
    }

    companion object {
        const val EXTRA_QUIZ_RESULT = "com.znam.app.EXTRA_QUIZ_RESULT"
    }

    private fun createQuestionView(parent: ViewGroup, question: Question, userAnswer: String?): View {
        val view = layoutInflater.inflate(R.layout.item_question_result, parent, false)
        
        val questionTextView = view.findViewById<TextView>(R.id.tv_question_text)
        val correctAnswerTextView = view.findViewById<TextView>(R.id.tv_correct_answer)
        val userAnswerTextView = view.findViewById<TextView>(R.id.tv_user_answer)

        questionTextView.text = question.questionText
        correctAnswerTextView.text = "Правилен отговор: ${question.correctAnswer}"

        val isCorrect = userAnswer?.trim()?.equals(question.correctAnswer.trim(), ignoreCase = true) == true

        if (!isCorrect) {
            userAnswerTextView.visibility = View.VISIBLE
            userAnswerTextView.text = if (userAnswer == "Въпросът е пропуснат.") {
                "Въпросът е пропуснат."
            } else {
                "Вашият отговор: $userAnswer"
            }
        } else {
            userAnswerTextView.visibility = View.GONE
        }
        
        return view
    }
}