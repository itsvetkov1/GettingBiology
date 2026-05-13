package com.znam.app

import android.util.Log
import com.znam.app.data.Achievement
import com.znam.app.data.Achievements
import com.znam.app.data.GamificationDao
import com.znam.app.data.UserProfile
import java.time.LocalDate

/**
 * Encapsulates all gamification logic: XP calculation, leveling,
 * streak management, and achievement unlocking.
 *
 * Stateless — reads from and writes to GamificationDao.
 * Called from QuizViewModel.finishQuiz().
 */
class GamificationManager(
    private val gamificationDao: GamificationDao,
    private val dailyChallengeManager: DailyChallengeManager? = null
) {
    companion object {
        private const val TAG = "GamificationManager"

        // XP rewards
        const val XP_PER_CORRECT = 10
        const val XP_PERFECT_BONUS = 50
        const val XP_STREAK_MULTIPLIER_BASE = 5   // +5 XP per streak day
        const val XP_NO_HINTS_BONUS = 25
        const val XP_SPEED_BONUS_THRESHOLD_SECONDS = 90
        const val XP_SPEED_BONUS = 30

        /**
         * XP required to reach a given level.
         * Formula: 100 * level * (level + 1) / 2
         * Level 1: 0, Level 2: 100, Level 3: 300, Level 4: 600, ...
         */
        fun xpForLevel(level: Int): Int {
            if (level <= 1) return 0
            return 100 * level * (level - 1) / 2
        }

        /**
         * Calculate level from total XP.
         */
        fun levelFromXp(totalXp: Int): Int {
            var level = 1
            while (xpForLevel(level + 1) <= totalXp) {
                level++
            }
            return level
        }

        /**
         * XP progress within current level as a fraction [0, 1).
         */
        fun levelProgress(totalXp: Int): Float {
            val level = levelFromXp(totalXp)
            val currentLevelXp = xpForLevel(level)
            val nextLevelXp = xpForLevel(level + 1)
            val range = nextLevelXp - currentLevelXp
            return if (range > 0) (totalXp - currentLevelXp).toFloat() / range else 0f
        }
    }

    /**
     * Result of processing a completed quiz through the gamification system.
     */
    data class GamificationResult(
        val xpEarned: Int,
        val newTotalXp: Int,
        val oldLevel: Int,
        val newLevel: Int,
        val leveledUp: Boolean,
        val currentStreak: Int,
        val newAchievements: List<String>  // achievement IDs unlocked this session
    )

    /**
     * Process a completed quiz: update XP, streak, check achievements.
     * Returns a result describing what changed (for UI feedback).
     */
    suspend fun processQuizCompletion(
        score: Int,
        totalQuestions: Int,
        elapsedTimeSeconds: Int,
        hintsUsed: Int,
        isDailyChallenge: Boolean = false,
        quizType: String = ""
    ): GamificationResult {
        val profile = gamificationDao.ensureProfile()
        val today = LocalDate.now().toEpochDay()
        val isPerfect = score == totalQuestions && totalQuestions > 0
        val isSpeedRun = elapsedTimeSeconds < XP_SPEED_BONUS_THRESHOLD_SECONDS && totalQuestions > 0
        val awardDailyChallengeBonus = dailyChallengeManager?.shouldAwardDailyChallenge(
            isDailyChallenge = isDailyChallenge,
            quizType = quizType
        ) == true

        // --- Update streak first so XP uses the post-rollover value ---
        val yesterday = today - 1
        val newStreak: Int
        val quizzesToday: Int

        when (profile.lastQuizDateEpochDay) {
            today -> {
                // Same day — streak unchanged, increment daily count
                newStreak = profile.currentStreak
                quizzesToday = profile.quizzesCompletedToday + 1
            }
            yesterday -> {
                // Consecutive day — extend streak
                newStreak = profile.currentStreak + 1
                quizzesToday = 1
            }
            else -> {
                // Gap — reset streak (but this quiz starts a new one)
                newStreak = 1
                quizzesToday = 1
            }
        }

        // --- Calculate XP ---
        var xpEarned = score * XP_PER_CORRECT
        if (isPerfect) xpEarned += XP_PERFECT_BONUS
        if (hintsUsed == 0 && isPerfect) xpEarned += XP_NO_HINTS_BONUS
        if (isSpeedRun) xpEarned += XP_SPEED_BONUS
        if (awardDailyChallengeBonus) xpEarned += DailyChallengeManager.DAILY_CHALLENGE_XP_BONUS

        // Streak multiplier uses the streak after same-day/rollover/reset handling.
        val streakBonus = newStreak * XP_STREAK_MULTIPLIER_BASE
        xpEarned += streakBonus

        val newTotalXp = profile.totalXp + xpEarned
        val oldLevel = profile.level
        val newLevel = levelFromXp(newTotalXp)
        val newLongest = maxOf(profile.longestStreak, newStreak)
        val newPerfectCount = if (isPerfect) profile.perfectScoreCount + 1 else profile.perfectScoreCount

        // --- Persist updated profile ---
        val baseUpdatedProfile = profile.copy(
            totalXp = newTotalXp,
            level = newLevel,
            currentStreak = newStreak,
            longestStreak = newLongest,
            lastQuizDateEpochDay = today,
            quizzesCompletedToday = quizzesToday,
            perfectScoreCount = newPerfectCount,
            totalQuizzesCompleted = profile.totalQuizzesCompleted + 1
        )
        val updatedProfile = if (awardDailyChallengeBonus) {
            dailyChallengeManager?.markDailyChallengeCompleted(baseUpdatedProfile, quizType) ?: baseUpdatedProfile
        } else {
            baseUpdatedProfile
        }
        gamificationDao.updateProfile(updatedProfile)

        // --- Check achievements ---
        val newAchievements = mutableListOf<String>()
        val totalQuizzes = updatedProfile.totalQuizzesCompleted

        fun tryUnlock(id: String, condition: Boolean) {
            if (condition) {
                try {
                    // We'll check and insert — IGNORE conflict means no error if already unlocked
                    newAchievements.add(id)
                } catch (e: Exception) {
                    Log.w(TAG, "Achievement check failed for $id", e)
                }
            }
        }

        // Milestone achievements
        tryUnlock(Achievements.FIRST_QUIZ, totalQuizzes >= 1)
        tryUnlock(Achievements.TEN_QUIZZES, totalQuizzes >= 10)
        tryUnlock(Achievements.FIFTY_QUIZZES, totalQuizzes >= 50)
        tryUnlock(Achievements.HUNDRED_QUIZZES, totalQuizzes >= 100)

        // Streak achievements
        tryUnlock(Achievements.STREAK_3, newStreak >= 3)
        tryUnlock(Achievements.STREAK_7, newStreak >= 7)
        tryUnlock(Achievements.STREAK_14, newStreak >= 14)
        tryUnlock(Achievements.STREAK_30, newStreak >= 30)

        // Performance achievements
        tryUnlock(Achievements.FIRST_PERFECT, newPerfectCount >= 1)
        tryUnlock(Achievements.FIVE_PERFECTS, newPerfectCount >= 5)
        tryUnlock(Achievements.SPEED_DEMON, isSpeedRun)
        tryUnlock(Achievements.NO_HINTS, isPerfect && hintsUsed == 0)

        // Level achievements
        tryUnlock(Achievements.LEVEL_5, newLevel >= 5)
        tryUnlock(Achievements.LEVEL_10, newLevel >= 10)
        tryUnlock(Achievements.LEVEL_25, newLevel >= 25)

        // XP achievements
        tryUnlock(Achievements.XP_1000, newTotalXp >= 1000)
        tryUnlock(Achievements.XP_5000, newTotalXp >= 5000)

        // Actually persist only newly unlocked achievements
        val alreadyUnlocked = gamificationDao.getUnlockedIds().toSet()
        val trulyNew = newAchievements.filter { it !in alreadyUnlocked }
        for (id in trulyNew) {
            gamificationDao.unlockAchievement(Achievement(achievementId = id))
        }

        Log.d(TAG, "Quiz processed: +${xpEarned}XP, level $oldLevel->$newLevel, streak=$newStreak, new achievements=$trulyNew")

        return GamificationResult(
            xpEarned = xpEarned,
            newTotalXp = newTotalXp,
            oldLevel = oldLevel,
            newLevel = newLevel,
            leveledUp = newLevel > oldLevel,
            currentStreak = newStreak,
            newAchievements = trulyNew
        )
    }
}
