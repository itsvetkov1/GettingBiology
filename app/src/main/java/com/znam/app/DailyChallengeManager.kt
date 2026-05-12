package com.znam.app

import com.znam.app.data.GamificationDao
import com.znam.app.data.UserProfile
import java.time.LocalDate

/**
 * Manages the daily challenge feature.
 *
 * Each day generates a deterministic "challenge" quiz type based on the date.
 * The user gets bonus XP for completing the daily challenge.
 * Tracks whether today's challenge has been completed via UserProfile.
 */
class DailyChallengeManager(
    private val gamificationDao: GamificationDao
) {
    companion object {
        const val DAILY_CHALLENGE_XP_BONUS = 50
        private val QUIZ_TYPES = listOf("class9.db", "class10.db", "db_entrance_exam.db")
        private val CHALLENGE_NAMES = mapOf(
            "class9.db" to "9th Grade Challenge",
            "class10.db" to "10th Grade Challenge",
            "db_entrance_exam.db" to "Entrance Exam Challenge"
        )
    }

    /**
     * Get today's challenge quiz type (deterministic based on date).
     */
    fun getTodaysChallengeType(): String {
        val dayOfYear = LocalDate.now().dayOfYear
        return QUIZ_TYPES[dayOfYear % QUIZ_TYPES.size]
    }

    /**
     * Get the display name for today's challenge.
     */
    fun getTodaysChallengeName(): String {
        return CHALLENGE_NAMES[getTodaysChallengeType()] ?: "Daily Challenge"
    }

    /**
     * Check if the user has completed today's challenge.
     */
    suspend fun isTodaysChallengeCompleted(): Boolean {
        val profile = gamificationDao.getProfile() ?: return false
        val today = LocalDate.now().toEpochDay()
        // If last quiz was today and they've done at least one quiz today
        return profile.lastQuizDateEpochDay == today && profile.quizzesCompletedToday > 0
    }
}
