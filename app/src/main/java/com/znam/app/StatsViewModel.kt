package com.znam.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.znam.app.data.Achievement
import com.znam.app.data.CategoryStats
import com.znam.app.data.GamificationDao
import com.znam.app.data.QuizSession
import com.znam.app.data.StatsDao
import com.znam.app.data.UserProfile
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
    val sessionsThisWeek: Int = 0,
    // Gamification
    val userProfile: UserProfile? = null,
    val achievements: List<Achievement> = emptyList()
) {
    val hasData: Boolean
        get() = totalSessions > 0
}

/**
 * ViewModel for the Statistics dashboard.
 *
 * Loads lifetime aggregates, per-category breakdowns, recent session
 * history, and gamification data from the DAOs.
 */
class StatsViewModel(
    application: Application,
    private val statsDao: StatsDao,
    private val gamificationDao: GamificationDao? = null
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val weekAgoMillis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

                // Load gamification data
                val profile = try { gamificationDao?.getProfile() } catch (e: Exception) { null }
                val achievements = try { gamificationDao?.getAllAchievements() ?: emptyList() } catch (e: Exception) { emptyList() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalSessions = statsDao.totalSessions(),
                        totalQuestionsAnswered = statsDao.totalQuestionsAnswered(),
                        totalCorrectAnswers = statsDao.totalCorrectAnswers(),
                        overallAccuracy = statsDao.overallAccuracyPercent(),
                        totalTimeSeconds = statsDao.totalTimeSeconds(),
                        bestScore = statsDao.bestScore(),
                        bestAccuracy = statsDao.bestAccuracyPercent(),
                        categoryStats = statsDao.statsByCategory(),
                        recentSessions = statsDao.recentSessions(20),
                        sessionsThisWeek = statsDao.sessionsAfter(weekAgoMillis),
                        userProfile = profile,
                        achievements = achievements
                    )
                }
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Failed to load stats", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadStats()
    }
}
