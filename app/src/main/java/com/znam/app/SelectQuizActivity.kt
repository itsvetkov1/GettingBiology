package com.znam.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SelectQuizActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_quiz)

        // Enable edge-to-edge display and handle system window insets
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        findViewById<Button>(R.id.btnClass8).setOnClickListener {
            startQuiz("class8.db")
        }
        findViewById<Button>(R.id.btnClass9).setOnClickListener {
            startQuiz("class9.db")
        }
        findViewById<Button>(R.id.btnClass10).setOnClickListener {
            startQuiz("class10.db")
        }
        findViewById<Button>(R.id.btnEntranceExam).setOnClickListener {
            startQuiz("db_entrance_exam.db")
        }
    }

    private fun startQuiz(databaseName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("QUIZ_TYPE", databaseName)
        }
        // Save quiz type to SharedPreferences
        val sharedPref = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("LAST_QUIZ_TYPE", databaseName)
            apply()
        }
        startActivity(intent)
    }

}
