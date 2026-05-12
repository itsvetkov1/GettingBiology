package com.znam.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the user's gamification profile: XP, level, streaks.
 * Single-row table (userId always = 1).
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val userId: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastQuizDateEpochDay: Long = 0L,  // LocalDate.toEpochDay()
    val quizzesCompletedToday: Int = 0,
    val perfectScoreCount: Int = 0,
    val totalQuizzesCompleted: Int = 0
)

/**
 * Records an unlocked achievement.
 */
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val achievementId: String,  // e.g. "first_quiz", "streak_7"
    val unlockedAt: Long = System.currentTimeMillis()
)

/**
 * Known achievement definitions (not stored in DB — just constants).
 */
object Achievements {
    // Milestone achievements
    const val FIRST_QUIZ = "first_quiz"
    const val TEN_QUIZZES = "ten_quizzes"
    const val FIFTY_QUIZZES = "fifty_quizzes"
    const val HUNDRED_QUIZZES = "hundred_quizzes"

    // Streak achievements
    const val STREAK_3 = "streak_3"
    const val STREAK_7 = "streak_7"
    const val STREAK_14 = "streak_14"
    const val STREAK_30 = "streak_30"

    // Performance achievements
    const val FIRST_PERFECT = "first_perfect"
    const val FIVE_PERFECTS = "five_perfects"
    const val SPEED_DEMON = "speed_demon"       // quiz < 60s
    const val NO_HINTS = "no_hints"             // perfect score, 0 hints

    // XP/Level achievements
    const val LEVEL_5 = "level_5"
    const val LEVEL_10 = "level_10"
    const val LEVEL_25 = "level_25"
    const val XP_1000 = "xp_1000"
    const val XP_5000 = "xp_5000"

    /** Human-readable names for display. */
    val displayNames = mapOf(
        FIRST_QUIZ to "First Steps",
        TEN_QUIZZES to "Getting Started",
        FIFTY_QUIZZES to "Dedicated Learner",
        HUNDRED_QUIZZES to "Quiz Master",
        STREAK_3 to "On a Roll",
        STREAK_7 to "Week Warrior",
        STREAK_14 to "Fortnight Force",
        STREAK_30 to "Monthly Legend",
        FIRST_PERFECT to "Perfectionist",
        FIVE_PERFECTS to "Flawless Five",
        SPEED_DEMON to "Speed Demon",
        NO_HINTS to "Solo Genius",
        LEVEL_5 to "Rising Star",
        LEVEL_10 to "Knowledge Seeker",
        LEVEL_25 to "Biology Expert",
        XP_1000 to "XP Collector",
        XP_5000 to "XP Hoarder"
    )

    /** Emoji icons for each achievement. */
    val icons = mapOf(
        FIRST_QUIZ to "🌟",      // star
        TEN_QUIZZES to "📚",     // books
        FIFTY_QUIZZES to "🏆",   // trophy
        HUNDRED_QUIZZES to "👑", // crown
        STREAK_3 to "🔥",        // fire
        STREAK_7 to "🔥",        // fire
        STREAK_14 to "⚡",             // lightning
        STREAK_30 to "🌟",       // star
        FIRST_PERFECT to "✅",         // check
        FIVE_PERFECTS to "💯",   // 100
        SPEED_DEMON to "⏱️",     // stopwatch
        NO_HINTS to "🧠",        // brain
        LEVEL_5 to "🚀",         // rocket
        LEVEL_10 to "🎓",        // graduation cap
        LEVEL_25 to "🧬",        // DNA
        XP_1000 to "💰",         // money bag
        XP_5000 to "💎"          // gem
    )

    /** Description of how to earn each achievement. */
    val descriptions = mapOf(
        FIRST_QUIZ to "Complete your first quiz",
        TEN_QUIZZES to "Complete 10 quizzes",
        FIFTY_QUIZZES to "Complete 50 quizzes",
        HUNDRED_QUIZZES to "Complete 100 quizzes",
        STREAK_3 to "Maintain a 3-day streak",
        STREAK_7 to "Maintain a 7-day streak",
        STREAK_14 to "Maintain a 14-day streak",
        STREAK_30 to "Maintain a 30-day streak",
        FIRST_PERFECT to "Get a perfect score",
        FIVE_PERFECTS to "Get 5 perfect scores",
        SPEED_DEMON to "Complete a quiz in under 60 seconds",
        NO_HINTS to "Get a perfect score without using any hints",
        LEVEL_5 to "Reach level 5",
        LEVEL_10 to "Reach level 10",
        LEVEL_25 to "Reach level 25",
        XP_1000 to "Earn 1,000 XP",
        XP_5000 to "Earn 5,000 XP"
    )
}
