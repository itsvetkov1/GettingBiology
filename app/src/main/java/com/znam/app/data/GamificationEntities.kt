package com.znam.app.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.znam.app.R

/**
 * Tracks the user's gamification profile: XP, level, streaks.
 * Single-row table (userId always = 1).
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val userId: Int = 1,
    @ColumnInfo(defaultValue = "0") val totalXp: Int = 0,
    @ColumnInfo(defaultValue = "1") val level: Int = 1,
    @ColumnInfo(defaultValue = "0") val currentStreak: Int = 0,
    @ColumnInfo(defaultValue = "0") val longestStreak: Int = 0,
    @ColumnInfo(defaultValue = "0") val lastQuizDateEpochDay: Long = 0L,  // LocalDate.toEpochDay()
    @ColumnInfo(defaultValue = "0") val quizzesCompletedToday: Int = 0,
    @ColumnInfo(defaultValue = "0") val perfectScoreCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val totalQuizzesCompleted: Int = 0,
    @ColumnInfo(defaultValue = "0") val lastDailyChallengeDateEpochDay: Long = 0L,
    @ColumnInfo(defaultValue = "''") val dailyChallengeQuizType: String = ""
)

/**
 * Records an unlocked achievement.
 */
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val achievementId: String,  // e.g. "first_quiz", "streak_7"
    @ColumnInfo(defaultValue = "0") val unlockedAt: Long = System.currentTimeMillis()
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
    const val SPEED_DEMON = "speed_demon"       // quiz < 90s
    const val NO_HINTS = "no_hints"             // perfect score, 0 hints

    // XP/Level achievements
    const val LEVEL_5 = "level_5"
    const val LEVEL_10 = "level_10"
    const val LEVEL_25 = "level_25"
    const val XP_1000 = "xp_1000"
    const val XP_5000 = "xp_5000"

    private val displayNameResIds = mapOf(
        FIRST_QUIZ to R.string.achievement_first_quiz_name,
        TEN_QUIZZES to R.string.achievement_ten_quizzes_name,
        FIFTY_QUIZZES to R.string.achievement_fifty_quizzes_name,
        HUNDRED_QUIZZES to R.string.achievement_hundred_quizzes_name,
        STREAK_3 to R.string.achievement_streak_3_name,
        STREAK_7 to R.string.achievement_streak_7_name,
        STREAK_14 to R.string.achievement_streak_14_name,
        STREAK_30 to R.string.achievement_streak_30_name,
        FIRST_PERFECT to R.string.achievement_first_perfect_name,
        FIVE_PERFECTS to R.string.achievement_five_perfects_name,
        SPEED_DEMON to R.string.achievement_speed_demon_name,
        NO_HINTS to R.string.achievement_no_hints_name,
        LEVEL_5 to R.string.achievement_level_5_name,
        LEVEL_10 to R.string.achievement_level_10_name,
        LEVEL_25 to R.string.achievement_level_25_name,
        XP_1000 to R.string.achievement_xp_1000_name,
        XP_5000 to R.string.achievement_xp_5000_name
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

    private val descriptionResIds = mapOf(
        FIRST_QUIZ to R.string.achievement_first_quiz_description,
        TEN_QUIZZES to R.string.achievement_ten_quizzes_description,
        FIFTY_QUIZZES to R.string.achievement_fifty_quizzes_description,
        HUNDRED_QUIZZES to R.string.achievement_hundred_quizzes_description,
        STREAK_3 to R.string.achievement_streak_3_description,
        STREAK_7 to R.string.achievement_streak_7_description,
        STREAK_14 to R.string.achievement_streak_14_description,
        STREAK_30 to R.string.achievement_streak_30_description,
        FIRST_PERFECT to R.string.achievement_first_perfect_description,
        FIVE_PERFECTS to R.string.achievement_five_perfects_description,
        SPEED_DEMON to R.string.achievement_speed_demon_description,
        NO_HINTS to R.string.achievement_no_hints_description,
        LEVEL_5 to R.string.achievement_level_5_description,
        LEVEL_10 to R.string.achievement_level_10_description,
        LEVEL_25 to R.string.achievement_level_25_description,
        XP_1000 to R.string.achievement_xp_1000_description,
        XP_5000 to R.string.achievement_xp_5000_description
    )

    @StringRes
    fun displayNameResId(achievementId: String): Int? = displayNameResIds[achievementId]

    @StringRes
    fun descriptionResId(achievementId: String): Int? = descriptionResIds[achievementId]

}


fun Context.achievementDisplayName(achievementId: String): String {
    val resId = Achievements.displayNameResId(achievementId) ?: return achievementId
    return getString(resId)
}

fun Context.achievementDescription(achievementId: String): String {
    val resId = Achievements.descriptionResId(achievementId) ?: return ""
    return getString(resId)
}
