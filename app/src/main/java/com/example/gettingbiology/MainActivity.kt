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

import com.google.android.gms.ads.AdView


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
    private lateinit var quizType: String
    private var answeredQuestionIds = arrayListOf<Int>()
    private lateinit var mAdView: AdView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Mobile Ads first to avoid any initialization delay later on
        MobileAds.initialize(this) {}

        // Retrieve the quiz type from intent or SharedPreferences as a fallback.
        quizType = intent.getStringExtra("QUIZ_TYPE") ?: getSharedPreferences("QuizPrefs", MODE_PRIVATE)
            .getString("LAST_QUIZ_TYPE", "default") ?: "default"

        // Load answeredQuestionIds from SharedPreferences
        val sharedPrefs = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
        val savedIds = sharedPrefs.getStringSet("AnsweredQuestionIds", null)
        answeredQuestionIds = if (savedIds != null) savedIds.map { it.toInt() }.toCollection(ArrayList()) else ArrayList()

        // Initialize the database with the retrieved quiz type and previously answered question IDs.
        initializeDatabase(quizType, answeredQuestionIds)

        // Initialize user answers list.
        userAnswers = mutableListOf()

        // Ad request and InterstitialAd load block.
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        InterstitialAd.load(
            this,
            "ca-app-pub-3551035007628625/7595976845",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString() ?: "Unknown error")
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd.apply {
                        fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Ad was dismissed.")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                Log.d(TAG, "Ad failed to show.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                Log.d(TAG, "Ad showed fullscreen content.")
                                mInterstitialAd = null // Ensure the reference is cleared once the ad is shown.
                            }
                        }
                    }
                    Log.d(TAG, "Ad was loaded.")
                }
            })



        // Initialize UI components.
        initializeComponents()
    }


    override fun onPause() {
        mAdView.pause()
        super.onPause()
    }



    override fun onDestroy() {
        mAdView.destroy()
        super.onDestroy()
    }



    private fun initializeComponents() {
        // Initialize your UI components here
        questionTextView = findViewById(R.id.question_text_view)
        radioGroup = findViewById(R.id.options_radio_group)
        submitButton = findViewById(R.id.submit_button)
        hintText = findViewById(R.id.hint_text_view)

        // If you have additional initialization logic, include it here
    }

    private fun initializeDatabase(quizType: String, answeredQuestionIds: ArrayList<Int>) {
        val dbName = when (quizType) {
            "class8.db" -> "class8.db"
            "class9.db" -> "class9.db"
            "class10.db" -> "class10.db"
            "db_entrance_exam.db" -> "db_entrance_exam.db"
            else -> "dbquestions.db" // Fallback to default database
        }
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, dbName)
            .createFromAsset(dbName)
            .fallbackToDestructiveMigration()
            .build()

        // Assuming fetchQuestions is correctly implemented to load questions from the database
        fetchQuestions(answeredQuestionIds)
    }
    private fun fetchQuestions(answeredQuestionIds: ArrayList<Int>) {
        CoroutineScope(Dispatchers.IO).launch {
            val allQuestions = db.questionDao().getAllQuestions()
            val filteredQuestions = allQuestions.filterNot { it.id in answeredQuestionIds }
            withContext(Dispatchers.Main) {
                questions = filteredQuestions
                // Proceed to load the first question or handle UI updates accordingly
                if (questions.isNotEmpty()) {
                    loadQuestion()
                }
            }
        }
    }


    private fun saveProgress() {
        val sharedPref = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putStringSet("AnsweredQuestionIds", answeredQuestionIds.map { it.toString() }.toSet())
            apply()
        }
    }



        private fun startNewQuiz() {
            // Reset the current question index to 0 for a new quiz
            currentQuestionIndex = 0

            // Optionally, reset userAnswers if you want a fresh start for answers
            userAnswers.clear()

            // Reload the questions from the database
            fetchQuestions(answeredQuestionIds)


            // Otherwise, keep the score as is, assuming you're tracking progress across quizzes

            // Update UI or SharedPreferences as needed to reflect the new quiz state
            val sharedPref = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("NewQuizStarted", true)
                apply()
            }

            // Load the first question of the new quiz
            loadQuestion()
        }





    override fun onResume() {
        super.onResume()
        mAdView.resume()

        // Retrieve the potentially updated quiz type from SharedPreferences.
        val sharedPref = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
        val newQuizType = sharedPref.getString("LAST_QUIZ_TYPE", "default") ?: "default"

        // Check if the quiz type has changed since the last time the activity was paused.
        if (newQuizType != quizType) {
            quizType = newQuizType

            // Reinitialize the database only if the quiz type has changed.
            initializeDatabase(quizType, answeredQuestionIds)
        }

        // Load or reload questions and progress only if needed.
        // If the database was reinitialized (meaning the quiz type changed),
        // it's necessary to reload questions and potentially update the UI based on new progress.
        // Otherwise, this step can be skipped to avoid redundant operations.
        CoroutineScope(Dispatchers.IO).launch {
            val lastProgress = db.userProgressDao().getLastProgress()
            withContext(Dispatchers.Main) {
                if (lastProgress != null) {
                    currentQuestionIndex = lastProgress.questionId
                    // Assuming `fetchQuestions` and `loadQuestion` correctly use `currentQuestionIndex`
                    // and `answeredQuestionIds`, respectively.
                    fetchQuestions(answeredQuestionIds)  // Ensure questions are re-fetched considering exclusions.
                } else {
                    // If there's no progress to resume from, you might want to reset to the first question
                    // or handle this scenario appropriately.
                    currentQuestionIndex = 0
                    fetchQuestions(answeredQuestionIds)  // Fetch questions considering exclusions even if starting fresh.
                }
            }
        }
    }



        private fun updateProgress(questionId: Int, isCompleted: Boolean) {
            CoroutineScope(Dispatchers.IO).launch {
                db.userProgressDao().insertProgress(UserProgress(questionId, isCompleted))
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
                    buttonDrawable =
                        ContextCompat.getDrawable(context, R.drawable.radio_button_custom)
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
            hintText.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_white)) // Assuming this color is defined
            hintText.visibility = View.VISIBLE
            return
        }

        val selectedOption = radioGroup.findViewById<RadioButton>(selectedOptionIndex).text.toString()
        val correctAnswer = questions[currentQuestionIndex].correctAnswer
        userAnswers.add(selectedOption)
        answeredQuestionIds.add(questions[currentQuestionIndex].id)
        saveProgress()

        val isCorrect = selectedOption == correctAnswer
        if (isCorrect) {
            score++
        }
        Log.d("ScoreUpdate", "Current score: $score") // Log to verify score increment

        // Reset backgrounds and update accordingly
        radioGroup.children.forEach { child ->
            if (child is RadioButton) {
                val layoutParams = child.layoutParams as RadioGroup.LayoutParams
                layoutParams.width = RadioGroup.LayoutParams.MATCH_PARENT
                child.layoutParams = layoutParams

                child.background = if (child.text == correctAnswer) {
                    ContextCompat.getDrawable(this, R.drawable.correct_answer_background)
                } else if (!isCorrect && child.text == selectedOption) {
                    ContextCompat.getDrawable(this, R.drawable.incorrect_answer_background)
                } else {
                    null // No background for unselected options
                }
                child.setTextColor(Color.BLACK) // Keep text color unchanged
            }
        }

        updateProgress(currentQuestionIndex, true)

        if (currentQuestionIndex < questions.size - 1) {
            currentQuestionIndex++
            radioGroup.postDelayed({ loadQuestion() }, 2000)
        } else {
            navigateToResultActivity()
        }
    }




    private fun navigateToResultActivity() {
        // Populate QuizResultsHolder with the current quiz results
        QuizResultsHolder.score = score
        QuizResultsHolder.questions = questions
        // Convert MutableList to ArrayList
        QuizResultsHolder.userAnswers = ArrayList(userAnswers)

        // Navigate to ResultActivity
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Ad dismissed, proceed to result activity
                    proceedToResultActivity()
                }
                // Include other callback methods if needed
            }
            mInterstitialAd?.show(this)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            proceedToResultActivity()
        }
    }

    private fun proceedToResultActivity() {
        // Intent to launch ResultActivity without passing quiz results as extras
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
        finish()
    }
}


