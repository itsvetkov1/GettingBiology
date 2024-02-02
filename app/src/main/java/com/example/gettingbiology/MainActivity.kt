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
    private lateinit var quizType: String
    private var answeredQuestionIds = arrayListOf<Int>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Retrieve the quiz type from intent or SharedPreferences as a fallback.
        quizType = intent.getStringExtra("QUIZ_TYPE")
            ?: getSharedPreferences("QuizPrefs", MODE_PRIVATE)
                .getString("LAST_QUIZ_TYPE", "default") ?: "default"

        val answeredQuestionIds = intent.getIntegerArrayListExtra("ANSWERED_QUESTION_IDS") ?: ArrayList()

        // Initialize the database with the retrieved quiz type.
        initializeDatabase(quizType, answeredQuestionIds)

        // Initialize user answers list.
        userAnswers = mutableListOf()




        // Initialize Mobile Ads.
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
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
                                // Optionally, reload the ad or handle ad dismissal.
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                Log.d(TAG, "Ad failed to show.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                Log.d(TAG, "Ad showed fullscreen content.")
                                mInterstitialAd =
                                    null // Ensure the reference is cleared once the ad is shown.
                            }
                        }
                    }
                    Log.d(TAG, "Ad was loaded.")
                }
            })

        // Initialize UI components.
        initializeComponents()

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


//    private fun initializeComponents() {
//        userAnswers = mutableListOf()
//        questionTextView = findViewById(R.id.question_text_view)
//        radioGroup = findViewById(R.id.options_radio_group)
//        submitButton = findViewById(R.id.submit_button)
//        hintText = findViewById(R.id.hint_text_view)
//    }


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


//    private fun initializeDatabase(quizType: String) {
//        val dbName = when (quizType) {
//            "class8.db" -> "class8.db"
//            "class9.db" -> "class9.db"
//            "class10.db" -> "class10.db"
//            "db_entrance_exam.db" -> "db_entrance_exam.db"
//            else -> "dbquestions.db" // Fallback to default database
//        }
//        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, dbName)
//            .createFromAsset(dbName)
//            .fallbackToDestructiveMigration()
//            .build()
//
//        fetchQuestions()
//
//        initializeComponents()
//
//    }


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
                hintText.setTextColor(Color.RED)
                hintText.visibility = View.VISIBLE
                return
            }

            val selectedOption =
                radioGroup.findViewById<RadioButton>(selectedOptionIndex).text.toString()
            userAnswers.add(selectedOption)

            if (questions[currentQuestionIndex].correctAnswer == selectedOption) {
                score++
                radioGroup.findViewById<RadioButton>(selectedOptionIndex).setTextColor(Color.GREEN)
            } else {
                radioGroup.children.forEach { button ->
                    if ((button as RadioButton).text == questions[currentQuestionIndex].correctAnswer) {
                        button.setTextColor(Color.GREEN)
                    }
                }
                radioGroup.findViewById<RadioButton>(selectedOptionIndex).setTextColor(Color.RED)
            }

            updateProgress(currentQuestionIndex, true) // Update progress here

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

