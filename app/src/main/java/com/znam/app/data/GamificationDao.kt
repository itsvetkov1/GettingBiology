package com.znam.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {

    // --- UserProfile ---

    @Query("SELECT * FROM user_profile WHERE userId = 1")
    suspend fun getProfile(): UserProfile?

    @Query("SELECT * FROM user_profile WHERE userId = 1")
    fun observeProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    /**
     * Ensure a profile row exists. Call on app start.
     */
    suspend fun ensureProfile(): UserProfile {
        val existing = getProfile()
        if (existing != null) return existing
        val fresh = UserProfile()
        insertProfile(fresh)
        return fresh
    }

    // --- Achievements ---

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    suspend fun getAllAchievements(): List<Achievement>

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun observeAchievements(): Flow<List<Achievement>>

    @Query("SELECT achievementId FROM achievements")
    suspend fun getUnlockedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockAchievement(achievement: Achievement)

    @Query("SELECT COUNT(*) FROM achievements WHERE achievementId = :id")
    suspend fun isUnlocked(id: String): Int

    /**
     * Persist profile changes and related achievement unlocks atomically.
     *
     * The caller computes the new profile from a mutex-guarded snapshot; this
     * transaction prevents the profile commit and achievement inserts from
     * being split across separate database units of work.
     */
    @Transaction
    suspend fun updateProfileAndUnlockAchievements(
        profile: UserProfile,
        candidateAchievementIds: List<String>
    ): List<String> {
        updateProfile(profile)
        val alreadyUnlocked = getUnlockedIds().toSet()
        val trulyNew = candidateAchievementIds.distinct().filter { it !in alreadyUnlocked }
        for (id in trulyNew) {
            unlockAchievement(Achievement(achievementId = id))
        }
        return trulyNew
    }
}
