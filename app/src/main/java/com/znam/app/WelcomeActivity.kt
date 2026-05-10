package com.znam.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        setupLanguageToggle()

        findViewById<MaterialButton>(R.id.startQuizButton).setOnClickListener {
            navigateToQuiz()
        }

        findViewById<MaterialButton>(R.id.statsButton).setOnClickListener {
            navigateToStats()
        }
    }

    private fun setupLanguageToggle() {
        val toggleButton = findViewById<MaterialButton>(R.id.languageToggleButton)
        val currentLanguage = LocaleHelper.getSavedLanguage(this)
        toggleButton.text = getString(
            if (currentLanguage == LocaleHelper.LANGUAGE_BG) {
                R.string.language_toggle_to_en
            } else {
                R.string.language_toggle_to_bg
            }
        )
        toggleButton.setOnClickListener {
            val nextLanguage = if (LocaleHelper.getSavedLanguage(this) == LocaleHelper.LANGUAGE_BG) {
                LocaleHelper.LANGUAGE_EN
            } else {
                LocaleHelper.LANGUAGE_BG
            }
            LocaleHelper.setSavedLanguage(this, nextLanguage)
            recreate()
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
