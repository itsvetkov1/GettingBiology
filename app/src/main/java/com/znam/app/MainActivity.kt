package com.znam.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.room.Room
import kotlinx.coroutines.*
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.gms.ads.AdView

class MainActivity : AppCompatActivity() {

    // Constants
    private val TAG = "MainActivity"

    // UI Components
    private lateinit var questionTextView: TextView
    private lateinit var questionCounterTextView: TextView
    private lateinit var hintTextView: TextView
    private lateinit var option1Button: Button
    private lateinit var option2Button: Button
    private lateinit var option3Button: Button
    private lateinit var option4Button: Button
    private lateinit var nextButton: Button
    private lateinit var mAdView: AdView

    // Quiz Data
    private var currentQuestionIndex = 0
    private lateinit var questions: List<Question>
    private var score = 0
    private lateinit var userAnswers: MutableList<String>
    private var selectedOption = -1

    // Database and Ads
    private lateinit var db: AppDatabase
    private var mInterstitialAd: InterstitialAd? = null

    // Consent Information
    private lateinit var consentInformation: ConsentInformation

    // Other Variables
    private lateinit var quizType: String
    private var answeredQuestionIds = arrayListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the updated layout
        setContentView(R.layout.activity_main)

        // Initialize Consent and Ads
        initializeConsent()
        initializeAds()

        // Retrieve Quiz Type and Answered Question IDs
        retrieveQuizPreferences()

        // Initialize the database with the retrieved quiz type and previously answered question IDs
        initializeDatabase(quizType, answeredQuestionIds)

        // Initialize UI components
        initializeComponents()

        // Load Questions
        fetchQuestions(answeredQuestionIds)
    }

    private fun initializeConsent() {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                Log.d(TAG, "Consent status: ${consentInformation.consentStatus}")
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    loadAndShowConsentForm()
                } else {
                    Log.d(TAG, "Consent form not required.")
                }
            },
            { requestConsentError ->
                Log.w(TAG, "${requestConsentError.errorCode}: ${requestConsentError.message}")
            })
    }

    private fun loadAndShowConsentForm() {
        UserMessagingPlatform.loadConsentForm(
            this,
            { consentForm ->
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    consentForm.show(this) { formError ->
                        Log.e(TAG, "Consent form error: $formError")
                    }
                }
            },
            { formError ->
                Log.e(TAG, "Consent form loading error: $formError")
            }
        )
    }

    private fun initializeAds() {
        MobileAds.initialize(this) {}

        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        InterstitialAd.load(
            this,
            "ca-app-pub-3551035007628625/7595976845",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
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
                                mInterstitialAd = null
                            }
                        }
                    }
                    Log.d(TAG, "Ad was loaded.")
                }
            })
    }

    private fun retrieveQuizPreferences() {
        quizType = intent.getStringExtra("QUIZ_TYPE") ?: getSharedPreferences("QuizPrefs", MODE_PRIVATE)
            .getString("LAST_QUIZ_TYPE", "default") ?: "default"

        val sharedPrefs = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
        val savedIds = sharedPrefs.getStringSet("AnsweredQuestionIds", null)
        answeredQuestionIds = if (savedIds != null) savedIds.map { it.toInt() }.toCollection(ArrayList()) else ArrayList()
    }

    private fun initializeComponents() {
        // Initialize UI components
        questionTextView = findViewById(R.id.question_text_view)
        questionCounterTextView = findViewById(R.id.question_counter_text_view)
        hintTextView = findViewById(R.id.hint_text_view)
        option1Button = findViewById(R.id.btn_option_1)
        option2Button = findViewById(R.id.btn_option_2)
        option3Button = findViewById(R.id.btn_option_3)
        option4Button = findViewById(R.id.btn_option_4)
        nextButton = findViewById(R.id.btn_next)

        // Set click listeners for option buttons
        option1Button.setOnClickListener { onOptionSelected(1) }
        option2Button.setOnClickListener { onOptionSelected(2) }
        option3Button.setOnClickListener { onOptionSelected(3) }
        option4Button.setOnClickListener { onOptionSelected(4) }

        // Set click listener for the Next button
        nextButton.setOnClickListener { onNextClicked() }

        // Hide Next button initially
        nextButton.visibility = View.GONE
    }

    private fun initializeDatabase(quizType: String, answeredQuestionIds: ArrayList<Int>) {
        val dbName = when (quizType) {
            "class8.db" -> "class8.db"
            "class9.db" -> "class9.db"
            "class10.db" -> "class10.db"
            "db_entrance_exam.db" -> "db_entrance_exam.db"
            else -> "dbquestions.db"
        }
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, dbName)
            .createFromAsset(dbName)
            .fallbackToDestructiveMigration()
            .build()
    }

    private fun fetchQuestions(answeredQuestionIds: ArrayList<Int>) {
        CoroutineScope(Dispatchers.IO).launch {
            val allQuestions = db.questionDao().getAllQuestions()
            val filteredQuestions = allQuestions.filterNot { it.id in answeredQuestionIds }
            withContext(Dispatchers.Main) {
                questions = filteredQuestions
                if (questions.isNotEmpty()) {
                    userAnswers = MutableList(questions.size) { "Въпросът е пропуснат." }
                    loadQuestion()
                } else {
                    // Handle case where no questions are available
                    Toast.makeText(this@MainActivity, "No questions available.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun loadQuestion() {
        if (currentQuestionIndex >= questions.size || currentQuestionIndex >= 15) {
            navigateToResultActivity()
            return
        }

        // Reset selected option
        selectedOption = -1

        // Reset option buttons
        resetOptionButtons()

        // Update question counter
        questionCounterTextView.text = getString(R.string.question_counter_format, currentQuestionIndex + 1, 15)

        // Load the question and options
        val question = questions[currentQuestionIndex]
        questionTextView.apply {
            text = question.questionText
            setTextColor(Color.BLACK)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }

        // Set options text
        val options = question.options.split(";")
        option1Button.text = options.getOrNull(0)?.trim() ?: ""
        option2Button.text = options.getOrNull(1)?.trim() ?: ""
        option3Button.text = options.getOrNull(2)?.trim() ?: ""
        option4Button.text = options.getOrNull(3)?.trim() ?: ""

        // Hide hint text view and Next button
        hintTextView.visibility = View.GONE
        nextButton.visibility = View.GONE

        // Enable option buttons
        option1Button.isEnabled = true
        option2Button.isEnabled = true
        option3Button.isEnabled = true
        option4Button.isEnabled = true
    }

    private fun resetOptionButtons() {
        // Enable all option buttons
        option1Button.isEnabled = true
        option2Button.isEnabled = true
        option3Button.isEnabled = true
        option4Button.isEnabled = true

        // Reset background to default
        option1Button.setBackgroundResource(R.drawable.option_button_background)
        option2Button.setBackgroundResource(R.drawable.option_button_background)
        option3Button.setBackgroundResource(R.drawable.option_button_background)
        option4Button.setBackgroundResource(R.drawable.option_button_background)
    }

    private fun onOptionSelected(optionNumber: Int) {
        selectedOption = optionNumber

        // Disable all option buttons after selection
        option1Button.isEnabled = false
        option2Button.isEnabled = false
        option3Button.isEnabled = false
        option4Button.isEnabled = false

        // Highlight selected option
        getOptionButton(optionNumber).setBackgroundResource(R.drawable.option_button_background_selected)

        // Check the answer
        checkAnswer()
    }

    private fun checkAnswer() {
        val currentQuestion = questions[currentQuestionIndex]
        val options = currentQuestion.options.split(";")
        val selectedOptionText = options.getOrNull(selectedOption - 1)?.trim() ?: ""
        val correctAnswer = currentQuestion.correctAnswer.trim()

        userAnswers[currentQuestionIndex] = selectedOptionText
        answeredQuestionIds.add(currentQuestion.id)
        saveProgress()

        val isCorrect = selectedOptionText.equals(correctAnswer, ignoreCase = true)
        if (isCorrect) {
            score++
        }

        updateAnswerBackgrounds(isCorrect)

        // Show Next button
        nextButton.visibility = View.VISIBLE

        Log.d("AnswerCheck", "Question: ${currentQuestion.questionText}")
        Log.d("AnswerCheck", "Selected: $selectedOptionText, Correct: $correctAnswer, IsCorrect: $isCorrect")
    }

    private fun updateAnswerBackgrounds(isCorrect: Boolean) {
        val currentQuestion = questions[currentQuestionIndex]
        val options = currentQuestion.options.split(";")
        val correctOptionNumber = options.indexOfFirst { it.trim().equals(currentQuestion.correctAnswer.trim(), ignoreCase = true) } + 1

        // Change background of selected option
        if (isCorrect) {
            getOptionButton(selectedOption).setBackgroundResource(R.drawable.option_button_background_correct)
        } else {
            getOptionButton(selectedOption).setBackgroundResource(R.drawable.option_button_background_incorrect)
            // Highlight the correct answer
            getOptionButton(correctOptionNumber).setBackgroundResource(R.drawable.option_button_background_correct)
        }
    }

    private fun onNextClicked() {
        if (selectedOption == -1) {
            // No option selected, show a hint
            hintTextView.text = "Моля, изберете отговор!"
            hintTextView.visibility = View.VISIBLE
        } else {
            // Move to the next question
            currentQuestionIndex++
            loadQuestion()
        }
    }

    private fun getOptionButton(optionNumber: Int): Button {
        return when (optionNumber) {
            1 -> option1Button
            2 -> option2Button
            3 -> option3Button
            4 -> option4Button
            else -> throw IllegalArgumentException("Invalid option number")
        }
    }

    private fun saveProgress() {
        val sharedPref = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putStringSet("AnsweredQuestionIds", answeredQuestionIds.map { it.toString() }.toSet())
            apply()
        }
    }

    private fun navigateToResultActivity() {
        // Populate QuizResultsHolder with the current quiz results
        QuizResultsHolder.score = score
        QuizResultsHolder.questions = questions
        QuizResultsHolder.userAnswers = ArrayList(userAnswers)

        // Navigate to ResultActivity
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    proceedToResultActivity()
                }
            }
            mInterstitialAd?.show(this)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            proceedToResultActivity()
        }
    }

    private fun proceedToResultActivity() {
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Handle AdView lifecycle
    override fun onResume() {
        super.onResume()
        mAdView.resume()
    }

    override fun onPause() {
        mAdView.pause()
        super.onPause()
    }

    override fun onDestroy() {
        mAdView.destroy()
        super.onDestroy()
    }
}
