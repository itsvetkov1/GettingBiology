package com.example.gettingbiology

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.room.Room
import kotlinx.coroutines.*
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdError
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.util.ArrayList
import com.google.android.gms.ads.FullScreenContentCallback


class MainActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var hintText: TextView
    private var currentQuestionIndex = 0
    private lateinit var questions: List<Question>
    private var score = 0
    private lateinit var userAnswers: MutableList<String>
    private lateinit var db: AppDatabase
    private var mInterstitialAd: InterstitialAd? = null
    private final var TAG = "MainActivity"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userAnswers = mutableListOf()

        MobileAds.initialize(this) {}

        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError?.toString() ?: "Unknown error")
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // Code to be executed when the interstitial ad is dismissed.
                        Log.d(TAG, "Ad was dismissed.")
                        // Consider reloading the ad
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        // Code to be executed when the interstitial ad failed to show.
                        Log.d(TAG, "Ad failed to show.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        // Code to be executed when the interstitial ad is shown.
                        Log.d(TAG, "Ad showed fullscreen content.")
                        mInterstitialAd = null
                    }
                }

                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })

        questionTextView = findViewById(R.id.question_text_view)
        radioGroup = findViewById(R.id.options_radio_group)
        submitButton = findViewById(R.id.submit_button)
        hintText = findViewById(R.id.hint_text_view)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "quiz-database")
            .createFromAsset("dbquestions.db")
            .build()

        fetchQuestions()
    }




    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            val lastProgress = db.userProgressDao().getLastProgress()
            withContext(Dispatchers.Main) {
                // Use lastProgress to determine where to resume
                // Example: currentQuestionIndex = lastProgress?.questionId ?: 0
            }
        }
    }

    private fun updateProgress(questionId: Int, isCompleted: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            db.userProgressDao().insertProgress(UserProgress(questionId, isCompleted))
        }
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
        if (currentQuestionIndex >= questions.size || currentQuestionIndex >= 15) {
            navigateToResultActivity()
            return
        }

        val question = questions[currentQuestionIndex]
        questionTextView.apply {
            text = question.questionText
            setTextColor(Color.BLACK)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        radioGroup.removeAllViews()
        radioGroup.clearCheck()
        hintText.visibility = View.GONE

        question.options.split(";").forEachIndexed { index, option ->
            val radioButton = RadioButton(this).apply {
                text = option
                setTextColor(Color.BLACK)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                buttonDrawable = ContextCompat.getDrawable(context, R.drawable.radio_button_custom)
                id = index
            }
            radioGroup.addView(radioButton)
        }

        submitButton.setOnClickListener { checkAnswer() }
    }

    private fun checkAnswer() {
        val selectedOptionIndex = radioGroup.checkedRadioButtonId
        if (selectedOptionIndex == -1) {
            hintText.text = "Моля, изберете отговор!"
            hintText.setTextColor(Color.RED)
            hintText.visibility = View.VISIBLE
            return
        }

        val selectedOption = radioGroup.findViewById<RadioButton>(selectedOptionIndex).text.toString()
        userAnswers.add(selectedOption)

        hintText.visibility = View.GONE
        val correctAnswer = questions[currentQuestionIndex].correctAnswer


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

// ...

    private fun navigateToResultActivity() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Ad dismissed, proceed to result activity
                    proceedToResultActivity()
                }
                // Include other callback methods if needed, like onAdFailedToShowFullScreenContent
            }
            mInterstitialAd?.show(this)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            proceedToResultActivity()
        }
    }

    private fun proceedToResultActivity() {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("QUESTIONS", ArrayList(questions))
            putExtra("USER_ANSWERS", ArrayList(userAnswers))
        }
        startActivity(intent)
        finish()
    }
}
