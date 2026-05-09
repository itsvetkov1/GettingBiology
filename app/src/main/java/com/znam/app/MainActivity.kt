package com.znam.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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
    private lateinit var option1Button: MaterialButton
    private lateinit var option2Button: MaterialButton
    private lateinit var option3Button: MaterialButton
    private lateinit var option4Button: MaterialButton
    private lateinit var nextButton: Button
    private lateinit var mAdView: AdView
    private lateinit var timerTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var nestedScrollView: androidx.core.widget.NestedScrollView

    // Hint UI Components
    private lateinit var hintButton: MaterialCardView
    private lateinit var hintLabel: TextView
    private lateinit var hintBubblesContainer: LinearLayout
    private lateinit var hint1Bubble: View
    private lateinit var hint2Bubble: View
    private lateinit var hint1Text: TextView
    private lateinit var hint2Text: TextView

    // Quiz Data
    private var currentQuestionIndex = 0
    private lateinit var questions: List<Question>
    private var score = 0
    private lateinit var userAnswers: MutableList<String>
    private var selectedOption = -1
    private var isAnswered = false
    private var hintsShown = 0

    // Timer
    private var startTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsedMillis = System.currentTimeMillis() - startTime
            val elapsedSeconds = (elapsedMillis / 1000).toInt()
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    // Auto-Advance
    private val autoAdvanceHandler = Handler(Looper.getMainLooper())
    private var autoAdvanceRunnable: Runnable? = null
    private val AUTO_ADVANCE_DELAY = 1500L // 1.5 seconds

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
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error starting quiz: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
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
        mAdView.adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        InterstitialAd.load(
            this,
            BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
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
        timerTextView = findViewById(R.id.tv_time)
        scoreTextView = findViewById(R.id.tv_score)
        nestedScrollView = findViewById(R.id.nested_scroll_view)

        // Initialize Hint UI
        hintButton = findViewById(R.id.hintButton)
        hintLabel = findViewById(R.id.hintLabel)
        hintBubblesContainer = findViewById(R.id.hintBubblesContainer)
        hint1Bubble = findViewById(R.id.hint1Bubble)
        hint2Bubble = findViewById(R.id.hint2Bubble)
        hint1Text = hint1Bubble.findViewById(R.id.hintText)
        hint2Text = hint2Bubble.findViewById(R.id.hintText)

        // Set click listeners for option buttons
        option1Button.setOnClickListener { onOptionSelected(1) }
        option2Button.setOnClickListener { onOptionSelected(2) }
        option3Button.setOnClickListener { onOptionSelected(3) }
        option4Button.setOnClickListener { onOptionSelected(4) }

        // Set click listener for hint button
        hintButton.setOnClickListener { onHintRequested() }

        // Initialize score display
        updateScoreDisplay()
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
            try {
                val allQuestions = db.questionDao().getAllQuestions()
                val filteredQuestions = allQuestions.filterNot { it.id in answeredQuestionIds }
                withContext(Dispatchers.Main) {
                    questions = filteredQuestions
                    if (questions.isNotEmpty()) {
                        userAnswers = MutableList(questions.size) { "Въпросът е пропуснат." }
                        startTimer()
                        loadQuestion()
                    } else {
                        // Handle case where no questions are available
                        Toast.makeText(this@MainActivity, "No questions available.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error fetching questions", e)
                    Toast.makeText(this@MainActivity, "Error loading questions: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        handler.post(timerRunnable)
    }

    private fun stopTimer() {
        handler.removeCallbacks(timerRunnable)
    }

    private fun getElapsedTimeInSeconds(): Int {
        val elapsedMillis = System.currentTimeMillis() - startTime
        return (elapsedMillis / 1000).toInt()
    }

    private fun updateScoreDisplay() {
        scoreTextView.text = "$score / 15"
    }

    private fun onHintRequested() {
        if (currentQuestionIndex >= questions.size) return
        val currentQuestion = questions[currentQuestionIndex]

        when (hintsShown) {
            0 -> {
                if (!currentQuestion.hint1.isNullOrBlank()) {
                    showHintBubble(1, currentQuestion.hint1)
                    hintsShown = 1
                    updateHintButtonState()
                }
            }
            1 -> {
                if (!currentQuestion.hint2.isNullOrBlank()) {
                    showHintBubble(2, currentQuestion.hint2)
                    hintsShown = 2
                    updateHintButtonState()
                }
            }
        }
    }

    private fun showHintBubble(hintNumber: Int, text: String) {
        val bubbleView = if (hintNumber == 1) hint1Bubble else hint2Bubble
        val textView = if (hintNumber == 1) hint1Text else hint2Text

        textView.text = text
        hintBubblesContainer.visibility = View.VISIBLE
        bubbleView.visibility = View.VISIBLE

        // Use post to ensure layout is complete before calculating pivot and starting animation
        bubbleView.post {
            // Set pivot for animation (right edge)
            bubbleView.pivotX = bubbleView.width.toFloat()
            bubbleView.pivotY = bubbleView.height.toFloat() / 2

            // Pop animation
            val scaleX = ObjectAnimator.ofFloat(bubbleView, "scaleX", 0f, 1f)
            val scaleY = ObjectAnimator.ofFloat(bubbleView, "scaleY", 0f, 1f)
            val alpha = ObjectAnimator.ofFloat(bubbleView, "alpha", 0f, 1f)

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 300
                interpolator = OvershootInterpolator(1.2f)
                start()
            }

            // Scroll to top to ensure the new hint is visible
            nestedScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun hideAllHintBubbles() {
        hintBubblesContainer.visibility = View.GONE
        hint1Bubble.visibility = View.GONE
        hint2Bubble.visibility = View.GONE
    }

    private fun updateHintButtonState() {
        if (currentQuestionIndex >= questions.size) return
        val currentQuestion = questions[currentQuestionIndex]

        val shouldBeEnabled = when {
            !currentQuestion.hasHints() -> false
            hintsShown >= 2 -> false
            hintsShown == 1 && currentQuestion.hint2.isNullOrBlank() -> false
            else -> true
        }

        hintButton.isEnabled = shouldBeEnabled
        hintButton.alpha = if (shouldBeEnabled) 1.0f else 0.5f
        hintLabel.alpha = if (shouldBeEnabled) 1.0f else 0.5f
    }

    private fun loadQuestion() {
        if (currentQuestionIndex >= questions.size || currentQuestionIndex >= 15) {
            navigateToResultActivity()
            return
        }

        // Reset selected option
        selectedOption = -1
        isAnswered = false
        hintsShown = 0
        hideAllHintBubbles()

        // Reset option buttons
        resetOptionButtons()

        // Update question counter
        questionCounterTextView.text = getString(R.string.question_counter_format, currentQuestionIndex + 1, 15)

        // Load the question and options
        val question = questions[currentQuestionIndex]
        questionTextView.apply {
            text = question.questionText
        }

        // Update hint button state for new question
        updateHintButtonState()

        // Set options text
        val options = question.getParsedOptions()
        option1Button.text = options.getOrNull(0) ?: ""
        option2Button.text = options.getOrNull(1) ?: ""
        option3Button.text = options.getOrNull(2) ?: ""
        option4Button.text = options.getOrNull(3) ?: ""

        // Hide buttons if they have no text
        option1Button.visibility = if (option1Button.text.isEmpty()) View.GONE else View.VISIBLE
        option2Button.visibility = if (option2Button.text.isEmpty()) View.GONE else View.VISIBLE
        option3Button.visibility = if (option3Button.text.isEmpty()) View.GONE else View.VISIBLE
        option4Button.visibility = if (option4Button.text.isEmpty()) View.GONE else View.VISIBLE

        // Hide hint text view and Next button
        hintTextView.visibility = View.GONE
        nextButton.visibility = View.GONE
    }

    private fun resetOptionButtons() {
        // Enable all option buttons
        option1Button.isClickable = true
        option2Button.isClickable = true
        option3Button.isClickable = true
        option4Button.isClickable = true

        // Reset background to default
        option1Button.setBackgroundResource(R.drawable.option_button_background)
        option2Button.setBackgroundResource(R.drawable.option_button_background)
        option3Button.setBackgroundResource(R.drawable.option_button_background)
        option4Button.setBackgroundResource(R.drawable.option_button_background)

        // Reset text color to default
        option1Button.setTextColor(Color.BLACK)
        option2Button.setTextColor(Color.BLACK)
        option3Button.setTextColor(Color.BLACK)
        option4Button.setTextColor(Color.BLACK)
    }

    private fun onOptionSelected(optionNumber: Int) {
        if (isAnswered) return
        isAnswered = true
        selectedOption = optionNumber

        // Disable all option buttons after selection
        option1Button.isClickable = false
        option2Button.isClickable = false
        option3Button.isClickable = false
        option4Button.isClickable = false

        // Highlight selected option with blue background
        val selectedButton = getOptionButton(optionNumber)
        selectedButton?.setBackgroundResource(R.drawable.option_button_background_selected)
        selectedButton?.setTextColor(Color.BLACK)

        // Check the answer
        checkAnswer()
    }

    private fun checkAnswer() {
        val currentQuestion = questions[currentQuestionIndex]
        val options = currentQuestion.getParsedOptions()
        val selectedOptionText = options.getOrNull(selectedOption - 1) ?: ""
        val correctAnswer = currentQuestion.correctAnswer.trim()

        userAnswers[currentQuestionIndex] = selectedOptionText
        answeredQuestionIds.add(currentQuestion.id)
        saveProgress()

        val isCorrect = selectedOptionText.equals(correctAnswer, ignoreCase = true)
        if (isCorrect) {
            score++
            updateScoreDisplay()
        }

        updateAnswerBackgrounds(isCorrect)

        // Schedule auto-advance
        scheduleAutoAdvance()

        Log.d("AnswerCheck", "Question: ${currentQuestion.questionText}")
        Log.d("AnswerCheck", "Selected: $selectedOptionText, Correct: $correctAnswer, IsCorrect: $isCorrect")
    }

    private fun updateAnswerBackgrounds(isCorrect: Boolean) {
        val currentQuestion = questions[currentQuestionIndex]
        val options = currentQuestion.getParsedOptions()
        val correctOptionNumber = options.indexOfFirst { it.equals(currentQuestion.correctAnswer.trim(), ignoreCase = true) } + 1

        // Change background of selected option
        if (isCorrect) {
            // Subtle green for correct answer
            val selectedButton = getOptionButton(selectedOption)
            selectedButton?.setBackgroundResource(R.drawable.option_button_background_correct)
            selectedButton?.setTextColor(Color.BLACK)
        } else {
            // Subtle red for incorrect answer
            val selectedButton = getOptionButton(selectedOption)
            selectedButton?.setBackgroundResource(R.drawable.option_button_background_incorrect)
            selectedButton?.setTextColor(Color.BLACK)

            // Highlight the correct answer in green
            val correctButton = getOptionButton(correctOptionNumber)
            correctButton?.setBackgroundResource(R.drawable.option_button_background_correct)
            correctButton?.setTextColor(Color.BLACK)
        }
    }

    private fun scheduleAutoAdvance() {
        // Cancel any previously pending auto-advance runnable
        autoAdvanceRunnable?.let { autoAdvanceHandler.removeCallbacks(it) }

        autoAdvanceRunnable = Runnable {
            if (isFinishing || isDestroyed) return@Runnable

            if (currentQuestionIndex >= 14 || currentQuestionIndex >= questions.size - 1) {
                // Last question, navigate to results
                navigateToResultActivity()
            } else {
                // Move to the next question
                currentQuestionIndex++
                loadQuestion()
                isAnswered = false // Reset for the next question
            }
        }

        autoAdvanceHandler.postDelayed(autoAdvanceRunnable!!, AUTO_ADVANCE_DELAY)
    }

    private fun getOptionButton(optionNumber: Int): MaterialButton? {
        return when (optionNumber) {
            1 -> option1Button
            2 -> option2Button
            3 -> option3Button
            4 -> option4Button
            else -> null
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
        // Stop the timer
        stopTimer()

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
        val result = QuizResult(
            score = score,
            questions = ArrayList(questions.take(15)),
            userAnswers = ArrayList(userAnswers.take(15)),
            elapsedTimeInSeconds = getElapsedTimeInSeconds()
        )
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_QUIZ_RESULT, result)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // Handle AdView lifecycle
    override fun onResume() {
        super.onResume()
        mAdView.resume()
    }

    override fun onPause() {
        mAdView.pause()
        // Cancel auto-advance if the activity is paused
        autoAdvanceRunnable?.let { autoAdvanceHandler.removeCallbacks(it) }
        super.onPause()
    }

    override fun onDestroy() {
        stopTimer()
        mAdView.destroy()
        // Cancel auto-advance to prevent memory leaks
        autoAdvanceRunnable?.let { autoAdvanceHandler.removeCallbacks(it) }
        super.onDestroy()
    }
}
