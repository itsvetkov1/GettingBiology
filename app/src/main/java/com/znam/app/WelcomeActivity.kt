package com.znam.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.znam.app.data.GamificationDao
import com.znam.app.data.UserProfile
import com.znam.app.ui.theme.ZnamTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class WelcomeActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private val gamificationDao: GamificationDao by inject()
    private val dailyChallengeManager: DailyChallengeManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        setupLanguageToggle()
        setupGamificationView()
        setupDailyChallengeButton()

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

    private fun setupGamificationView() {
        val composeView = findViewById<ComposeView>(R.id.welcome_gamification_view)

        composeView.setContent {
            ZnamTheme {
                WelcomeGamificationBar(gamificationDao = gamificationDao)
            }
        }
    }

    private fun setupDailyChallengeButton() {
        val button = findViewById<MaterialButton>(R.id.dailyChallengeButton)
        val challengeName = dailyChallengeManager.getTodaysChallengeName()
        val challengeType = dailyChallengeManager.getTodaysChallengeType()

        // Check if challenge is completed
        lifecycleScope.launch {
            val completed = withContext(Dispatchers.IO) {
                dailyChallengeManager.isTodaysChallengeCompleted()
            }
            if (completed) {
                button.text = "✅ $challengeName (Done!)"
                button.alpha = 0.7f
            } else {
                button.text = "⚡ $challengeName"
            }
        }

        button.setOnClickListener {
            val intent = Intent(this, ComposeQuizActivity::class.java).apply {
                putExtra("QUIZ_TYPE", challengeType)
                putExtra("IS_DAILY_CHALLENGE", true)
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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

/**
 * Composable bar showing streak + level on the welcome screen.
 * Only visible if the user has a profile (has played at least once).
 */
@Composable
private fun WelcomeGamificationBar(gamificationDao: GamificationDao) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        profile = try { gamificationDao.getProfile() } catch (e: Exception) { null }
    }

    val p = profile ?: return  // Hide if no profile

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎓", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Level ${p.level}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // XP
            Text(
                text = "${p.totalXp} XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Streak
            if (p.currentStreak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${p.currentStreak}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
