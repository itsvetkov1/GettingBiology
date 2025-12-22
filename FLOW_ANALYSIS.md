# GettingBiology App - "Start Practice" Button Flow Analysis

**Document Version**: 1.0
**Created**: 2025-11-26
**Analysis Scope**: Complete user journey from "Start practice" button press to quiz completion

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Visual Flow Diagram](#visual-flow-diagram)
3. [Technical Deep Dive](#technical-deep-dive)
4. [Layman's Explanation](#laymans-explanation)
5. [Data Flow and Persistence](#data-flow-and-persistence)
6. [Ad Integration Flow](#ad-integration-flow)
7. [Error Handling](#error-handling)

---

## Executive Summary

When a user presses the **"Start practice"** button (labeled "Биология" in Bulgarian) in the GettingBiology app, they initiate a complex multi-stage process that involves:

- **4 Activity transitions**: WelcomeActivity → SelectQuizActivity → MainActivity → ResultActivity
- **Database operations**: Loading questions from pre-bundled SQLite databases
- **Progress tracking**: Preventing repeated questions using SharedPreferences
- **Ad serving**: Loading and displaying banner and interstitial advertisements
- **Real-time feedback**: Immediate answer validation with color-coded visual feedback
- **Session management**: 15-question quiz sessions with score tracking

**Total User Journey Time**: ~5-10 minutes (depending on answer speed)
**Key Technologies**: Kotlin, Room Database, Coroutines, Google Mobile Ads SDK, Material Design 3

---

## Visual Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 1: WELCOME SCREEN                                         │
│ Activity: WelcomeActivity                                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [App Launch]                                                   │
│       ↓                                                         │
│  ┌──────────────────────┐                                      │
│  │ Display GIF Animation │ (3 seconds)                         │
│  └──────────┬───────────┘                                      │
│             ↓                                                   │
│  ┌──────────────────────┐                                      │
│  │ Show Static Image    │                                      │
│  │ Show "Биология"      │                                      │
│  │ Button               │                                      │
│  └──────────┬───────────┘                                      │
│             ↓                                                   │
│  [USER PRESSES "БИОЛОГИЯ" BUTTON] ← START OF ANALYSIS          │
│             ↓                                                   │
│  ┌──────────────────────┐                                      │
│  │ Create Intent to     │                                      │
│  │ SelectQuizActivity   │                                      │
│  └──────────┬───────────┘                                      │
│             ↓                                                   │
│  startActivity(intent)                                          │
│  Transition: FADE_IN/FADE_OUT                                  │
│                                                                 │
└─────────────────────┬───────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 2: QUIZ SELECTION                                        │
│ Activity: SelectQuizActivity                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  onCreate() executes                                            │
│       ↓                                                         │
│  ┌──────────────────────┐                                      │
│  │ Display 4 Buttons:   │                                      │
│  │  - 8-ми клас         │                                      │
│  │  - 9-ти клас         │                                      │
│  │  - 10-ти клас        │                                      │
│  │  - Кандидатстудент   │                                      │
│  └──────────┬───────────┘                                      │
│             ↓                                                   │
│  [USER SELECTS A QUIZ TYPE]                                    │
│             ↓                                                   │
│  ┌──────────────────────────────────┐                          │
│  │ Example: User clicks "8-ми клас" │                          │
│  └──────────┬───────────────────────┘                          │
│             ↓                                                   │
│  startQuiz("class8.db") called                                 │
│       ↓                                                         │
│  ┌────────────────────────────────────────────┐                │
│  │ 1. Create Intent to MainActivity            │                │
│  │    - putExtra("QUIZ_TYPE", "class8.db")     │                │
│  │                                             │                │
│  │ 2. Save to SharedPreferences:               │                │
│  │    - Key: "LAST_QUIZ_TYPE"                  │                │
│  │    - Value: "class8.db"                     │                │
│  │    - Storage: QuizPrefs (MODE_PRIVATE)      │                │
│  │                                             │                │
│  │ 3. Launch MainActivity                      │                │
│  │    - Transition: FADE_IN/FADE_OUT           │                │
│  └────────────────┬───────────────────────────┘                │
│                   ↓                                             │
└─────────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 3: QUIZ INITIALIZATION                                   │
│ Activity: MainActivity                                         │
│ Phase: onCreate()                                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Initialize Consent Management                         │
│  ┌──────────────────────────────────────┐                      │
│  │ initializeConsent()                   │                      │
│  │  - Request GDPR consent info          │                      │
│  │  - Display consent form if required   │                      │
│  │  - Handle user privacy preferences    │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 2: Initialize Ad Systems                                 │
│  ┌──────────────────────────────────────┐                      │
│  │ initializeAds()                       │                      │
│  │                                       │                      │
│  │ A. Mobile Ads SDK Initialization      │                      │
│  │    MobileAds.initialize(this)         │                      │
│  │                                       │                      │
│  │ B. Load Banner Ad                     │                      │
│  │    - Find AdView by ID                │                      │
│  │    - Create AdRequest                 │                      │
│  │    - mAdView.loadAd(adRequest)        │                      │
│  │                                       │                      │
│  │ C. Load Interstitial Ad (Background)  │                      │
│  │    - Ad Unit ID:                      │                      │
│  │      ca-app-pub-3551035007628625/    │                      │
│  │      7595976845                       │                      │
│  │    - Set callbacks:                   │                      │
│  │      * onAdLoaded                     │                      │
│  │      * onAdFailedToLoad               │                      │
│  │      * onAdDismissed                  │                      │
│  │      * onAdShowedFullScreen           │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 3: Retrieve Quiz Preferences                             │
│  ┌──────────────────────────────────────┐                      │
│  │ retrieveQuizPreferences()             │                      │
│  │                                       │                      │
│  │ A. Get Quiz Type                      │                      │
│  │    quizType = intent.getStringExtra   │                      │
│  │      ("QUIZ_TYPE")                    │                      │
│  │    Fallback: SharedPreferences        │                      │
│  │      .getString("LAST_QUIZ_TYPE")     │                      │
│  │    Result: "class8.db"                │                      │
│  │                                       │                      │
│  │ B. Get Previously Answered Questions  │                      │
│  │    Read from SharedPreferences:       │                      │
│  │      Key: "AnsweredQuestionIds"       │                      │
│  │      Type: StringSet                  │                      │
│  │      Convert to ArrayList<Int>        │                      │
│  │    Purpose: Avoid repeating questions │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 4: Initialize Database                                   │
│  ┌──────────────────────────────────────┐                      │
│  │ initializeDatabase(quizType, ids)     │                      │
│  │                                       │                      │
│  │ Database Selection Logic:             │                      │
│  │  when (quizType) {                    │                      │
│  │    "class8.db" → "class8.db"          │                      │
│  │    "class9.db" → "class9.db"          │                      │
│  │    "class10.db" → "class10.db"        │                      │
│  │    "db_entrance_exam.db" → "db..."    │                      │
│  │    else → "dbquestions.db"            │                      │
│  │  }                                    │                      │
│  │                                       │                      │
│  │ Room Database Creation:               │                      │
│  │  Room.databaseBuilder(...)            │                      │
│  │    .createFromAsset(dbName)           │                      │
│  │    .fallbackToDestructiveMigration()  │                      │
│  │    .build()                           │                      │
│  │                                       │                      │
│  │ Database loaded from:                 │                      │
│  │  app/src/main/assets/class8.db        │                      │
│  │  (323 KB, pre-bundled)                │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 5: Initialize UI Components                              │
│  ┌──────────────────────────────────────┐                      │
│  │ initializeComponents()                │                      │
│  │                                       │                      │
│  │ Find views by ID:                     │                      │
│  │  - questionTextView                   │                      │
│  │  - questionCounterTextView            │                      │
│  │  - hintTextView                       │                      │
│  │  - option1Button (MaterialButton)     │                      │
│  │  - option2Button                      │                      │
│  │  - option3Button                      │                      │
│  │  - option4Button                      │                      │
│  │  - nextButton                         │                      │
│  │  - scoreTextView                      │                      │
│  │                                       │                      │
│  │ Set Click Listeners:                  │                      │
│  │  option1Button → onOptionSelected(1)  │                      │
│  │  option2Button → onOptionSelected(2)  │                      │
│  │  option3Button → onOptionSelected(3)  │                      │
│  │  option4Button → onOptionSelected(4)  │                      │
│  │  nextButton → onNextClicked()         │                      │
│  │                                       │                      │
│  │ Initial State:                        │                      │
│  │  nextButton.visibility = GONE         │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 6: Fetch Questions                                       │
│  ┌──────────────────────────────────────┐                      │
│  │ fetchQuestions(answeredQuestionIds)   │                      │
│  │                                       │                      │
│  │ Execute in Background Thread:         │                      │
│  │  CoroutineScope(Dispatchers.IO)       │                      │
│  │                                       │                      │
│  │  A. Load All Questions from Database  │                      │
│  │     val allQuestions =                │                      │
│  │       db.questionDao()                │                      │
│  │         .getAllQuestions()            │                      │
│  │                                       │                      │
│  │  B. Filter Out Answered Questions     │                      │
│  │     val filteredQuestions =           │                      │
│  │       allQuestions.filterNot {        │                      │
│  │         it.id in answeredQuestionIds  │                      │
│  │       }                               │                      │
│  │                                       │                      │
│  │  C. Switch to Main Thread             │                      │
│  │     withContext(Dispatchers.Main)     │                      │
│  │                                       │                      │
│  │  D. Initialize Quiz State             │                      │
│  │     questions = filteredQuestions     │                      │
│  │     userAnswers = MutableList(        │                      │
│  │       questions.size,                 │                      │
│  │       { "Въпросът е пропуснат." }     │                      │
│  │     )                                 │                      │
│  │                                       │                      │
│  │  E. Load First Question               │                      │
│  │     loadQuestion()                    │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
└─────────────────────────────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 4: QUIZ INTERACTION LOOP                                 │
│ Activity: MainActivity                                         │
│ Phase: Question Display & Answer                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  SUBSTAGE 4A: Load Question                                    │
│  ┌──────────────────────────────────────┐                      │
│  │ loadQuestion()                        │                      │
│  │                                       │                      │
│  │ Validation:                           │                      │
│  │  if (currentQuestionIndex >= 15 ||   │                      │
│  │      currentQuestionIndex >=          │                      │
│  │      questions.size) {                │                      │
│  │    navigateToResultActivity()         │                      │
│  │    return                             │                      │
│  │  }                                    │                      │
│  │                                       │                      │
│  │ Reset State:                          │                      │
│  │  - selectedOption = -1                │                      │
│  │  - resetOptionButtons()               │                      │
│  │                                       │                      │
│  │ Update UI:                            │                      │
│  │  A. Question Counter                  │                      │
│  │     "Въпрос 1/15"                     │                      │
│  │                                       │                      │
│  │  B. Question Text                     │                      │
│  │     questionTextView.text =           │                      │
│  │       questions[0].questionText       │                      │
│  │                                       │                      │
│  │  C. Parse and Display Options         │                      │
│  │     val options = question.options    │                      │
│  │       .split(";")                     │                      │
│  │     option1Button.text = options[0]   │                      │
│  │     option2Button.text = options[1]   │                      │
│  │     option3Button.text = options[2]   │                      │
│  │     option4Button.text = options[3]   │                      │
│  │                                       │                      │
│  │  D. Hide Hint and Next Button         │                      │
│  │     hintTextView.visibility = GONE    │                      │
│  │     nextButton.visibility = GONE      │                      │
│  │                                       │                      │
│  │  E. Update Score Display              │                      │
│  │     scoreTextView.text = "0 / 15"     │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  SUBSTAGE 4B: User Selects Answer                              │
│  ┌──────────────────────────────────────┐                      │
│  │ [USER CLICKS AN OPTION BUTTON]        │                      │
│  │                                       │                      │
│  │ Example: User clicks option2Button    │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  ┌──────────────────────────────────────┐                      │
│  │ onOptionSelected(2) triggered         │                      │
│  │                                       │                      │
│  │ Step 1: Store Selection               │                      │
│  │   selectedOption = 2                  │                      │
│  │                                       │                      │
│  │ Step 2: Disable All Buttons           │                      │
│  │   option1Button.isClickable = false   │                      │
│  │   option2Button.isClickable = false   │                      │
│  │   option3Button.isClickable = false   │                      │
│  │   option4Button.isClickable = false   │                      │
│  │   (Prevents multiple selections)      │                      │
│  │                                       │                      │
│  │ Step 3: Highlight Selected Option     │                      │
│  │   option2Button.backgroundTintList =  │                      │
│  │     md_theme_light_primaryContainer   │                      │
│  │   option2Button.isChecked = true      │                      │
│  │                                       │                      │
│  │ Step 4: Check Answer                  │                      │
│  │   checkAnswer()                       │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  SUBSTAGE 4C: Answer Validation                                │
│  ┌──────────────────────────────────────┐                      │
│  │ checkAnswer()                         │                      │
│  │                                       │                      │
│  │ Retrieve Data:                        │                      │
│  │  currentQuestion = questions[0]       │                      │
│  │  options = currentQuestion.options    │                      │
│  │    .split(";")                        │                      │
│  │  selectedOptionText =                 │                      │
│  │    options[selectedOption - 1].trim() │                      │
│  │  correctAnswer =                      │                      │
│  │    currentQuestion.correctAnswer      │                      │
│  │      .trim()                          │                      │
│  │                                       │                      │
│  │ Validation:                           │                      │
│  │  isCorrect =                          │                      │
│  │    selectedOptionText.equals(         │                      │
│  │      correctAnswer,                   │                      │
│  │      ignoreCase = true                │                      │
│  │    )                                  │                      │
│  │                                       │                      │
│  │ Record Answer:                        │                      │
│  │  userAnswers[0] = selectedOptionText  │                      │
│  │  answeredQuestionIds.add(             │                      │
│  │    currentQuestion.id                 │                      │
│  │  )                                    │                      │
│  │                                       │                      │
│  │ Persist Progress:                     │                      │
│  │  saveProgress()                       │                      │
│  │    → SharedPreferences.edit()         │                      │
│  │    → putStringSet(                    │                      │
│  │         "AnsweredQuestionIds",        │                      │
│  │         answeredQuestionIds.toSet()   │                      │
│  │       )                               │                      │
│  │    → apply()                          │                      │
│  │                                       │                      │
│  │ Update Score:                         │                      │
│  │  if (isCorrect) score++               │                      │
│  │  updateScoreDisplay()                 │                      │
│  │    → scoreTextView.text = "1 / 15"    │                      │
│  │                                       │                      │
│  │ Visual Feedback:                      │                      │
│  │  updateAnswerBackgrounds(isCorrect)   │                      │
│  │                                       │                      │
│  │  If CORRECT:                          │                      │
│  │    selectedButton.backgroundTintList  │                      │
│  │      = green_100 (#4CAF50)            │                      │
│  │                                       │                      │
│  │  If INCORRECT:                        │                      │
│  │    selectedButton.backgroundTintList  │                      │
│  │      = red_100 (#F44336)              │                      │
│  │    correctButton.backgroundTintList   │                      │
│  │      = green_100 (#4CAF50)            │                      │
│  │    (Shows both wrong and correct)     │                      │
│  │                                       │                      │
│  │ Show Next Button:                     │                      │
│  │  nextButton.visibility = VISIBLE      │                      │
│  │                                       │                      │
│  │ Debug Logging:                        │                      │
│  │  Log.d("AnswerCheck",                 │                      │
│  │    "Selected: ..., Correct: ...,      │                      │
│  │     IsCorrect: ...")                  │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  SUBSTAGE 4D: Move to Next Question                            │
│  ┌──────────────────────────────────────┐                      │
│  │ [USER CLICKS "NEXT" BUTTON]           │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  ┌──────────────────────────────────────┐                      │
│  │ onNextClicked()                       │                      │
│  │                                       │                      │
│  │ Validation:                           │                      │
│  │  if (selectedOption == -1) {          │                      │
│  │    hintTextView.text =                │                      │
│  │      "Моля, изберете отговор!"        │                      │
│  │    hintTextView.visibility = VISIBLE  │                      │
│  │    return                             │                      │
│  │  }                                    │                      │
│  │                                       │                      │
│  │ Progression:                          │                      │
│  │  currentQuestionIndex++               │                      │
│  │  loadQuestion() → LOOP BACK TO 4A     │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  LOOP CONTINUES FOR QUESTIONS 2-15                             │
│  (Repeat substages 4A → 4B → 4C → 4D)                          │
│                 ↓                                               │
│  When currentQuestionIndex reaches 15:                         │
│    navigateToResultActivity()                                  │
│                 ↓                                               │
└─────────────────────────────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 5: QUIZ COMPLETION & AD DISPLAY                          │
│ Activity: MainActivity                                         │
│ Phase: navigateToResultActivity()                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Populate Results Holder                               │
│  ┌──────────────────────────────────────┐                      │
│  │ QuizResultsHolder (Singleton Object)  │                      │
│  │                                       │                      │
│  │ QuizResultsHolder.score = score       │                      │
│  │   (Final score, e.g., 12)             │                      │
│  │                                       │                      │
│  │ QuizResultsHolder.questions =         │                      │
│  │   questions                           │                      │
│  │   (List of 15 Question objects)       │                      │
│  │                                       │                      │
│  │ QuizResultsHolder.userAnswers =       │                      │
│  │   ArrayList(userAnswers)              │                      │
│  │   (List of 15 user answer strings)    │                      │
│  │                                       │                      │
│  │ Purpose: Pass data to ResultActivity  │                      │
│  │ without using Intent extras           │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 2: Show Interstitial Ad (If Loaded)                      │
│  ┌──────────────────────────────────────┐                      │
│  │ if (mInterstitialAd != null)          │                      │
│  │                                       │                      │
│  │ Set Full Screen Callback:             │                      │
│  │  fullScreenContentCallback =          │                      │
│  │    FullScreenContentCallback() {      │                      │
│  │                                       │                      │
│  │      onAdDismissedFullScreenContent() │                      │
│  │        → proceedToResultActivity()    │                      │
│  │                                       │                      │
│  │      onAdFailedToShowFullScreenContent│                      │
│  │        → Log failure                  │                      │
│  │                                       │                      │
│  │      onAdShowedFullScreenContent()    │                      │
│  │        → mInterstitialAd = null       │                      │
│  │    }                                  │                      │
│  │                                       │                      │
│  │ Display Ad:                           │                      │
│  │  mInterstitialAd.show(this)           │                      │
│  │                                       │                      │
│  │ User Experience:                      │                      │
│  │  - Full screen ad covers app          │                      │
│  │  - User can skip after 5 seconds      │                      │
│  │  - On ad dismiss, continue to results │                      │
│  │                                       │                      │
│  │ else (Ad Not Loaded)                  │                      │
│  │  Log.d("Ad wasn't ready yet.")        │                      │
│  │  proceedToResultActivity()            │                      │
│  │  (Skip ad, go directly to results)    │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 3: Navigate to Results                                   │
│  ┌──────────────────────────────────────┐                      │
│  │ proceedToResultActivity()             │                      │
│  │                                       │                      │
│  │ val intent = Intent(                  │                      │
│  │   this,                               │                      │
│  │   ResultActivity::class.java          │                      │
│  │ )                                     │                      │
│  │                                       │                      │
│  │ startActivity(intent)                 │                      │
│  │                                       │                      │
│  │ overridePendingTransition(            │                      │
│  │   android.R.anim.fade_in,             │                      │
│  │   android.R.anim.fade_out             │                      │
│  │ )                                     │                      │
│  │                                       │                      │
│  │ finish() // Close MainActivity        │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
└─────────────────────────────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 6: RESULTS DISPLAY                                       │
│ Activity: ResultActivity                                       │
│ Phase: onCreate()                                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Retrieve Results from Singleton                       │
│  ┌──────────────────────────────────────┐                      │
│  │ val score =                           │                      │
│  │   QuizResultsHolder.score             │                      │
│  │   (e.g., 12)                          │                      │
│  │                                       │                      │
│  │ val questions =                       │                      │
│  │   QuizResultsHolder.questions         │                      │
│  │   (List of 15 Question objects)       │                      │
│  │                                       │                      │
│  │ val userAnswers =                     │                      │
│  │   QuizResultsHolder.userAnswers       │                      │
│  │   (List of 15 answer strings)         │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 2: Display Overall Score                                 │
│  ┌──────────────────────────────────────┐                      │
│  │ resultTextView.apply {                │                      │
│  │   text = "Резултат: 12/15"            │                      │
│  │   setBackgroundColor(                 │                      │
│  │     transparent_white                 │                      │
│  │   )                                   │                      │
│  │   setTextColor(BLACK)                 │                      │
│  │   setTypeface(null, BOLD)             │                      │
│  │                                       │                      │
│  │   // Fade-in animation                │                      │
│  │   alpha = 0f                          │                      │
│  │   animate()                           │                      │
│  │     .alpha(1f)                        │                      │
│  │     .setDuration(1000)                │                      │
│  │     .start()                          │                      │
│  │ }                                     │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 3: Build Detailed Review                                 │
│  ┌──────────────────────────────────────┐                      │
│  │ val questionsLayout =                 │                      │
│  │   findViewById<LinearLayout>(         │                      │
│  │     R.id.questions_layout             │                      │
│  │   )                                   │                      │
│  │                                       │                      │
│  │ For each question (index 0-14):       │                      │
│  │                                       │                      │
│  │   createQuestionView(                 │                      │
│  │     question,                         │                      │
│  │     userAnswer                        │                      │
│  │   )                                   │                      │
│  │                                       │                      │
│  │   Structure:                          │                      │
│  │   ┌────────────────────────────┐      │                      │
│  │   │ LinearLayout (Vertical)    │      │                      │
│  │   │                            │      │                      │
│  │   │ ┌────────────────────────┐ │      │                      │
│  │   │ │ Question Text          │ │      │                      │
│  │   │ │ Background: Light Gray │ │      │                      │
│  │   │ │ Bold, 18sp             │ │      │                      │
│  │   │ └────────────────────────┘ │      │                      │
│  │   │                            │      │                      │
│  │   │ ┌────────────────────────┐ │      │                      │
│  │   │ │ Correct Answer:        │ │      │                      │
│  │   │ │ "Митохондрия"          │ │      │                      │
│  │   │ │ Background: Green      │ │      │                      │
│  │   │ │ (#4CAF50)              │ │      │                      │
│  │   │ │ Bold, 18sp             │ │      │                      │
│  │   │ └────────────────────────┘ │      │                      │
│  │   │                            │      │                      │
│  │   │ ┌────────────────────────┐ │      │                      │
│  │   │ │ Your Answer:           │ │      │                      │
│  │   │ │ "Рибозома"             │ │      │                      │
│  │   │ │ Background: Red        │ │      │                      │
│  │   │ │ (#F44336)              │ │      │                      │
│  │   │ │ Bold, 18sp             │ │      │                      │
│  │   │ │ (Only if incorrect)    │ │      │                      │
│  │   │ └────────────────────────┘ │      │                      │
│  │   │                            │      │                      │
│  │   │ Bottom Margin: 30dp        │      │                      │
│  │   └────────────────────────────┘      │                      │
│  │                                       │                      │
│  │   questionsLayout.addView(            │                      │
│  │     questionView                      │                      │
│  │   )                                   │                      │
│  │                                       │                      │
│  │ Result: Scrollable list of all 15     │                      │
│  │ questions with color-coded feedback   │                      │
│  └──────────────┬───────────────────────┘                      │
│                 ↓                                               │
│  Step 4: Restart Quiz Button                                   │
│  ┌──────────────────────────────────────┐                      │
│  │ restartQuizButton.setOnClickListener  │                      │
│  │                                       │                      │
│  │ QuizResultsHolder.clear()             │                      │
│  │   (Clear singleton data)              │                      │
│  │                                       │                      │
│  │ startActivity(                        │                      │
│  │   Intent(this, MainActivity::class)   │                      │
│  │ )                                     │                      │
│  │                                       │                      │
│  │ finish() // Close ResultActivity      │                      │
│  │                                       │                      │
│  │ User restarts quiz with same          │                      │
│  │ quiz type (e.g., Grade 8)             │                      │
│  └───────────────────────────────────────┘                     │
│                                                                 │
│  [END OF FLOW]                                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Technical Deep Dive

### 1. Activity Lifecycle & Navigation

#### 1.1 WelcomeActivity → SelectQuizActivity Transition

**File**: `app/src/main/java/com/znam/app/WelcomeActivity.kt:36-45`

```kotlin
startQuizButton.setOnClickListener {
    navigateToQuiz()
}

private fun navigateToQuiz() {
    val intent = Intent(this, SelectQuizActivity::class.java)
    startActivity(intent)
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}
```

**Technical Details**:
- **Intent Creation**: Explicit intent targeting `SelectQuizActivity::class.java`
- **No Extras**: Intent contains no data payload
- **Animation Override**: Uses system fade animations instead of default slide
- **Activity Stack**: WelcomeActivity remains in back stack (not finished)

#### 1.2 SelectQuizActivity → MainActivity Transition

**File**: `app/src/main/java/com/znam/app/SelectQuizActivity.kt:27-39`

```kotlin
private fun startQuiz(databaseName: String) {
    val intent = Intent(this, MainActivity::class.java).apply {
        putExtra("QUIZ_TYPE", databaseName)
    }

    val sharedPref = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("LAST_QUIZ_TYPE", databaseName)
        apply()
    }

    startActivity(intent)
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}
```

**Technical Details**:
- **Intent Extras**: Passes `QUIZ_TYPE` as String (e.g., "class8.db")
- **SharedPreferences Persistence**:
  - File name: `QuizPrefs`
  - Mode: `MODE_PRIVATE` (app-private storage)
  - Key: `LAST_QUIZ_TYPE`
  - Storage location: `/data/data/com.znam.app/shared_prefs/QuizPrefs.xml`
  - Purpose: Fallback if Intent extra is null, persistence across app sessions
- **Async Write**: `.apply()` writes asynchronously to disk
- **Database Name Mapping**:
  - Grade 8 button → `"class8.db"`
  - Grade 9 button → `"class9.db"`
  - Grade 10 button → `"class10.db"`
  - Entrance Exam button → `"db_entrance_exam.db"`

### 2. Database Architecture

#### 2.1 Database Selection & Initialization

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:201-213`

```kotlin
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
```

**Technical Breakdown**:

1. **Database Location**: Pre-built SQLite databases in `app/src/main/assets/`
2. **Room Builder Configuration**:
   - `applicationContext`: Global application context (not activity context)
   - `AppDatabase::class.java`: Database abstract class with DAOs
   - `dbName`: Unique database name per quiz type
   - `.createFromAsset(dbName)`: Copies database from assets to app's internal storage on first access
   - `.fallbackToDestructiveMigration()`: Drops and recreates database on schema version mismatch (development-only setting)

3. **Database Files**:
   | File | Size | Questions | Target Audience |
   |------|------|-----------|-----------------|
   | class8.db | 323 KB | ~400-500 | 8th grade students |
   | class9.db | 483 KB | ~600-700 | 9th grade students |
   | class10.db | 405 KB | ~500-600 | 10th grade students |
   | db_entrance_exam.db | 892 KB | ~1100-1300 | University entrance candidates |
   | dbquestions.db | 90 KB | ~100-200 | Fallback/default |

4. **Internal Storage Path**:
   ```
   /data/data/com.znam.app/databases/class8.db
   ```

5. **Copy-on-Write Mechanism**:
   - First access: Room copies from assets to internal storage
   - Subsequent access: Reads from internal storage
   - Modification: Updates internal copy only
   - Uninstall: Deletes internal copy
   - Reinstall: Copies fresh database from assets

#### 2.2 Question Entity Structure

**File**: `app/src/main/java/com/znam/app/Question.kt:8-14`

```kotlin
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val questionText: String,
    val options: String,
    val correctAnswer: String
) : Parcelable
```

**Schema Details**:
- **Table Name**: `questions`
- **Primary Key**: `id` (auto-incrementing integer)
- **Columns**:
  - `id`: Unique identifier (1, 2, 3, ...)
  - `questionText`: Full question text (e.g., "Кой органел отговаря за производството на енергия?")
  - `options`: Semicolon-delimited string (e.g., "Митохондрия;Рибозома;Ядро;Голджи")
  - `correctAnswer`: Exact match to one option (e.g., "Митохондрия")
- **Parcelable Implementation**: Enables passing Question objects between activities (though not used in current implementation due to QuizResultsHolder singleton)

**Critical Design Note**: Options stored as delimited string instead of normalized table (1NF violation). This simplifies queries but creates parsing overhead and prohibits options containing semicolons.

#### 2.3 Database Access Object (DAO)

**File**: `app/src/main/java/com/znam/app/QuestionDao.kt`

```kotlin
@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions")
    fun getAllQuestions(): List<Question>

    @Insert
    fun insertAll(vararg questions: Question)
}
```

**Query Execution**:
- `getAllQuestions()`: Returns all questions in database (no filtering)
- Executed on `Dispatchers.IO` thread pool
- Returns `List<Question>` (not `Flow` or `LiveData`, so one-time read)
- No pagination or limiting (loads entire table into memory)

#### 2.4 Question Fetching & Filtering

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:215-239`

```kotlin
private fun fetchQuestions(answeredQuestionIds: ArrayList<Int>) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val allQuestions = db.questionDao().getAllQuestions()
            val filteredQuestions = allQuestions.filterNot { it.id in answeredQuestionIds }
            withContext(Dispatchers.Main) {
                questions = filteredQuestions
                if (questions.isNotEmpty()) {
                    userAnswers = MutableList(questions.size) { "Въпросът е пропуснат." }
                    loadQuestion()
                } else {
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
```

**Coroutine Breakdown**:

1. **Dispatchers.IO**: Background thread pool for I/O operations
   - Optimized for blocking I/O (database reads)
   - Thread pool size: max(64, 2 × CPU cores)
   - Does not block main thread

2. **Query Execution**:
   ```kotlin
   val allQuestions = db.questionDao().getAllQuestions()
   ```
   - SQLite query: `SELECT * FROM questions`
   - Returns all questions in database (e.g., 500 questions for Grade 8)
   - Blocking call (synchronous on IO thread)

3. **In-Memory Filtering**:
   ```kotlin
   val filteredQuestions = allQuestions.filterNot { it.id in answeredQuestionIds }
   ```
   - Kotlin collection filter operation
   - Excludes questions with IDs in `answeredQuestionIds` set
   - Time complexity: O(n × m) where n = total questions, m = answered questions
   - Result: Only unanswered questions remain

4. **Context Switch to Main Thread**:
   ```kotlin
   withContext(Dispatchers.Main)
   ```
   - Suspends coroutine and switches to main UI thread
   - Required for UI updates (Android enforces main-thread UI access)

5. **Initialization**:
   ```kotlin
   userAnswers = MutableList(questions.size) { "Въпросът е пропуснат." }
   ```
   - Creates mutable list pre-filled with default "Skipped" text
   - Size matches filtered questions count
   - Index-aligned with questions list

6. **Error Handling**:
   - Catches all exceptions (database errors, corruption, etc.)
   - Logs to Logcat with tag "MainActivity"
   - Shows user-friendly Toast message
   - Calls `finish()` to close activity on failure

### 3. Progress Tracking System

#### 3.1 SharedPreferences Storage

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:381-387`

```kotlin
private fun saveProgress() {
    val sharedPref = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
    with(sharedPref.edit()) {
        putStringSet("AnsweredQuestionIds", answeredQuestionIds.map { it.toString() }.toSet())
        apply()
    }
}
```

**Storage Mechanism**:
- **File**: `/data/data/com.znam.app/shared_prefs/QuizPrefs.xml`
- **Format**: XML
- **Key**: `AnsweredQuestionIds`
- **Value Type**: `Set<String>` (converted from `ArrayList<Int>`)
- **Persistence**: Survives app restarts, but not app uninstall
- **Thread Safety**: `.apply()` writes asynchronously on background thread

**Example XML Content**:
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="LAST_QUIZ_TYPE">class8.db</string>
    <set name="AnsweredQuestionIds">
        <string>1</string>
        <string>5</string>
        <string>12</string>
        <string>23</string>
        <!-- ... up to 15 IDs after one session -->
    </set>
</map>
```

#### 3.2 Progress Retrieval

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:167-174`

```kotlin
private fun retrieveQuizPreferences() {
    quizType = intent.getStringExtra("QUIZ_TYPE") ?: getSharedPreferences("QuizPrefs", MODE_PRIVATE)
        .getString("LAST_QUIZ_TYPE", "default") ?: "default"

    val sharedPrefs = getSharedPreferences("QuizPrefs", MODE_PRIVATE)
    val savedIds = sharedPrefs.getStringSet("AnsweredQuestionIds", null)
    answeredQuestionIds = if (savedIds != null) savedIds.map { it.toInt() }.toCollection(ArrayList()) else ArrayList()
}
```

**Retrieval Logic**:
1. **Quiz Type Fallback Chain**:
   - First: Intent extra `"QUIZ_TYPE"` (from SelectQuizActivity)
   - Second: SharedPreferences `"LAST_QUIZ_TYPE"` (from previous session)
   - Third: Default value `"default"` → maps to `dbquestions.db`

2. **Answered IDs Conversion**:
   - Read as `Set<String>` from SharedPreferences
   - Convert each string to Int: `savedIds.map { it.toInt() }`
   - Collect into `ArrayList<Int>` for mutability
   - If null (first launch), initialize empty `ArrayList()`

#### 3.3 Progress Update Flow

**Sequence**:
1. User answers question → `checkAnswer()` called
2. `answeredQuestionIds.add(currentQuestion.id)` (MainActivity.kt:326)
3. `saveProgress()` called immediately (MainActivity.kt:327)
4. SharedPreferences updated asynchronously
5. Next app launch: `retrieveQuizPreferences()` reads saved IDs
6. `fetchQuestions()` filters out answered questions
7. User only sees unanswered questions

**Important Behavior**:
- Progress persists across app sessions
- Once answered, a question never appears again (no "reset" mechanism in UI)
- Progress is per-device, not per-user (no account system)
- Uninstalling app clears progress

### 4. Ad Integration

#### 4.1 Google Mobile Ads SDK Initialization

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:128-165`

```kotlin
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
```

**Ad Types**:

1. **Banner Ad** (Bottom of quiz screen):
   - Ad View ID: `R.id.adView` (defined in `activity_main.xml`)
   - Ad Unit ID: Defined in layout XML (not visible in code)
   - Load timing: onCreate() (immediately)
   - Display: Persistent during entire quiz session
   - Size: Standard banner (320×50 dp)

2. **Interstitial Ad** (Full-screen between quiz and results):
   - Ad Unit ID: `ca-app-pub-3551035007628625/7595976845`
   - Load timing: onCreate() (preloads in background)
   - Display timing: After quiz completion, before results
   - User action: Can skip after 5 seconds
   - Single-use: Nullified after display (`mInterstitialAd = null`)

**Ad Request Builder**:
```kotlin
val adRequest = AdRequest.Builder().build()
```
- Uses default ad request (no targeting, demographics, or test device IDs)
- Respects user consent settings from UMP SDK

#### 4.2 Consent Management (GDPR/CCPA)

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:90-126`

```kotlin
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
```

**User Messaging Platform (UMP) Flow**:
1. Check if consent is required (based on user location)
2. If required (EU/EEA users), display consent dialog
3. User chooses "Consent" or "Do Not Consent"
4. SDK stores choice and applies to ad requests
5. Non-personalized ads shown if user declines

**Age Gate**:
- `.setTagForUnderAgeOfConsent(false)`: App declares content is not for children under 13
- Required for COPPA compliance (US law)

#### 4.3 Interstitial Ad Display Flow

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:389-414`

```kotlin
private fun navigateToResultActivity() {
    QuizResultsHolder.score = score
    QuizResultsHolder.questions = questions
    QuizResultsHolder.userAnswers = ArrayList(userAnswers)

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
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    finish()
}
```

**Flow Logic**:
1. **Populate Singleton**: Store quiz results in `QuizResultsHolder`
2. **Check Ad Availability**: `if (mInterstitialAd != null)`
   - Ad loaded → Show ad → Wait for dismiss → Navigate to results
   - Ad not loaded → Skip ad → Navigate to results immediately
3. **Callback Pattern**:
   - `onAdDismissedFullScreenContent()`: Fired when user closes ad
   - Triggers `proceedToResultActivity()` navigation
4. **Graceful Degradation**: App continues even if ad fails to load

**User Experience Timeline**:
```
Quiz completes → Ad displays → [User waits 5s] → [User clicks X] →
onAdDismissed callback → Navigate to ResultActivity → Display results
```

### 5. Quiz Interaction Logic

#### 5.1 Question Display

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:241-274`

```kotlin
private fun loadQuestion() {
    if (currentQuestionIndex >= questions.size || currentQuestionIndex >= 15) {
        navigateToResultActivity()
        return
    }

    selectedOption = -1
    resetOptionButtons()

    questionCounterTextView.text = getString(R.string.question_counter_format, currentQuestionIndex + 1, 15)

    val question = questions[currentQuestionIndex]
    questionTextView.apply {
        text = question.questionText
    }

    val options = question.options.split(";")
    option1Button.text = options.getOrNull(0)?.trim() ?: ""
    option2Button.text = options.getOrNull(1)?.trim() ?: ""
    option3Button.text = options.getOrNull(2)?.trim() ?: ""
    option4Button.text = options.getOrNull(3)?.trim() ?: ""

    hintTextView.visibility = View.GONE
    nextButton.visibility = View.GONE

    updateScoreDisplay()
}
```

**Stopping Conditions**:
```kotlin
if (currentQuestionIndex >= questions.size || currentQuestionIndex >= 15)
```
- **questions.size**: Stops if all unanswered questions exhausted
- **15**: Hardcoded maximum questions per session
- Whichever limit is reached first triggers `navigateToResultActivity()`

**Options Parsing**:
```kotlin
val options = question.options.split(";")
```
- Splits semicolon-delimited string: `"A;B;C;D"` → `["A", "B", "C", "D"]`
- `.getOrNull(index)`: Safe accessor (returns null if index out of bounds)
- `.trim()`: Removes leading/trailing whitespace
- `?: ""`: Elvis operator provides empty string fallback

**UI State Reset**:
- `selectedOption = -1`: Clears selection
- `resetOptionButtons()`: Restores default colors, re-enables buttons
- `hintTextView.visibility = GONE`: Hides hint text
- `nextButton.visibility = GONE`: Hides next button

#### 5.2 Answer Selection & Validation

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:301-357`

```kotlin
private fun onOptionSelected(optionNumber: Int) {
    selectedOption = optionNumber

    option1Button.isClickable = false
    option2Button.isClickable = false
    option3Button.isClickable = false
    option4Button.isClickable = false

    val selectedBtn = getOptionButton(optionNumber)
    selectedBtn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.md_theme_light_primaryContainer)
    selectedBtn.isChecked = true

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
    updateScoreDisplay()

    updateAnswerBackgrounds(isCorrect)

    nextButton.visibility = View.VISIBLE

    Log.d("AnswerCheck", "Question: ${currentQuestion.questionText}")
    Log.d("AnswerCheck", "Selected: $selectedOptionText, Correct: $correctAnswer, IsCorrect: $isCorrect")
}
```

**Selection Flow**:
1. **Store Selection**: `selectedOption = optionNumber` (1-4)
2. **Disable Buttons**: Prevent multiple selections
3. **Highlight Selection**: Change background to primary container color
4. **Trigger Validation**: Call `checkAnswer()` immediately

**Validation Logic**:
```kotlin
val selectedOptionText = options.getOrNull(selectedOption - 1)?.trim() ?: ""
```
- `selectedOption - 1`: Convert 1-based button number to 0-based array index
- Example: User clicks option 2 → `selectedOption = 2` → `options[1]`

```kotlin
val isCorrect = selectedOptionText.equals(correctAnswer, ignoreCase = true)
```
- **Case-insensitive comparison**: "митохондрия" matches "Митохондрия"
- **Exact match required**: No partial credit or fuzzy matching
- **Whitespace trimmed**: Leading/trailing spaces ignored

**State Updates**:
- `userAnswers[currentQuestionIndex] = selectedOptionText`: Records user's choice
- `answeredQuestionIds.add(currentQuestion.id)`: Marks question as answered
- `saveProgress()`: Persists to SharedPreferences immediately
- `score++`: Increments only if correct
- `nextButton.visibility = VISIBLE`: Shows next button

#### 5.3 Visual Feedback System

**File**: `app/src/main/java/com/znam/app/MainActivity.kt:344-357`

```kotlin
private fun updateAnswerBackgrounds(isCorrect: Boolean) {
    val currentQuestion = questions[currentQuestionIndex]
    val options = currentQuestion.options.split(";")
    val correctOptionNumber = options.indexOfFirst {
        it.trim().equals(currentQuestion.correctAnswer.trim(), ignoreCase = true)
    } + 1

    if (isCorrect) {
        getOptionButton(selectedOption).backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.green_100)
    } else {
        getOptionButton(selectedOption).backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.red_100)
        getOptionButton(correctOptionNumber).backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.green_100)
    }
}
```

**Color Coding**:
- **Correct Answer**:
  - Selected button → Green (`#4CAF50` / `green_100`)
  - Single button highlighted
- **Incorrect Answer**:
  - Selected button → Red (`#F44336` / `red_100`)
  - Correct button → Green (`#4CAF50` / `green_100`)
  - Two buttons highlighted simultaneously

**Visual States**:
```
Before Selection:
[Option A] [Option B] [Option C] [Option D]  (All light gray/default)

After Correct Selection (Option B):
[Option A] [Option B (GREEN)] [Option C] [Option D]

After Incorrect Selection (Option A, Correct is B):
[Option A (RED)] [Option B (GREEN)] [Option C] [Option D]
```

**Material Design 3 Integration**:
- Uses `backgroundTintList` for dynamic color theming
- Respects dark mode theme (colors adjust automatically)
- `ContextCompat.getColorStateList()`: Backward-compatible color resolution

### 6. Results Display

#### 6.1 QuizResultsHolder Singleton

**File**: `app/src/main/java/com/znam/app/QuizResultsHolder.kt`

```kotlin
object QuizResultsHolder {
    var score: Int = 0
    var questions: List<Question> = emptyList()
    var userAnswers: ArrayList<String> = ArrayList()

    fun clear() {
        score = 0
        questions = emptyList()
        userAnswers = ArrayList()
    }
}
```

**Design Pattern**: Singleton Object
- **Kotlin `object` keyword**: Creates single instance (thread-safe, lazy-initialized)
- **Lifetime**: Exists for entire app process lifetime
- **Purpose**: Pass data between activities without Intent extras
- **Advantage**: Avoids Parcelable serialization overhead for large datasets
- **Disadvantage**: Not destroyed with activities (memory leak risk if not cleared)

**Data Stored**:
- `score`: Final score (0-15)
- `questions`: List of 15 Question objects (includes question text, options, correct answers)
- `userAnswers`: List of 15 user answer strings (matches questions list by index)

#### 6.2 Results Rendering

**File**: `app/src/main/java/com/znam/app/ResultActivity.kt:17-53`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_result)

    val score = QuizResultsHolder.score
    val questions = QuizResultsHolder.questions
    val userAnswers = QuizResultsHolder.userAnswers

    val resultTextView = findViewById<TextView>(R.id.result_text_view).apply {
        text = "Резултат: $score/15"
        setBackgroundColor(ContextCompat.getColor(this@ResultActivity, R.color.transparent_white))
        setTextColor(Color.BLACK)
        setTypeface(null, Typeface.BOLD)
        alpha = 0f
        animate().alpha(1f).setDuration(1000).start()
    }

    val questionsLayout = findViewById<LinearLayout>(R.id.questions_layout)

    questions.forEachIndexed { index, question ->
        if (index < 15) {
            val userAnswer = userAnswers.getOrNull(index) ?: "Question Skipped"
            Log.d("QuizDebug", "Displaying Question ${index + 1}: Correct Answer: ${question.correctAnswer}, User Answer: $userAnswer")
            questionsLayout.addView(createQuestionView(question, userAnswer))
        }
    }

    findViewById<Button>(R.id.restart_quiz_button).setOnClickListener {
        QuizResultsHolder.clear()
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
```

**Score Animation**:
```kotlin
alpha = 0f
animate().alpha(1f).setDuration(1000).start()
```
- Initial state: Fully transparent (`alpha = 0`)
- Animates to: Fully opaque (`alpha = 1`)
- Duration: 1000 milliseconds (1 second)
- Effect: Fade-in animation for dramatic reveal

**Dynamic View Creation**:
```kotlin
questions.forEachIndexed { index, question ->
    questionsLayout.addView(createQuestionView(question, userAnswer))
}
```
- Iterates through all 15 questions
- Creates custom `LinearLayout` for each question
- Adds to parent `questionsLayout` (vertical scrolling list)

#### 6.3 Question Review Cards

**File**: `app/src/main/java/com/znam/app/ResultActivity.kt:55-101`

```kotlin
private fun createQuestionView(question: Question, userAnswer: String?): LinearLayout {
    val layout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            (it as LinearLayout.LayoutParams).bottomMargin = 30
        }
    }

    val questionTextView = TextView(this).apply {
        text = question.questionText
        setTextColor(Color.BLACK)
        textSize = 18f
        typeface = Typeface.create("", Typeface.BOLD)
        setPadding(16, 16, 16, 16)
        setBackgroundColor(Color.parseColor("#D3D3D3"))
    }

    val correctAnswerTextView = TextView(this).apply {
        text = "Правилен отговор: ${question.correctAnswer}"
        setTextColor(Color.BLACK)
        textSize = 18f
        typeface = Typeface.create("", Typeface.BOLD)
        setPadding(16, 16, 16, 16)
        setBackgroundColor(Color.parseColor("#4CAF50"))
    }

    layout.addView(questionTextView)
    layout.addView(correctAnswerTextView)

    if (userAnswer != question.correctAnswer) {
        val userAnswerTextView = TextView(this).apply {
            text = if (userAnswer == "SKIPPED") {
                "Въпросът е пропуснат."
            } else {
                "Вашият отговор: $userAnswer"
            }
            setTextColor(Color.BLACK)
            textSize = 18f
            typeface = Typeface.create("", Typeface.BOLD)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(if (userAnswer == null || question.correctAnswer == userAnswer) Color.TRANSPARENT else Color.parseColor("#F44336"))
        }
        layout.addView(userAnswerTextView)
    }
    return layout
}
```

**Card Structure**:
```
┌────────────────────────────────────────────┐
│ Question Text                               │ ← Light gray (#D3D3D3)
│ "Кой органел отговаря за ..."              │   Bold, 18sp
├────────────────────────────────────────────┤
│ Правилен отговор: Митохондрия               │ ← Green (#4CAF50)
│                                            │   Always displayed
├────────────────────────────────────────────┤
│ Вашият отговор: Рибозома                    │ ← Red (#F44336)
│                                            │   Only if incorrect
└────────────────────────────────────────────┘
   ↓ 30dp margin
```

**Conditional Display Logic**:
```kotlin
if (userAnswer != question.correctAnswer) {
    layout.addView(userAnswerTextView) // Only add if answer was wrong
}
```
- **Correct Answers**: Show only question + correct answer (2 text views)
- **Incorrect Answers**: Show question + correct answer + user's wrong answer (3 text views)
- **Skipped Questions**: Show "Въпросът е пропуснат." instead of answer

**Color Coding**:
- Gray background: Question text (neutral)
- Green background: Correct answer (positive reinforcement)
- Red background: User's incorrect answer (error indication)

---

## Layman's Explanation

### What Happens When You Press "Start Practice"?

Imagine the app as a restaurant that serves you biology questions. Here's what happens behind the scenes when you press the "Start Practice" button:

#### Stage 1: Entering the Restaurant (WelcomeActivity)

You see a welcoming animation (like a restaurant's neon sign), and after 3 seconds, a "Биология" button appears. When you press this button:

- **What it looks like**: The screen smoothly fades to a new menu screen
- **What's actually happening**: The app creates a "ticket" (called an Intent) that says "Take the user to the quiz selection menu"

#### Stage 2: Choosing Your Meal (SelectQuizActivity)

You arrive at a menu with 4 options:
- 8th grade biology
- 9th grade biology
- 10th grade biology
- University entrance exam biology

When you tap one (let's say "8th grade"):

1. **The app writes a sticky note**: It saves "Last quiz type: 8th grade" in a digital notepad (SharedPreferences) that persists even if you close the app.

2. **The app creates a new ticket**: This ticket says "Load the quiz screen with 8th grade questions."

3. **You're escorted to your table**: The screen fades to the quiz area.

**Why the sticky note?**: If you crash the app or your phone dies, when you restart, the app remembers you were doing 8th grade quizzes.

#### Stage 3: Setting the Table (MainActivity Initialization)

Before serving you questions, the restaurant (app) does prep work:

##### 3.1 Asking About Allergies (Consent)
**Layman version**: "Before we show you ads, are you okay with personalized ads, or do you want generic ones?"

**What's happening**:
- If you're in Europe, the app legally must ask your consent for data collection
- Your choice affects whether ads are tailored to you or generic
- This complies with GDPR (European privacy law)

##### 3.2 Turning on the TV (Ads)
**Layman version**: There's a small TV screen at the bottom of your table showing ads. A larger TV is being prepared for later.

**What's happening**:
- **Banner ad**: Loads immediately at the bottom of the screen (like a small billboard)
- **Interstitial ad**: Preloads in the background (like buffering a video) to show later between quiz and results
- **Why preload?**: So the ad is ready instantly when you finish, not making you wait

##### 3.3 Checking Your Order History (Progress Tracking)
**Layman version**: The waiter checks if you've already tried some dishes (questions) before.

**What's happening**:
- The app reads your "order history" from the sticky notepad (SharedPreferences)
- It finds a list of question IDs you've already answered (e.g., questions 1, 5, 12, 23...)
- **Why?**: So you don't get served the same questions twice

##### 3.4 Opening the Recipe Book (Database)
**Layman version**: The chef opens the "8th grade biology cookbook" (database file).

**What's happening**:
- The app looks in its storage for a file called `class8.db` (a database containing ~500 questions)
- This file is **built into the app** when you download it (like a cookbook that comes with the restaurant)
- The app copies this cookbook from the app's "pantry" (assets folder) to the "kitchen" (internal storage) the first time you use it

**Database details** (in simple terms):
- **Size**: 323 KB (about 200 pages of text)
- **Contents**: Hundreds of biology questions with 4 answer choices each
- **Format**: Like a spreadsheet with columns for question text, answer options, and correct answer

##### 3.5 Preparing Your 15-Course Meal (Fetching Questions)
**Layman version**: The chef picks 15 fresh dishes you haven't tried yet.

**What's happening** (step-by-step):

1. **Chef goes to the back kitchen** (background thread):
   - The app runs this on a "background thread" so the screen doesn't freeze
   - Think of it like a waiter taking your order to the kitchen while you stay seated

2. **Chef pulls out ALL recipes** (load all questions):
   - Reads the entire cookbook (~500 questions from database)
   - Example: Question 1, Question 2, Question 3, ... Question 500

3. **Chef crosses out dishes you've had before** (filter):
   - Compares against your order history (e.g., you've had questions 1, 5, 12, 23)
   - Removes those from the list
   - Remaining: Questions 2, 3, 4, 6, 7, 8, 9, ... (496 questions left)

4. **Chef brings food to your table** (switch to main thread):
   - Returns to the main area (UI thread) with the filtered list
   - Updates the screen to show the first question

5. **Plating the meal** (initialize quiz state):
   - Creates a "plate" (data structure) for each of the 15 questions
   - Pre-fills each plate with "Въпросът е пропуснат" (Question skipped) as a default
   - This gets overwritten when you answer

##### 3.6 Serving the First Dish (Load First Question)
**Layman version**: The waiter brings your first course to the table.

**What's happening**:
- Screen shows: "Question 1/15"
- Displays question text: "Which organelle produces energy?"
- Displays 4 answer buttons:
  - Mitochondria
  - Ribosome
  - Nucleus
  - Golgi apparatus
- Score counter shows: "0 / 15"
- Next button is hidden (you must answer first)

#### Stage 4: Eating Your Meal (Quiz Interaction)

##### 4.1 Tasting a Dish (Selecting an Answer)
**Layman version**: You point at an answer (like choosing a dish from a tray).

**What's happening** when you tap "Mitochondria":

1. **Your choice is locked in**:
   - The app stores: "User selected option 1"
   - All buttons are disabled (you can't change your mind)

2. **The button lights up**:
   - Selected button changes to a highlighted color (primary container color)
   - Visual feedback that your tap registered

3. **Immediate grading**:
   - The app doesn't wait—it checks your answer right away

##### 4.2 Chef Confirms Your Order (Answer Validation)
**Layman version**: The waiter immediately tells you if you chose correctly.

**What's happening** (technical breakdown):

1. **App retrieves the answer key**:
   - Question text: "Which organelle produces energy?"
   - Answer options: Split by semicolons → `["Mitochondria", "Ribosome", "Nucleus", "Golgi"]`
   - Your selection: `options[0]` → "Mitochondria"
   - Correct answer: "Mitochondria"

2. **Comparison**:
   - App compares: "Mitochondria" == "Mitochondria" (case-insensitive)
   - Result: **Correct!**

3. **Recording your order**:
   - Saves "Mitochondria" to your answer sheet
   - Adds this question's ID (e.g., 47) to your "dishes tried" list
   - Immediately updates the sticky notepad (SharedPreferences) so you won't see this question again

4. **Score update**:
   - Increments score: 0 → 1
   - Updates screen: "1 / 15"

5. **Visual feedback**:
   - **If correct**: Selected button turns **green** (#4CAF50)
   - **If incorrect**: Selected button turns **red** (#F44336), and the correct button turns **green**
   - This is like the waiter giving you a thumbs up (green) or shaking their head (red) while pointing to the right dish

6. **Next button appears**:
   - A "Next" button fades into view
   - You control when to move to the next question

##### 4.3 Next Course (Moving to Next Question)
**Layman version**: You finish your dish and wave for the next course.

**What's happening** when you tap "Next":

1. **Validation**:
   - App checks: "Did the user actually select an answer?"
   - If not: Shows hint text "Моля, изберете отговор!" (Please select an answer)

2. **Progression**:
   - Increments question counter: `currentQuestionIndex++` (0 → 1)
   - Calls `loadQuestion()` again
   - **Loop repeats**: Stages 4.1 → 4.2 → 4.3 for questions 2 through 15

3. **Reset**:
   - All buttons return to default color
   - Hint text disappears
   - Next button hides again
   - Screen shows "Question 2/15"

##### 4.4 Stopping Conditions
**Layman version**: The meal ends when you've had 15 courses OR the kitchen runs out of dishes.

**What's happening**:
```kotlin
if (currentQuestionIndex >= 15 || currentQuestionIndex >= questions.size)
```

- **15-question limit**: Hardcoded maximum (even if 200 questions available)
- **Out of questions**: If you've answered so many quizzes that fewer than 15 unanswered questions remain
- Whichever limit hits first triggers the end-of-quiz process

#### Stage 5: Paying the Bill (Ad Display)

**Layman version**: Before leaving the restaurant, you watch a 30-second commercial.

**What's happening**:

1. **Chef tallies your final score**:
   - Stores your score (e.g., 12/15) in a temporary clipboard (QuizResultsHolder singleton)
   - Also stores all 15 questions and your 15 answers
   - **Why a clipboard?**: Passing 15 questions + answers through an Intent would be slow and bulky

2. **TV commercial time**:
   - **If the big TV loaded successfully**: Displays a full-screen interstitial ad
     - You see a 30-second ad for a mobile game, app, etc.
     - You can skip after 5 seconds (standard Google Ads behavior)
     - When you close the ad: `onAdDismissed` callback fires → Navigate to results
   - **If the TV didn't load**: Skip directly to results (no waiting)
   - **Why preload?**: So you don't sit staring at a loading spinner after finishing the quiz

3. **Leaving the restaurant**:
   - Screen fades to the results area
   - MainActivity closes (`finish()` called) to free memory

#### Stage 6: Reading the Receipt (Results Display)

**Layman version**: You get a detailed receipt showing what you ordered, what you should've ordered, and your total score.

**What's happening**:

##### 6.1 Overall Score Card
**Layman version**: Big bold text at the top: "You got 12 out of 15 correct!"

**What's happening**:
- Text: "Резултат: 12/15"
- Background: Semi-transparent white
- **Fade-in animation**: Starts invisible, smoothly fades in over 1 second (dramatic reveal effect)
- Font: Bold, black, 18sp

##### 6.2 Detailed Item Breakdown
**Layman version**: For each of the 15 questions, you see a card showing:
1. The question
2. The correct answer (in green)
3. Your answer (in red, only if you got it wrong)

**What's happening** (technical):

For each question, the app creates a custom card:

**Structure**:
```
╔════════════════════════════════════════════╗
║ Question Text                               ║ ← Gray box
║ "Which organelle produces energy?"          ║
╠════════════════════════════════════════════╣
║ Правилен отговор: Mitochondria              ║ ← Green box
╠════════════════════════════════════════════╣ (Only if wrong)
║ Вашият отговор: Ribosome                    ║ ← Red box
╚════════════════════════════════════════════╝
```

**Logic**:
- **If you answered correctly**: Shows only 2 boxes (question + correct answer)
- **If you answered incorrectly**: Shows 3 boxes (question + correct answer + your wrong answer)
- **If you skipped**: Shows "Въпросът е пропуснат." instead of your answer

**Colors** (Material Design):
- **Gray (#D3D3D3)**: Neutral background for question text
- **Green (#4CAF50)**: Positive reinforcement for correct answer
- **Red (#F44336)**: Error indication for your mistake

**Scroll behavior**:
- All 15 cards are stacked vertically in a `LinearLayout`
- Screen is scrollable (since 15 cards won't fit on one screen)
- Each card has 30dp bottom margin for spacing

##### 6.3 Starting Over
**Layman version**: A button at the bottom says "Restart Quiz"—like asking the waiter for another round.

**What's happening** when you tap "Restart Quiz":

1. **Clear the clipboard**:
   - `QuizResultsHolder.clear()` wipes the temporary storage
   - Prevents memory leaks (old quiz data doesn't linger)

2. **Return to the quiz**:
   - Creates new Intent to MainActivity
   - Uses same quiz type (8th grade) from SharedPreferences
   - Fade transition back to quiz screen

3. **Fresh start**:
   - Loads 15 NEW questions (excluding all previously answered, including the ones you just completed)
   - Score resets to 0/15
   - Process repeats from Stage 3

---

## Data Flow and Persistence

### Data Lifecycle Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ APP INSTALL                                                     │
├─────────────────────────────────────────────────────────────────┤
│ 1. APK contains database files in assets/                       │
│    - class8.db (323 KB)                                         │
│    - class9.db (483 KB)                                         │
│    - class10.db (405 KB)                                        │
│    - db_entrance_exam.db (892 KB)                               │
│    - dbquestions.db (90 KB)                                     │
│                                                                 │
│ 2. Files stored in:                                             │
│    /data/app/com.znam.app-<random>/base.apk!/assets/           │
│    (Read-only, part of APK)                                    │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ FIRST QUIZ LAUNCH (e.g., Grade 8)                              │
├─────────────────────────────────────────────────────────────────┤
│ 1. Room Database Builder:                                       │
│    Room.databaseBuilder(context, AppDatabase, "class8.db")     │
│      .createFromAsset("class8.db")                             │
│      .build()                                                  │
│                                                                 │
│ 2. Database Copy Operation:                                     │
│    Source: /data/app/.../assets/class8.db (read-only)         │
│    Destination: /data/data/com.znam.app/databases/class8.db    │
│    Method: File copy                                           │
│    Timing: Synchronous (blocks until complete)                │
│                                                                 │
│ 3. Result:                                                      │
│    - Writable database copy in internal storage                │
│    - Original asset remains untouched                          │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ QUESTION FETCHING (Every Quiz Session)                         │
├─────────────────────────────────────────────────────────────────┤
│ CoroutineScope(Dispatchers.IO).launch {                        │
│   val allQuestions = db.questionDao().getAllQuestions()        │
│   ↓                                                             │
│   SQLite Query: SELECT * FROM questions                         │
│   ↓                                                             │
│   Result: List<Question> (~500 questions for Grade 8)          │
│   ↓                                                             │
│   val filteredQuestions = allQuestions.filterNot {             │
│     it.id in answeredQuestionIds                               │
│   }                                                             │
│   ↓                                                             │
│   Result: Only unanswered questions                            │
│   ↓                                                             │
│   withContext(Dispatchers.Main) {                              │
│     questions = filteredQuestions                              │
│     loadQuestion()                                             │
│   }                                                             │
│ }                                                               │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ PROGRESS PERSISTENCE (After Each Answer)                        │
├─────────────────────────────────────────────────────────────────┤
│ User answers question → checkAnswer() called                    │
│   ↓                                                             │
│ answeredQuestionIds.add(currentQuestion.id)                    │
│   ↓                                                             │
│ saveProgress() called:                                          │
│   val sharedPref = getSharedPreferences("QuizPrefs",           │
│                                         MODE_PRIVATE)          │
│   sharedPref.edit()                                            │
│     .putStringSet("AnsweredQuestionIds",                       │
│                   answeredQuestionIds.map{it.toString()}.toSet())│
│     .apply()                                                   │
│   ↓                                                             │
│ Async write to XML file:                                        │
│   /data/data/com.znam.app/shared_prefs/QuizPrefs.xml           │
│   ↓                                                             │
│ XML Content:                                                    │
│   <set name="AnsweredQuestionIds">                             │
│     <string>1</string>                                          │
│     <string>47</string>                                         │
│     <string>93</string>                                         │
│     ... (up to 15 IDs per session)                             │
│   </set>                                                        │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ INTER-ACTIVITY DATA TRANSFER                                   │
├─────────────────────────────────────────────────────────────────┤
│ MainActivity (Quiz Complete)                                    │
│   ↓                                                             │
│ QuizResultsHolder.score = 12                                   │
│ QuizResultsHolder.questions = List<Question>(15 items)         │
│ QuizResultsHolder.userAnswers = ArrayList<String>(15 items)    │
│   ↓                                                             │
│ [Singleton object in app process memory]                       │
│   ↓                                                             │
│ ResultActivity.onCreate()                                       │
│   ↓                                                             │
│ val score = QuizResultsHolder.score                            │
│ val questions = QuizResultsHolder.questions                    │
│ val userAnswers = QuizResultsHolder.userAnswers                │
│   ↓                                                             │
│ Display results...                                              │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ APP RESTART (Next Day)                                          │
├─────────────────────────────────────────────────────────────────┤
│ MainActivity.onCreate()                                         │
│   ↓                                                             │
│ retrieveQuizPreferences()                                       │
│   ↓                                                             │
│ Read SharedPreferences:                                         │
│   quizType = "class8.db" (from previous session)               │
│   answeredQuestionIds = [1, 47, 93, ...] (cumulative)          │
│   ↓                                                             │
│ fetchQuestions(answeredQuestionIds)                            │
│   ↓                                                             │
│ Filter out all previously answered questions                    │
│   ↓                                                             │
│ User sees ONLY new questions                                    │
│   (never repeats questions)                                    │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ APP UNINSTALL                                                   │
├─────────────────────────────────────────────────────────────────┤
│ Android OS deletes:                                             │
│   - /data/data/com.znam.app/ (all internal storage)           │
│   - All SharedPreferences files                                │
│   - All copied databases                                        │
│   ↓                                                             │
│ Result: Complete data wipe                                      │
│   - No progress retained                                        │
│   - No answered question history                                │
│   ↓                                                             │
│ Reinstall: Fresh start (all questions available again)          │
└─────────────────────────────────────────────────────────────────┘
```

### Storage Locations Summary

| Data Type | Storage Method | Location | Persistence | Size |
|-----------|---------------|----------|-------------|------|
| Question databases | Assets → Internal DB | `/data/data/com.znam.app/databases/*.db` | Until uninstall | 323-892 KB per DB |
| Answered question IDs | SharedPreferences (StringSet) | `/data/data/com.znam.app/shared_prefs/QuizPrefs.xml` | Until uninstall | ~1-10 KB |
| Last quiz type | SharedPreferences (String) | Same file as above | Until uninstall | <1 KB |
| Current quiz results | Singleton object (memory) | RAM (process memory) | Until app killed | ~50-100 KB |
| Ad consent | UMP SDK storage | Google Play Services data | Until reset | <1 KB |

---

## Ad Integration Flow

### Ad Loading Timeline

```
Time: 0ms (onCreate starts)
│
├─ initializeConsent()
│  ├─ Request consent info from Google servers
│  ├─ If EU user: Display consent dialog
│  └─ Store consent choice (personalized vs non-personalized ads)
│
├─ initializeAds()
│  │
│  ├─ [BANNER AD]
│  │  ├─ MobileAds.initialize() → Google Ads SDK setup
│  │  ├─ Find AdView in layout (bottom banner slot)
│  │  ├─ Create AdRequest.Builder().build()
│  │  └─ mAdView.loadAd(adRequest) → Async network request
│  │     │
│  │     ↓ (~500-2000ms later, depending on network)
│  │     │
│  │     ├─ Success: Banner displays at bottom of screen
│  │     └─ Failure: Empty space (no fallback)
│  │
│  └─ [INTERSTITIAL AD]
│     ├─ InterstitialAd.load(context, "ad-unit-id", adRequest, callback)
│     │  │
│     │  ↓ (~1000-3000ms later)
│     │  │
│     │  ├─ onAdLoaded(interstitialAd)
│     │  │  ├─ mInterstitialAd = interstitialAd (store reference)
│     │  │  └─ Set fullScreenContentCallback (dismiss/show/error)
│     │  │
│     │  └─ onAdFailedToLoad(adError)
│     │     ├─ mInterstitialAd = null
│     │     └─ Log.d("Ad failed to load")
│
↓
Time: ~2000ms (Quiz UI displayed, ads loading in background)
│
├─ User answers questions (ads continue loading)
│
↓
Time: ~60000ms (User finishes 15 questions)
│
└─ navigateToResultActivity()
   │
   ├─ Populate QuizResultsHolder with results
   │
   ├─ Check: if (mInterstitialAd != null)
   │  │
   │  ├─ [AD LOADED] → Show interstitial ad
   │  │  ├─ mInterstitialAd.show(this)
   │  │  ├─ Full-screen ad covers app
   │  │  ├─ User waits 5 seconds
   │  │  ├─ User clicks [X] to dismiss
   │  │  │  ↓
   │  │  └─ onAdDismissedFullScreenContent() callback
   │  │     └─ proceedToResultActivity() → Navigate to results
   │  │
   │  └─ [AD NOT LOADED] → Skip ad
   │     └─ proceedToResultActivity() → Navigate to results immediately
```

### Ad Unit Configuration

**Manifest** (`AndroidManifest.xml`):
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3551035007628625~XXXXXXXXXX"/>
```

**Code** (`MainActivity.kt`):
```kotlin
// Banner Ad: Ad unit ID defined in layout XML (not visible in Kotlin code)
mAdView = findViewById(R.id.adView)

// Interstitial Ad: Hardcoded in Kotlin
InterstitialAd.load(this, "ca-app-pub-3551035007628625/7595976845", ...)
```

**Ad Types**:
1. **Banner Ad** (AdView):
   - Size: 320×50 dp (standard banner)
   - Position: Bottom of quiz screen
   - Refresh: Auto-refreshes every ~60 seconds (Google default)
   - Revenue: Low (~$0.01-0.05 per click)

2. **Interstitial Ad** (Full-screen):
   - Size: Full screen
   - Position: Between quiz completion and results
   - Frequency: Once per quiz session
   - Revenue: Higher (~$0.10-0.50 per click)

### Ad Request Configuration

```kotlin
val adRequest = AdRequest.Builder().build()
```

**Default Configuration** (no custom parameters):
- No test device IDs (production ads)
- No demographic targeting
- No content URL
- No keywords
- Respects UMP consent settings automatically
- Uses device's ad ID for personalization (if consented)

**Consent-Based Ad Serving**:
- **User consented**: Personalized ads (higher eCPM)
- **User declined**: Non-personalized ads (lower eCPM, generic ads)
- **Outside EU/EEA**: Defaults to personalized (no consent required)

---

## Error Handling

### Database Errors

**Scenario 1: Database File Missing**

```kotlin
// initializeDatabase() called, but asset file doesn't exist
db = Room.databaseBuilder(...)
    .createFromAsset("missing_database.db") // ← File not found
    .build()
```

**Result**:
- **Exception**: `FileNotFoundException` or `SQLiteException`
- **Caught in**: `fetchQuestions()` try-catch block
- **User Experience**:
  ```kotlin
  Toast.makeText(this, "Error loading questions: ...", LENGTH_LONG).show()
  finish() // Activity closes
  ```
- **Recovery**: User returns to SelectQuizActivity, can try different quiz type

**Scenario 2: Database Corruption**

```kotlin
val allQuestions = db.questionDao().getAllQuestions()
// Database file is corrupted (malformed SQLite)
```

**Result**:
- **Exception**: `SQLiteDatabaseCorruptException`
- **Caught in**: `fetchQuestions()` try-catch block
- **User Experience**: Same as Scenario 1
- **Automatic Recovery**:
  ```kotlin
  .fallbackToDestructiveMigration()
  ```
  - On next launch, Room deletes corrupted database
  - Copies fresh database from assets

**Scenario 3: No Questions Available**

```kotlin
val filteredQuestions = allQuestions.filterNot { it.id in answeredQuestionIds }
// Result: Empty list (user answered all questions)
```

**Result**:
```kotlin
if (questions.isEmpty()) {
    Toast.makeText(this, "No questions available.", LENGTH_LONG).show()
    finish()
}
```
- **User Experience**: Toast message, returns to SelectQuizActivity
- **Recovery**: User must choose different quiz type or wait for app data reset

### Network Errors (Ads)

**Scenario 1: No Internet Connection**

```kotlin
InterstitialAd.load(this, "ad-unit-id", adRequest, callback)
// Device has no internet
```

**Result**:
```kotlin
override fun onAdFailedToLoad(adError: LoadAdError) {
    Log.d(TAG, adError.message) // "No internet connection"
    mInterstitialAd = null
}
```
- **User Experience**:
  - Banner ad shows empty space
  - Interstitial ad skipped (navigates directly to results)
- **Graceful Degradation**: Quiz functionality unaffected

**Scenario 2: Ad Request Timeout**

```kotlin
// Ad request takes >30 seconds
```

**Result**:
- `onAdFailedToLoad()` called with timeout error
- Same graceful degradation as Scenario 1

**Scenario 3: Invalid Ad Unit ID**

```kotlin
InterstitialAd.load(this, "invalid-ad-unit-id", ...)
```

**Result**:
- `onAdFailedToLoad()` called with "Invalid ad unit" error
- Same graceful degradation

### Intent/Navigation Errors

**Scenario 1: QuizResultsHolder Data Null**

```kotlin
// ResultActivity.onCreate()
val score = QuizResultsHolder.score // 0 (default)
val questions = QuizResultsHolder.questions // emptyList()
val userAnswers = QuizResultsHolder.userAnswers // empty ArrayList()
```

**Result**:
```kotlin
questions.forEachIndexed { ... } // Loop runs 0 times
```
- **User Experience**: Results screen shows "Резултат: 0/15" with no question cards
- **Root Cause**: MainActivity was killed before navigating to ResultActivity
- **Prevention**: Data stored in singleton (survives activity recreation)

**Scenario 2: Activity Stack Corruption**

```kotlin
// User presses Back button repeatedly
WelcomeActivity ← SelectQuizActivity ← MainActivity ← Back, Back, Back
```

**Result**:
- Android handles back stack naturally
- MainActivity calls `finish()` when navigating to results (removes from stack)
- Prevents duplicate activities

### UI State Errors

**Scenario 1: Rapid Button Tapping**

```kotlin
// User taps option button multiple times quickly
onOptionSelected(1)
onOptionSelected(2) // ← Should be blocked
```

**Prevention**:
```kotlin
option1Button.isClickable = false
option2Button.isClickable = false
option3Button.isClickable = false
option4Button.isClickable = false
```
- All buttons disabled immediately after first tap
- Prevents race conditions

**Scenario 2: Next Button Without Selection**

```kotlin
// User taps Next without selecting answer
onNextClicked()
// selectedOption == -1
```

**Result**:
```kotlin
if (selectedOption == -1) {
    hintTextView.text = "Моля, изберете отговор!"
    hintTextView.visibility = VISIBLE
    return // Do not proceed
}
```
- **User Experience**: Hint text appears, question doesn't advance
- **Recovery**: User must select an answer

### Memory Management

**Scenario 1: Low Memory During Quiz**

**Android Behavior**:
- OS may kill MainActivity if app is backgrounded
- `onSaveInstanceState()` not implemented (no state persistence)
- Quiz progress lost

**Mitigation**:
- `answeredQuestionIds` saved to SharedPreferences after each answer
- User won't see repeated questions, but current session lost

**Scenario 2: QuizResultsHolder Memory Leak**

**Potential Issue**:
```kotlin
object QuizResultsHolder {
    var questions: List<Question> = emptyList() // Holds 15 Question objects
    var userAnswers: ArrayList<String> = ArrayList() // Holds 15 strings
}
```

**Mitigation**:
```kotlin
QuizResultsHolder.clear() // Called when restarting quiz
```
- Clears references to allow garbage collection
- Called in `ResultActivity` before navigating back to quiz

---

## Performance Considerations

### Database Query Performance

**Current Implementation**:
```kotlin
val allQuestions = db.questionDao().getAllQuestions()
// Loads ALL questions (~500-1300 depending on database)
```

**Pros**:
- Simple implementation
- Works offline (pre-bundled database)

**Cons**:
- Loads entire table into memory (~500-1300 objects)
- Memory usage: ~200-500 KB for question data
- Slower for large databases (entrance exam with 1300 questions)

**Optimization Opportunity** (not implemented):
```kotlin
@Query("SELECT * FROM questions WHERE id NOT IN (:answeredIds) LIMIT 15")
fun getUnansweredQuestions(answeredIds: List<Int>): List<Question>
```
- SQL-level filtering instead of in-memory
- Loads only 15 questions (not entire table)
- Faster, less memory

### UI Thread Performance

**Coroutine Usage**:
```kotlin
CoroutineScope(Dispatchers.IO).launch {
    // Database query on background thread
    val allQuestions = db.questionDao().getAllQuestions()

    withContext(Dispatchers.Main) {
        // UI update on main thread
        loadQuestion()
    }
}
```

**Why This Matters**:
- Database queries are blocking (synchronous)
- Running on main thread would freeze UI (ANR - Application Not Responding)
- `Dispatchers.IO` uses thread pool optimized for I/O operations

**ANR Threshold**: 5 seconds
- If main thread blocked >5 seconds, Android shows "App not responding" dialog
- Coroutines prevent this

### Ad Loading Performance

**Preloading Strategy**:
```kotlin
// Interstitial ad loads in onCreate() (before quiz starts)
InterstitialAd.load(this, "ad-unit-id", ...)
```

**Benefits**:
- Ad ready when quiz completes
- No waiting for ad to load after answering 15 questions
- Seamless user experience

**Network Impact**:
- ~100-500 KB data download per ad
- Happens in background during quiz
- User doesn't notice

**Memory Impact**:
- Interstitial ad kept in memory during quiz
- ~5-10 MB (includes ad creative assets)
- Nullified after display to free memory

### Layout Inflation Performance

**ResultActivity Dynamic Views**:
```kotlin
questions.forEachIndexed { index, question ->
    questionsLayout.addView(createQuestionView(question, userAnswer))
}
```

**Performance Analysis**:
- Creates 15 custom `LinearLayout` objects
- Each contains 2-3 `TextView` objects
- Total: 30-45 view objects
- Inflation time: ~50-100ms (negligible)

**Why Not RecyclerView?**:
- Fixed item count (always 15)
- All items visible (scrollable, but not virtualized)
- RecyclerView overhead not justified for 15 items

### App Size

**APK Breakdown**:
| Component | Size | Percentage |
|-----------|------|------------|
| Database files (assets) | ~2.2 MB | ~60% |
| welcome_animation.gif | ~1.5 MB | ~40% |
| Code (DEX) | ~300 KB | ~8% |
| Resources (layouts, drawables) | ~200 KB | ~5% |
| **Total APK** | **~4 MB** | **100%** |

**Optimization Opportunities**:
1. **Replace GIF with Lottie**: ~1.5 MB → ~100 KB (93% reduction)
2. **Database compression**: Use GZIP (not natively supported by Room)
3. **Enable ProGuard/R8**: Code shrinking (~300 KB → ~150 KB)

---

## Conclusion

When you press the "Start Practice" button in the GettingBiology app, you trigger a sophisticated chain of events involving:

1. **3 activity transitions** (Welcome → Selection → Quiz → Results)
2. **Database operations** (load, filter, query)
3. **Progress tracking** (SharedPreferences persistence)
4. **Ad serving** (consent management, banner/interstitial loading)
5. **Real-time validation** (answer checking with visual feedback)
6. **Results rendering** (dynamic view creation with color coding)

The app is designed with:
- **Offline-first architecture** (pre-bundled databases, no network required for quiz)
- **Graceful degradation** (ads fail → continue without ads)
- **Progress persistence** (never repeat questions)
- **Material Design 3** (modern, accessible UI)
- **Coroutine-based async operations** (smooth, non-blocking UI)

**Technical Stack Summary**:
- **Language**: Kotlin 1.9.20
- **Architecture**: Activity-based (not MVVM)
- **Database**: Room 2.6.1 with SQLite
- **Async**: Kotlin Coroutines
- **Ads**: Google Mobile Ads SDK 22.6.0
- **UI**: Material Components 1.11.0
- **Min SDK**: 24 (Android 8.0), Target SDK: 33 (Android 13)

**User Journey Duration**: ~5-10 minutes (15 questions × 20-40 seconds/question)

---

**End of Document**
