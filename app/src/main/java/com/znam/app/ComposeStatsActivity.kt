package com.znam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.znam.app.ui.StatsScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Compose-based statistics activity. Hosts StatsScreen.
 * Stats load automatically via the ViewModel's init block.
 */
class ComposeStatsActivity : ComponentActivity() {

    private val statsViewModel: StatsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                StatsScreen(
                    viewModel = statsViewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
