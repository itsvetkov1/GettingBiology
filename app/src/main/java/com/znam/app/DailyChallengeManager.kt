package com.znam.app

import com.znam.app.data.GamificationDao
import com.znam.app.data.UserProfile


/**
 * Manages the daily challenge feature.
 *
 * Each day generates a deterministic "challenge" quiz type based on the date.
 * The user gets bonus XP for completing the daily challenge.
 * Tracks whether today's challenge has been completed via explicit UserProfile fields.
 */
class DailyChallengeManager(
    private val gamificationDao: GamificationDao,
    private val clock: Clock = SystemClock
) {
    companion object {
        const val DAILY_CHALLENGE_XP_BONUS = 50
        private val QUIZ_TYPES = listOf("class9.db", "class10.db", "db_entrance_exam.db")
        private val CHALLENGE_NAME_RES_IDS = mapOf(
            "class9.db" to R.string.daily_challenge_class9,
            "class10.db" to R.string.daily_challenge_class10,
            "db_entrance_exam.db" to R.string.daily_challenge_entrance_exam
        )
    }

    fun getTodaysChallengeType(): String {
        val dayOfYear = clock.today().dayOfYear
        return QUIZ_TYPES[dayOfYear % QUIZ_TYPES.size]
    }

    fun getTodaysChallengeNameResId(): Int {
        return CHALLENGE_NAME_RES_IDS[getTodaysChallengeType()] ?: R.string.daily_challenge_default
    }

    fun isTodaysChallenge(quizType: String): Boolean {
        return quizType == getTodaysChallengeType()
    }

    suspend fun isTodaysChallengeCompleted(): Boolean {
        val profile = gamificationDao.getProfile() ?: return false
        val today = clock.today().toEpochDay()
        return profile.lastDailyChallengeDateEpochDay == today &&
            profile.dailyChallengeQuizType == getTodaysChallengeType()
    }

    suspend fun shouldAwardDailyChallenge(isDailyChallenge: Boolean, quizType: String): Boolean {
        return isDailyChallenge && isTodaysChallenge(quizType) && !isTodaysChallengeCompleted()
    }

    fun markDailyChallengeCompleted(profile: UserProfile, quizType: String): UserProfile {
        return profile.copy(
            lastDailyChallengeDateEpochDay = clock.today().toEpochDay(),
            dailyChallengeQuizType = quizType
        )
    }
}
