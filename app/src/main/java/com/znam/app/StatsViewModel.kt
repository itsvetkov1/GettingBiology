package com.znam.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.znam.app.data.CategoryStats
import com.znam.app.data.QuizSession
import com.znam.app.data.StatsDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Statistics dashboard.
 */
data class StatsUiState(
    val isLoading: Boolean = true,
    val totalSessions: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val overallAccuracy: Float = 0f,
    val totalTimeSeconds: Int = 0,
    val bestScore: Int = 0,
    val bestAccuracy: Float = 0f,
    val categoryStats: List<CategoryStats> = emptyList(),
    val recentSessions: List<QuizSession> = emptyList(),
    val sessionsThisWeek: Int = 0
) {
    val totalTimeFormatted: String
        get() {
            val hours = totalTimeSeconds / 3600
            val minutes = (totalTimeSeconds % 3600) / 60
            return when {
                hours > 0 -> "${hours}ч ${minutes}мин"
                minutes > 0 -> "${minutes}мин"
                else -> "${totalTimeSeconds}сек"
            }
        }

    val hasData: Boolean
        get() = totalSessions > 0
}

/**
 * ViewModel for the Statistics dashboard.
 *
 * Loads lifetime aggregates, per-category breakdowns, and recent session
 * history from the StatsDao. Stateless — just reads and exposes.
 */
class StatsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    // Injected via Koin in the real app; placeholder direct access for now
    private var statsDao: StatsDao? = null

    fun initialize(dao: StatsDao) {
        statsDao = dao
        loadStats()
    }

    private fun loadStats() {
        val dao = statsDao ?: return
        viewModelScope.launch {
            try {
                val weekAgoMillis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalSessions = dao.totalSessions(),
                        totalQuestionsAnswered = dao.totalQuestionsAnswered(),
                        totalCorrectAnswers = dao.totalCorrectAnswers(),
                        overallAccuracy = dao.overallAccuracyPercent(),
                        totalTimeSeconds = dao.totalTimeSeconds(),
                        bestScore = dao.bestScore(),
                        bestAccuracy = dao.bestAccuracyPercent(),
                        categoryStats = dao.statsByCategory(),
                        recentSessions = dao.recentSessions(20),
                        sessionsThisWeek = dao.sessionsAfter(weekAgoMillis)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadStats()
    }
}
