package com.znam.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseProviderTest {
    @Test
    fun databaseNameForQuizType_mapsKnownQuizTypesToAssets() {
        val provider = DatabaseProvider(context = null)

        assertEquals("class8.db", provider.databaseNameForQuizType("class8.db"))
        assertEquals("class9.db", provider.databaseNameForQuizType("class9.db"))
        assertEquals("class10.db", provider.databaseNameForQuizType("class10.db"))
        assertEquals("db_entrance_exam.db", provider.databaseNameForQuizType("db_entrance_exam.db"))
    }

    @Test
    fun databaseNameForQuizType_defaultsToFallbackAsset() {
        val provider = DatabaseProvider(context = null)

        assertEquals("dbquestions.db", provider.databaseNameForQuizType("unknown"))
    }
}
