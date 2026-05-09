package com.znam.app

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDatabaseMigrationTest {
    @Test
    fun migration2To3_isNoOpSchemaVersionBridge() {
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
    }

    @Test
    fun allMigrations_containsMigration2To3() {
        assertEquals(listOf(AppDatabase.MIGRATION_2_3), AppDatabase.ALL_MIGRATIONS.toList())
    }
}
