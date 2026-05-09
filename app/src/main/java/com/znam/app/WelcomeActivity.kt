package com.znam.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        findViewById<MaterialButton>(R.id.startQuizButton).setOnClickListener {
            navigateToQuiz()
        }

        findViewById<MaterialButton>(R.id.statsButton).setOnClickListener {
            navigateToStats()
        }
    }

    private fun navigateToQuiz() {
        val intent = Intent(this, SelectQuizActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToStats() {
        val intent = Intent(this, ComposeStatsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
