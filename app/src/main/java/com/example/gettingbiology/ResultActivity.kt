package com.example.gettingbiology

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Get score and questions from intent
        val score = intent.getIntExtra("SCORE", 0)
        val questions = intent.getSerializableExtra("QUESTIONS") as List<Question>

        val resultTextView = findViewById<TextView>(R.id.result_text_view)
        resultTextView.text = "Score: $score/${questions.size}"


    }
}
