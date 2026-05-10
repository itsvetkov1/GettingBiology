package com.znam.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.znam.app.ui.theme.ZnamTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.znam.app.ui.QuizScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Compose-based quiz activity. Hosts QuizScreen and manages
 * ad display + navigation to ResultActivity on completion.
 */
class ComposeQuizActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }


    companion object {
        private const val TAG = "ComposeQuizActivity"
    }

    private val quizViewModel: QuizViewModel by viewModel()
    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val quizType = intent.getStringExtra("QUIZ_TYPE")
            ?: getSharedPreferences("QuizPrefs", MODE_PRIVATE)
                .getString("LAST_QUIZ_TYPE", "default")
            ?: "default"

        // Preload interstitial ad
        loadInterstitialAd()

        // Initialize the ViewModel with the quiz type
        quizViewModel.initialize(quizType)

        setContent {
            ZnamTheme {
                QuizScreen(
                    viewModel = quizViewModel,
                    onNavigateToResults = { results ->
                        navigateToResults(results)
                    },
                    onShowInterstitialAd = { results ->
                        showInterstitialThenResults(results)
                    },
                    onNoQuestions = {
                        Toast.makeText(
                            this,
                            getString(R.string.no_questions_available),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                )
            }
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded")
                    interstitialAd = ad
                }
            }
        )
    }

    private fun showInterstitialThenResults(results: QuizResults) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    navigateToResults(results)
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    navigateToResults(results)
                }
            }
            ad.show(this)
        } else {
            navigateToResults(results)
        }
    }

    private fun navigateToResults(results: QuizResults) {
        val quizResult = QuizResult(
            score = results.score,
            questions = ArrayList(results.questions),
            userAnswers = ArrayList(results.userAnswers),
            elapsedTimeInSeconds = results.elapsedTimeSeconds
        )
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_QUIZ_RESULT, quizResult)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
