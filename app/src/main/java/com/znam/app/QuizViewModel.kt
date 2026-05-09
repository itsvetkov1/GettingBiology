package com.znam.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.znam.app.data.QuizSession
import com.znam.app.data.StatsDao

/**
 * Sealed class representing possible quiz events emitted as one-shots.
 * Consumed by the UI layer to trigger side effects (navigation, ads, toasts).
 */
sealed class QuizEvent {
    /** Navigate to the result screen with these results. */
    data class NavigateToResults(val results: QuizResults) : QuizEvent()

    /** Show an interstitial ad before navigating to results. */
    data class ShowInterstitialAd(val results: QuizResults) : QuizEvent()

    /** No questions available for the selected quiz type. */
    object NoQuestionsAvailable : QuizEvent()
}

/**
 * Immutable snapshot of a completed quiz, passed to the result screen.
 */
data class QuizResults(
    val score: Int,
    val questions: List<Question>,
    val userAnswers: List<String>,
    val elapsedTimeSeconds: Int
)

/**
 * The hint display state for the current question.
 */
data class HintState(
    val hint1Text: String? = null,
    val hint2Text: String? = null,
    val hint1Visible: Boolean = false,
    val hint2Visible: Boolean = false,
    val canShowMore: Boolean = false
) {
    val hintsShown: Int
        get() = when {
            hint2Visible -> 2
            hint1Visible -> 1
            else -> 0
        }
}

/**
 * Represents the feedback state after the user selects an answer.
 */
data class AnswerFeedback(
    val selectedOption: Int,       // 0-indexed
    val correctOption: Int,        // 0-indexed
    val isCorrect: Boolean
)

/**
 * Complete UI state for the quiz screen. The single source of truth.
 */
data class QuizUiState(
    val isLoading: Boolean = true,
    val quizType: String = "",
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 15,
    val question: Question? = null,
    val parsedOptions: List<String> = emptyList(),
    val score: Int = 0,
    val elapsedSeconds: Int = 0,
    val hintState: HintState = HintState(),
    val answerFeedback: AnswerFeedback? = null,  // null = not yet answered
    val isQuizFinished: Boolean = false
) {
    val questionCounterText: String
        get() = "${currentQuestionIndex + 1}/$totalQuestions"

    val scoreText: String
        get() = "$score / $totalQuestions"

    val timerText: String
        get() {
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val isAnswered: Boolean
        get() = answerFeedback != null

    val canRequestHint: Boolean
        get() = hintState.canShowMore && !isAnswered
}

/**
 * ViewModel managing the full quiz lifecycle.
 *
 * Responsibilities:
 * - Load questions from Room (filtering previously answered)
 * - Manage quiz progression (next question, score, timer)
 * - Handle hint reveal logic
 * - Track user answers for the result screen
 * - Persist progress to SharedPreferences
 *
 * Does NOT handle:
 * - Ad loading/display (that stays in the Activity/Composable as a side effect)
 * - Navigation (emits events, UI layer acts on them)
 * - Direct UI manipulation
 */
class QuizViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val statsDao: StatsDao? = null
) : AndroidViewModel(application) {

    companion object {
        const val MAX_QUESTIONS_PER_SESSION = 15
        const val AUTO_ADVANCE_DELAY_MS = 1500L
        private const val PREFS_NAME = "QuizPrefs"
        private const val KEY_ANSWERED_IDS = "AnsweredQuestionIds"
        private const val KEY_LAST_QUIZ_TYPE = "LAST_QUIZ_TYPE"
    }

    // ── State ───────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<QuizEvent?>(null)
    val events: StateFlow<QuizEvent?> = _events.asStateFlow()

    // ── Internal ────────────────────────────────────────────────────────

    private var questions: List<Question> = emptyList()
    private val userAnswers = mutableListOf<String>()
    private val answeredQuestionIds = mutableListOf<Int>()
    private var timerJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var db: AppDatabase? = null
    private var totalHintsUsed: Int = 0

    private val sharedPrefs by lazy {
        getApplication<Application>().getSharedPreferences(PREFS_NAME, 0)
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Initialize the quiz with a given type. Call once from the UI layer.
     * Loads the database, fetches questions, and starts the timer.
     */
    fun initialize(quizType: String) {
        if (!_uiState.value.isLoading) return  // already initialized

        _uiState.update { it.copy(quizType = quizType) }

        // Load previously answered question IDs
        val savedIds = sharedPrefs.getStringSet(KEY_ANSWERED_IDS, null)
        if (savedIds != null) {
            answeredQuestionIds.addAll(savedIds.map { it.toInt() })
        }

        // Save quiz type preference
        sharedPrefs.edit().putString(KEY_LAST_QUIZ_TYPE, quizType).apply()

        viewModelScope.launch {
            try {
                val loadedQuestions = withContext(Dispatchers.IO) {
                    val dbName = resolveDbName(quizType)
                    db = Room.databaseBuilder(
                        getApplication(),
                        AppDatabase::class.java,
                        dbName
                    )
                        .createFromAsset(dbName)
                        .fallbackToDestructiveMigration()  // TODO: replace with proper migration (Task 1.4)
                        .build()

                    val allQuestions = db!!.questionDao().getAllQuestions()
                    allQuestions.filterNot { it.id in answeredQuestionIds }
                }

                if (loadedQuestions.isEmpty()) {
                    _events.value = QuizEvent.NoQuestionsAvailable
                    return@launch
                }

                questions = loadedQuestions
                val totalToShow = minOf(questions.size, MAX_QUESTIONS_PER_SESSION)

                // Initialize user answers list with placeholder
                repeat(totalToShow) {
                    userAnswers.add("Въпросът е пропуснат.")
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalQuestions = totalToShow,
                        question = questions[0],
                        parsedOptions = questions[0].getParsedOptions(),
                        hintState = buildHintState(questions[0], 0)
                    )
                }

                startTimer()
            } catch (e: Exception) {
                // TODO: expose error state to UI
                _events.value = QuizEvent.NoQuestionsAvailable
            }
        }
    }

    /**
     * User selected an answer option (0-indexed).
     */
    fun selectAnswer(optionIndex: Int) {
        val state = _uiState.value
        if (state.isAnswered || state.question == null) return

        val question = state.question
        val options = state.parsedOptions
        val selectedText = options.getOrNull(optionIndex) ?: return
        val correctAnswer = question.correctAnswer.trim()
        val isCorrect = selectedText.equals(correctAnswer, ignoreCase = true)
        val correctIndex = options.indexOfFirst {
            it.equals(correctAnswer, ignoreCase = true)
        }

        // Record answer
        if (state.currentQuestionIndex < userAnswers.size) {
            userAnswers[state.currentQuestionIndex] = selectedText
        }
        answeredQuestionIds.add(question.id)
        persistProgress()

        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.update {
            it.copy(
                score = newScore,
                answerFeedback = AnswerFeedback(
                    selectedOption = optionIndex,
                    correctOption = correctIndex,
                    isCorrect = isCorrect
                )
            )
        }

        // Schedule auto-advance
        scheduleAutoAdvance()
    }

    /**
     * User tapped the hint button.
     */
    fun requestHint() {
        val state = _uiState.value
        if (!state.canRequestHint || state.question == null) return

        val question = state.question
        val currentHints = state.hintState

        val newHintState = when (currentHints.hintsShown) {
            0 -> {
                if (!question.hint1.isNullOrBlank()) {
                    currentHints.copy(
                        hint1Text = question.hint1,
                        hint1Visible = true,
                        canShowMore = !question.hint2.isNullOrBlank()
                    )
                } else currentHints
            }
            1 -> {
                if (!question.hint2.isNullOrBlank()) {
                    currentHints.copy(
                        hint2Text = question.hint2,
                        hint2Visible = true,
                        canShowMore = false
                    )
                } else currentHints
            }
            else -> currentHints
        }

        if (newHintState.hintsShown > currentHints.hintsShown) {
            totalHintsUsed++
        }
        _uiState.update { it.copy(hintState = newHintState) }
    }

    /**
     * Consume a one-shot event after the UI layer has acted on it.
     */
    fun consumeEvent() {
        _events.value = null
    }

    // ── Internals ───────────────────────────────────────────────────────

    private fun resolveDbName(quizType: String): String {
        return when (quizType) {
            "class8.db" -> "class8.db"
            "class9.db" -> "class9.db"
            "class10.db" -> "class10.db"
            "db_entrance_exam.db" -> "db_entrance_exam.db"
            else -> "dbquestions.db"
        }
    }

    private fun buildHintState(question: Question, hintsShown: Int): HintState {
        return HintState(
            hint1Text = if (hintsShown >= 1) question.hint1 else null,
            hint2Text = if (hintsShown >= 2) question.hint2 else null,
            hint1Visible = hintsShown >= 1,
            hint2Visible = hintsShown >= 2,
            canShowMore = when {
                !question.hasHints() -> false
                hintsShown >= 2 -> false
                hintsShown == 1 && question.hint2.isNullOrBlank() -> false
                else -> true
            }
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun scheduleAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(AUTO_ADVANCE_DELAY_MS)
            advanceToNextQuestion()
        }
    }

    private fun advanceToNextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentQuestionIndex + 1

        if (nextIndex >= state.totalQuestions || nextIndex >= questions.size) {
            finishQuiz()
            return
        }

        val nextQuestion = questions[nextIndex]
        _uiState.update {
            it.copy(
                currentQuestionIndex = nextIndex,
                question = nextQuestion,
                parsedOptions = nextQuestion.getParsedOptions(),
                answerFeedback = null,
                hintState = buildHintState(nextQuestion, 0)
            )
        }
    }

    private fun finishQuiz() {
        stopTimer()

        val state = _uiState.value
        val results = QuizResults(
            score = state.score,
            questions = questions.take(state.totalQuestions),
            userAnswers = ArrayList(userAnswers),
            elapsedTimeSeconds = state.elapsedSeconds
        )

        _uiState.update { it.copy(isQuizFinished = true) }

        // Persist quiz session to stats database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statsDao?.insertSession(
                    QuizSession(
                        quizType = state.quizType,
                        score = state.score,
                        totalQuestions = state.totalQuestions,
                        elapsedTimeSeconds = state.elapsedSeconds,
                        hintsUsed = totalHintsUsed,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {
                // Stats persistence is best-effort; don't crash the quiz flow
            }
        }

        // Emit event — the UI layer decides whether to show an ad first
        _events.value = QuizEvent.ShowInterstitialAd(results)
    }

    private fun persistProgress() {
        sharedPrefs.edit()
            .putStringSet(KEY_ANSWERED_IDS, answeredQuestionIds.map { it.toString() }.toSet())
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        autoAdvanceJob?.cancel()
        db?.close()
    }
}
