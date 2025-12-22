package com.znam.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val startQuizButton = findViewById<Button>(R.id.startQuizButton)

        // Set the click listener for the button
        startQuizButton.setOnClickListener {
            navigateToQuiz()
        }
    }

    private fun navigateToQuiz() {
        val intent = Intent(this, SelectQuizActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
