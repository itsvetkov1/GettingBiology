package com.znam.app

import com.znam.app.data.GamificationDao
import com.znam.app.data.UserProfile
import java.time.LocalDate

/**
 * Manages the daily challenge feature.
 *
 * Each day generates a deterministic "challenge" quiz type based on the date.
 * The user gets bonus XP for completing the daily challenge.
 * Tracks whether today's challenge has been completed via explicit UserProfile fields.
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

    fun getTodaysChallengeType(): String {
        val dayOfYear = LocalDate.now().dayOfYear
        return QUIZ_TYPES[dayOfYear % QUIZ_TYPES.size]
    }

    fun getTodaysChallengeName(): String {
        return CHALLENGE_NAMES[getTodaysChallengeType()] ?: "Daily Challenge"
    }

    fun isTodaysChallenge(quizType: String): Boolean {
        return quizType == getTodaysChallengeType()
    }

    suspend fun isTodaysChallengeCompleted(): Boolean {
        val profile = gamificationDao.getProfile() ?: return false
        val today = LocalDate.now().toEpochDay()
        return profile.lastDailyChallengeDateEpochDay == today &&
            profile.dailyChallengeQuizType == getTodaysChallengeType()
    }

    suspend fun shouldAwardDailyChallenge(isDailyChallenge: Boolean, quizType: String): Boolean {
        return isDailyChallenge && isTodaysChallenge(quizType) && !isTodaysChallengeCompleted()
    }

    fun markDailyChallengeCompleted(profile: UserProfile, quizType: String): UserProfile {
        return profile.copy(
            lastDailyChallengeDateEpochDay = LocalDate.now().toEpochDay(),
            dailyChallengeQuizType = quizType
        )
    }
}
