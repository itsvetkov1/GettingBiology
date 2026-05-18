package com.znam.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
        FIRST_QUIZ to "Първи стъпки",
        TEN_QUIZZES to "Добро начало",
        FIFTY_QUIZZES to "Отдаден ученик",
        HUNDRED_QUIZZES to "Майстор на тестовете",
        STREAK_3 to "В серия",
        STREAK_7 to "Седмичен воин",
        STREAK_14 to "Двуседмична сила",
        STREAK_30 to "Месечна легенда",
        FIRST_PERFECT to "Перфекционист",
        FIVE_PERFECTS to "Безгрешна петица",
        SPEED_DEMON to "Скоростен демон",
        NO_HINTS to "Самостоятелен гений",
        LEVEL_5 to "Изгряваща звезда",
        LEVEL_10 to "Търсач на знание",
        LEVEL_25 to "Експерт по биология",
        XP_1000 to "Колекционер на XP",
        XP_5000 to "Трупач на XP"
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
        FIRST_QUIZ to "Завърши първия си тест",
        TEN_QUIZZES to "Завърши 10 теста",
        FIFTY_QUIZZES to "Завърши 50 теста",
        HUNDRED_QUIZZES to "Завърши 100 теста",
        STREAK_3 to "Поддържай 3-дневна поредица",
        STREAK_7 to "Поддържай 7-дневна поредица",
        STREAK_14 to "Поддържай 14-дневна поредица",
        STREAK_30 to "Поддържай 30-дневна поредица",
        FIRST_PERFECT to "Постигни перфектен резултат",
        FIVE_PERFECTS to "Постигни 5 перфектни резултата",
        SPEED_DEMON to "Завърши тест за под 90 секунди",
        NO_HINTS to "Постигни перфектен резултат без подсказки",
        LEVEL_5 to "Достигни ниво 5",
        LEVEL_10 to "Достигни ниво 10",
        LEVEL_25 to "Достигни ниво 25",
        XP_1000 to "Спечели 1 000 XP",
        XP_5000 to "Спечели 5 000 XP"
    )
}
